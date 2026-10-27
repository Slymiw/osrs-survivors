package com.osrssurvivors.web;

import com.osrssurvivors.game.Boost;
import com.osrssurvivors.game.BossDrop;
import com.osrssurvivors.game.EnemyKind;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.Item;
import com.osrssurvivors.game.Location;
import com.osrssurvivors.game.Offer;
import com.osrssurvivors.game.Perk;
import com.osrssurvivors.game.Pet;
import com.osrssurvivors.game.Slot;
import com.osrssurvivors.render.ActorSprite;
import com.osrssurvivors.render.GameAssets;
import com.osrssurvivors.render.MeshSnapshot;
import com.osrssurvivors.render.ModelRasterizer;
import com.osrssurvivors.render.PlayerPose;
import com.osrssurvivors.render.Prop;
import com.osrssurvivors.render.Terrain;
import com.osrssurvivors.render.TextStyle;
import com.osrssurvivors.render.UiSprite;
import com.osrssurvivors.web.awt.WebImage;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.teavm.jso.dom.html.HTMLImageElement;

/**
 * Artwork for the browser from the exported assets: interface sprites and item icons by name,
 * NPC and player models posed in their animations and rasterised into cached sprites, and the
 * current map location's ground and scenery. Sprites are cached per pose
 * and direction, rendered lazily within a per-frame time budget, and re-rendered when the zoom
 * changes.
 */
final class WebAssets implements GameAssets
{
	/** One game tile (128 model units) spans 32 world units. */
	private static final double WORLD_UNITS_PER_MODEL_UNIT = 0.25;
	private static final double CAMERA_PITCH = Math.toRadians(35);
	private static final int DIRECTIONS = 8;
	/** Time per frame that may be spent rendering new sprites, so warming caches never causes a hitch. */
	private static final double RENDER_BUDGET_MS = 6;
	/** Sprite scale resolution: sprites are re-rendered when the zoom changes by more than this. */
	private static final double SCALE_STEP = 1 / 40.0;
	private static final int PLAYER_SLOT = EnemyKind.values().length;
	private static final int EQUIPMENT_SLOTS = 12;

	private static final class NpcLook
	{
		final AnimModel model;
		final Sequence walk;
		final Sequence idle;
		final double scale;

		NpcLook(AnimModel model, Sequence walk, Sequence idle, double scale)
		{
			this.model = model;
			this.walk = walk;
			this.idle = idle;
			this.scale = scale;
		}
	}

	/** A worn item: its model, the equipment slot it fills and the body parts it hides. */
	private static final class Worn
	{
		final AnimModel model;
		final int slot;
		final int[] hides;
		final Sequence attack;

		Worn(AnimModel model, int slot, int[] hides, Sequence attack)
		{
			this.model = model;
			this.slot = slot;
			this.hides = hides;
			this.attack = attack;
		}
	}

	private final Map<String, String> icons = new HashMap<>();
	private final Map<String, BufferedImage> images = new HashMap<>();
	private final Map<EnemyKind, NpcLook> npcs = new EnumMap<>(EnemyKind.class);
	private final Map<Gear, Worn> gear = new EnumMap<>(Gear.class);
	/** Body part models by equipment slot. */
	private final AnimModel[] kits = new AnimModel[EQUIPMENT_SLOTS];
	private final Sequence punch;
	private final Sequence ready;
	private final Sequence walk;
	private final Font regular = new Font("RuneScape", Font.PLAIN, 16);
	private final Font bold = new Font("RuneScape Bold", Font.PLAIN, 16);
	private final Font small = new Font("RuneScape Small", Font.PLAIN, 16);

	private final Map<Long, MeshSnapshot> npcMeshes = new HashMap<>();
	private final Map<Long, MeshSnapshot> playerMeshes = recent(PLAYER_CACHE);
	private final Map<Long, ActorSprite> npcSprites = new HashMap<>();
	private final Map<Long, ActorSprite> playerSprites = recent(PLAYER_CACHE);
	private final Map<Integer, ActorSprite> propSprites = new HashMap<>();
	/** Most recent sprite per character and direction, shown while new frames wait for budget. */
	private final Map<Integer, ActorSprite> lastSprite = new HashMap<>();
	private AnimModel playerModel;
	private int playerGearVersion = -1;
	private MapArea area;
	private long scaleBucket = -1;
	private double deadline;

	WebAssets(Map<String, Object> manifest, ActorData actors)
	{
		Map<String, Object> iconMap = Json.object(manifest.get("icons"));
		for (Map.Entry<String, Object> e : iconMap.entrySet())
		{
			icons.put(e.getKey(), Json.string(e.getValue()));
		}

		Map<String, Object> npcMap = Json.object(manifest.get("npcs"));
		for (EnemyKind kind : EnemyKind.values())
		{
			Map<String, Object> n = Json.object(npcMap.get(kind.name()));
			AnimModel model = n == null ? null : actors.models.get(Json.string(n.get("model")));
			if (model != null)
			{
				npcs.put(kind, new NpcLook(model, actors.sequences.get(Json.integer(n.get("walk"), -1)),
					actors.sequences.get(Json.integer(n.get("idle"), -1)), Json.number(n.get("scale"), 1)));
			}
		}

		Map<String, Object> player = Json.object(manifest.get("player"));
		Map<String, Object> kitMap = Json.object(player.get("kits"));
		for (Map.Entry<String, Object> e : kitMap.entrySet())
		{
			kits[Integer.parseInt(e.getKey())] = actors.models.get(Json.string(e.getValue()));
		}
		Map<String, Object> gearMap = Json.object(player.get("gear"));
		for (Gear g : Gear.values())
		{
			Map<String, Object> w = Json.object(gearMap.get(g.name()));
			if (w == null)
			{
				continue;
			}
			List<Object> hideList = Json.array(w.get("hides"));
			int[] hides = new int[hideList == null ? 0 : hideList.size()];
			for (int i = 0; i < hides.length; i++)
			{
				hides[i] = Json.integer(hideList.get(i), -1);
			}
			String modelName = Json.string(w.get("model"));
			gear.put(g, new Worn(modelName == null ? null : actors.models.get(modelName), Json.integer(w.get("slot"), -1), hides,
				actors.sequences.get(Json.integer(w.get("attack"), -1))));
		}
		punch = actors.sequences.get(Json.integer(player.get("punch"), -1));
		ready = actors.sequences.get(Json.integer(player.get("ready"), -1));
		walk = actors.sequences.get(Json.integer(player.get("walk"), -1));
	}

	/** The map location the game is played in, or null for the plain field. */
	void setArea(MapArea area)
	{
		this.area = area;
		propSprites.clear();
	}

	@Override
	public void beginFrame()
	{
		deadline = Browser.now() + RENDER_BUDGET_MS;
	}

	private boolean canRender()
	{
		return Browser.now() < deadline;
	}

	// ---------------------------------------------------------------- characters

	@Override
	public ActorSprite enemy(EnemyKind kind, double facing, boolean moving, double animTime, double pixelsPerUnit)
	{
		NpcLook npc = npcs.get(kind);
		if (npc == null)
		{
			return null;
		}
		Sequence anim = moving ? npc.walk : npc.idle;
		int frame = anim == null ? 0 : anim.loopFrame(animTime);
		long poseKey = (long) kind.ordinal() << 40 | (moving ? 1L << 32 : 0) | (frame & 0xffffffffL);
		return actor(kind.ordinal(), poseKey, facing, pixelsPerUnit, npcMeshes, npcSprites,
			() -> npc.model.pose(anim == null ? null : anim.frames[frame], npc.scale));
	}

	/** Player poses kept: flowing capes give every pose many looks, so only the latest are kept. */
	private static final int PLAYER_CACHE = 480;

	/** A map that forgets its least recently used entries beyond {@code limit}. */
	private static <V> Map<Long, V> recent(int limit)
	{
		return new LinkedHashMap<Long, V>(64, 0.75f, true)
		{
			@Override
			protected boolean removeEldestEntry(Map.Entry<Long, V> eldest)
			{
				return size() > limit;
			}
		};
	}

	@Override
	public ActorSprite player(PlayerPose pose, double pixelsPerUnit)
	{
		if (pose.gearVersion != playerGearVersion || playerModel == null)
		{
			playerModel = dress(pose.worn);
			playerGearVersion = pose.gearVersion;
			// poses in the old gear are no longer needed
			playerMeshes.clear();
			playerSprites.clear();
		}
		Sequence anim;
		int frame;
		boolean walking = false;
		if (pose.isAttacking())
		{
			Worn weapon = pose.weapon() == null ? null : gear.get(pose.weapon());
			anim = weapon == null || weapon.attack == null ? punch : weapon.attack;
			frame = anim == null ? 0 : anim.progressFrame(pose.attackProgress);
		}
		else
		{
			walking = pose.moving;
			anim = walking ? walk : ready;
			frame = anim == null ? 0 : anim.loopFrame(pose.time);
		}
		// worn lava (the fire capes) flows, a step at a time
		int flow = playerModel.flowStep(pose.time);
		long poseKey = (long) ((anim == null ? -1 : anim.id) + 1 & 0xffff) << 32 | (long) (frame & 0xffff) << 16
			| (walking ? 1L << 15 : 0) | (long) (flow & 0xff) << 48;
		AnimModel model = playerModel;
		Sequence a = anim;
		int f = frame;
		return actor(PLAYER_SLOT, poseKey, pose.facing, pixelsPerUnit, playerMeshes, playerSprites,
			() -> model.pose(a == null ? null : a.frames[f], 1, flow));
	}

	/** The player's body with the worn items, merged so animations move it as one. */
	private AnimModel dress(Map<Slot, Gear> worn)
	{
		AnimModel[] slots = kits.clone();
		Set<Integer> hidden = new HashSet<>();
		for (Gear g : worn.values())
		{
			Worn w = gear.get(g);
			if (w == null || w.slot < 0)
			{
				continue;
			}
			slots[w.slot] = w.model;
			for (int h : w.hides)
			{
				hidden.add(h);
			}
		}
		List<AnimModel> parts = new ArrayList<>();
		for (int i = 0; i < EQUIPMENT_SLOTS; i++)
		{
			if (slots[i] != null && !hidden.contains(i))
			{
				parts.add(slots[i]);
			}
		}
		return AnimModel.concat(parts);
	}

	/**
	 * A cached sprite for a pose seen from a direction, rendered if the frame's budget allows.
	 * Falls back to the last sprite drawn for this character and direction.
	 */
	private ActorSprite actor(int slot, long poseKey, double facing, double pixelsPerUnit, Map<Long, MeshSnapshot> meshes,
		Map<Long, ActorSprite> sprites, Supplier<MeshSnapshot> mesh)
	{
		checkScale(pixelsPerUnit);
		int dir = direction(facing);
		long key = poseKey * DIRECTIONS + dir;
		ActorSprite sprite = sprites.get(key);
		if (sprite != null)
		{
			return sprite;
		}
		int dirKey = slot * DIRECTIONS + dir;
		if (!canRender())
		{
			return lastSprite.get(dirKey);
		}
		MeshSnapshot m = meshes.get(poseKey);
		if (m == null)
		{
			m = mesh.get();
			meshes.put(poseKey, m);
		}
		sprite = rasterise(m, ModelRasterizer.yawFor(dir * 2 * Math.PI / DIRECTIONS));
		if (sprite == null)
		{
			return lastSprite.get(dirKey);
		}
		sprites.put(key, sprite);
		lastSprite.put(dirKey, sprite);
		return sprite;
	}

	private ActorSprite rasterise(MeshSnapshot mesh, double yaw)
	{
		return ModelRasterizer.render(mesh, yaw, CAMERA_PITCH, scaleBucket * SCALE_STEP * WORLD_UNITS_PER_MODEL_UNIT);
	}

	private void checkScale(double pixelsPerUnit)
	{
		long bucket = Math.max(1, Math.round(pixelsPerUnit / SCALE_STEP));
		if (bucket != scaleBucket)
		{
			npcSprites.clear();
			playerSprites.clear();
			propSprites.clear();
			lastSprite.clear();
			scaleBucket = bucket;
		}
	}

	private static int direction(double facing)
	{
		double turns = facing / (2 * Math.PI);
		int dir = (int) Math.round(turns * DIRECTIONS) % DIRECTIONS;
		return dir < 0 ? dir + DIRECTIONS : dir;
	}

	// ---------------------------------------------------------------- the area

	@Override
	public Terrain terrain()
	{
		return area == null ? null : area.terrain;
	}

	@Override
	public List<Prop> props(double minX, double minY, double maxX, double maxY, double pixelsPerUnit)
	{
		MapArea a = area;
		if (a == null)
		{
			return Collections.emptyList();
		}
		checkScale(pixelsPerUnit);
		List<Prop> visible = new ArrayList<>();
		for (int p = 0; p < a.propMesh.length; p++)
		{
			double x = a.propX[p];
			double y = a.propY[p];
			if (x < minX || x > maxX || y < minY || y > maxY)
			{
				continue;
			}
			int meshId = a.propMesh[p];
			ActorSprite sprite = propSprites.get(meshId);
			if (sprite == null && canRender())
			{
				sprite = rasterise(a.meshes[meshId], 0);
				if (sprite != null)
				{
					propSprites.put(meshId, sprite);
				}
			}
			if (sprite != null)
			{
				visible.add(new Prop(x, y, sprite));
			}
		}
		return visible;
	}

	// ---------------------------------------------------------------- icons

	/** An exported picture by name ("i995", "s1203"), or null. */
	BufferedImage picture(String name)
	{
		if (name == null)
		{
			return null;
		}
		BufferedImage image = images.get(name);
		if (image == null)
		{
			HTMLImageElement element = Browser.image(name);
			if (element == null)
			{
				return null;
			}
			image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			((WebImage) (Object) image).load(element);
			images.put(name, image);
		}
		return image;
	}

	private BufferedImage icon(String key)
	{
		return picture(icons.get(key));
	}

	@Override
	public BufferedImage sprite(UiSprite which)
	{
		return icon("ui." + which.name());
	}

	@Override
	public BufferedImage offerIcon(Offer offer)
	{
		switch (offer.getKind())
		{
			case GEAR:
				return gearIcon(offer.getGear());
			case PASSIVE:
				return icon("passive." + offer.getPassive().name() + "." + offer.getRank());
			case COINS:
				return icon("offer.coins");
			case BOOST:
				return icon("boost." + offer.getBoost().name() + "." + Boost.DOSES);
			default:
				return icon("food." + offer.getFood().name());
		}
	}

	@Override
	public BufferedImage itemIcon(Item item)
	{
		if (item.getGear() != null)
		{
			return gearIcon(item.getGear());
		}
		if (item.getPotion() != null)
		{
			return icon("boost." + item.getPotion().name() + "." + item.getDoses());
		}
		return icon("food." + item.getFood().name());
	}

	@Override
	public BufferedImage locationIcon(Location location)
	{
		return icon("location." + location.name());
	}

	@Override
	public BufferedImage perkIcon(Perk perk)
	{
		return icon("perk." + perk.name());
	}

	@Override
	public BufferedImage petIcon(Pet pet)
	{
		return icon("pet." + pet.name());
	}

	@Override
	public BufferedImage prizeIcon(BossDrop.Prize prize)
	{
		switch (prize.getKind())
		{
			case COINS:
				return icon("prize.coins");
			case LAMP:
				return icon("prize.lamp");
			case GEAR:
				return gearIcon(prize.getGear());
			default:
				return petIcon(prize.getPet());
		}
	}

	@Override
	public BufferedImage gearIcon(Gear g)
	{
		return g == null ? null : icon("gear." + g.name());
	}

	@Override
	public BufferedImage projectile(Gear weapon)
	{
		return weapon == null ? null : icon("spell." + weapon.name());
	}

	@Override
	public BufferedImage gem(int xpValue)
	{
		// as GearArt.gemItem
		int[] steps = {25, 15, 8, 4, 2};
		for (int step : steps)
		{
			if (xpValue >= step)
			{
				return icon("gem." + step);
			}
		}
		return icon("gem.1");
	}

	@Override
	public Font font(TextStyle style)
	{
		switch (style)
		{
			case BOLD:
				return bold;
			case SMALL:
				return small;
			default:
				return regular;
		}
	}

	@Override
	public boolean pixelFonts()
	{
		return true;
	}
}
