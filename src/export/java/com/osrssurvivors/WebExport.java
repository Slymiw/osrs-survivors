package com.osrssurvivors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.osrssurvivors.game.Arena;
import com.osrssurvivors.game.Boost;
import com.osrssurvivors.game.Food;
import com.osrssurvivors.game.EnemyKind;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.Location;
import com.osrssurvivors.game.Passive;
import com.osrssurvivors.game.Perk;
import com.osrssurvivors.game.Pet;
import com.osrssurvivors.game.Slot;
import com.osrssurvivors.game.Style;
import com.osrssurvivors.map.AreaBuilder;
import com.osrssurvivors.map.FloorDefinition;
import com.osrssurvivors.map.MapSource;
import com.osrssurvivors.map.MapSquare;
import com.osrssurvivors.map.ObjectDefinition;
import com.osrssurvivors.render.JagexPalette;
import com.osrssurvivors.render.MeshSnapshot;
import com.osrssurvivors.render.UiSprite;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;
import net.runelite.api.PlayerComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;
import net.runelite.cache.IndexType;
import net.runelite.cache.ItemManager;
import net.runelite.cache.SpriteManager;
import net.runelite.cache.TextureManager;
import net.runelite.cache.definitions.FrameDefinition;
import net.runelite.cache.definitions.FramemapDefinition;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.definitions.KitDefinition;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.NpcDefinition;
import net.runelite.cache.definitions.SequenceDefinition;
import net.runelite.cache.definitions.SpriteDefinition;
import net.runelite.cache.definitions.TextureDefinition;
import net.runelite.cache.definitions.loaders.FrameLoader;
import net.runelite.cache.definitions.loaders.FramemapLoader;
import net.runelite.cache.definitions.loaders.KitLoader;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.definitions.loaders.NpcLoader;
import net.runelite.cache.definitions.loaders.SequenceLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.ArchiveFiles;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Storage;
import net.runelite.cache.fs.Store;
import net.runelite.cache.item.ItemSpriteFactory;
import net.runelite.cache.models.FaceNormal;
import net.runelite.cache.models.VertexNormal;

/**
 * Exports everything the browser version draws from a local OSRS cache: interface sprites, item
 * icons, the game fonts, NPC/player/gear models with their animations, and each map location
 * (collision, painted ground and scenery). It runs offline on the JVM, using the artwork tables
 * (GearArt, ExportTables) and map code (AreaBuilder). Models are lit here like the client lights
 * them and resolved to one colour per face; animation happens in the browser.
 *
 * <p>Output: {@code data.js} (manifest, images, fonts, models), {@code loc/NAME.js} per location
 * and {@code manifest.json} for reference.
 */
public final class WebExport
{
	private static final int CONFIG_UNDERLAY = 1;
	private static final int CONFIG_KIT = 3;
	private static final int CONFIG_OVERLAY = 4;
	private static final int CONFIG_OBJECT = 6;
	private static final int CONFIG_NPC = 9;
	private static final int CONFIG_SEQUENCE = 12;

	/** Object placement types. */
	private static final int WALL_L = 2;
	private static final int CENTREPIECE = 10;
	private static final int CENTREPIECE_DIAGONAL = 11;

	/** ModelData defaults (NPCs and scenery), and the client's lighting for player models. */
	private static final int[] DEFAULT_LIGHT = {64, 768, -50, -10, -50};
	private static final int[] PLAYER_LIGHT = {64, 850, -30, -50, -30};
	private static final double TEXTURE_LIGHT = 80;

	/** Blob format version, checked by the browser. */
	static final int MODELS_VERSION = 2;
	/** Edge of a texture as the client loads it, and as exported for worn items (smaller, still sharp on a sprite). */
	private static final int TEXTURE_SIZE = 128;
	private static final int EXPORTED_TEXTURE_SIZE = 64;

	private final Store store;
	private final Storage storage;
	private final Index configs;
	private final Index models;
	private final Index frames;
	private final Index skeletons;
	private final Index maps;
	private final ItemManager items;
	private final SpriteManager sprites;
	private final TextureManager textures;
	private final int[] palette = JagexPalette.build(0.7);
	private final Map<Integer, ArchiveFiles> configGroups = new HashMap<>();
	private final Map<Integer, Integer> configRevisions = new HashMap<>();
	private final Map<Integer, TextureDefinition> textureDefs = new HashMap<>();
	/** Texture pixels by texture id, loaded on first use; an empty array when they can't be. */
	private final Map<Integer, int[]> texturePixels = new HashMap<>();
	/** Textures drawn on worn items, written after the models. */
	private final Set<Integer> usedTextures = new TreeSet<>();
	private final Map<Integer, FramemapDefinition> framemaps = new TreeMap<>();
	private final Map<Integer, SequenceDefinition> sequences = new TreeMap<>();
	/** Lit, animatable models by name, in the order written. */
	private final Map<String, LitModel> actorModels = new LinkedHashMap<>();
	/** PNG images by name ("i" + item id or "s" + sprite id). */
	private final Map<String, byte[]> images = new TreeMap<>();
	private final Map<String, Object> manifest = new LinkedHashMap<>();
	private final Map<String, String> icons = new TreeMap<>();

	/** A lit model: vertex positions with animation labels, faces with resolved ARGB colours. */
	static final class LitModel
	{
		int[] x;
		int[] y;
		int[] z;
		/** Animation label per vertex, -1 for none. */
		int[] labels;
		int[] a;
		int[] b;
		int[] c;
		int[] argb;
		/** Textured faces (worn items only): texture id, u and v at each corner, and light at each corner. */
		int[] texFaces = new int[0];
		int[] texIds = new int[0];
		float[][] texUv = new float[0][];
		float[][] texLight = new float[0][];

		int vertexCount()
		{
			return x.length;
		}

		MeshSnapshot toMesh()
		{
			float[] fx = new float[x.length];
			float[] fy = new float[x.length];
			float[] fz = new float[x.length];
			for (int i = 0; i < x.length; i++)
			{
				fx[i] = x[i];
				fy[i] = y[i];
				fz[i] = z[i];
			}
			return new MeshSnapshot(fx, fy, fz, a.clone(), b.clone(), c.clone(), argb.clone());
		}

		static LitModel concat(List<LitModel> parts)
		{
			int nv = 0;
			int nf = 0;
			for (LitModel p : parts)
			{
				nv += p.x.length;
				nf += p.a.length;
			}
			LitModel m = new LitModel();
			m.x = new int[nv];
			m.y = new int[nv];
			m.z = new int[nv];
			m.labels = new int[nv];
			m.a = new int[nf];
			m.b = new int[nf];
			m.c = new int[nf];
			m.argb = new int[nf];
			int vo = 0;
			int fo = 0;
			for (LitModel p : parts)
			{
				int pv = p.x.length;
				System.arraycopy(p.x, 0, m.x, vo, pv);
				System.arraycopy(p.y, 0, m.y, vo, pv);
				System.arraycopy(p.z, 0, m.z, vo, pv);
				System.arraycopy(p.labels, 0, m.labels, vo, pv);
				for (int f = 0; f < p.a.length; f++)
				{
					m.a[fo + f] = p.a[f] + vo;
					m.b[fo + f] = p.b[f] + vo;
					m.c[fo + f] = p.c[f] + vo;
					m.argb[fo + f] = p.argb[f];
				}
				vo += pv;
				fo += p.a.length;
			}
			return m;
		}
	}

	private WebExport(File cacheDir) throws IOException
	{
		store = new Store(cacheDir);
		store.load();
		storage = store.getStorage();
		configs = store.getIndex(IndexType.CONFIGS);
		models = store.getIndex(IndexType.MODELS);
		frames = store.getIndex(IndexType.ANIMATIONS);
		skeletons = store.getIndex(IndexType.SKELETONS);
		maps = store.getIndex(IndexType.MAPS);
		items = new ItemManager(store);
		items.load();
		sprites = new SpriteManager(store);
		sprites.load();
		textures = new TextureManager(store);
		textures.load();
		for (TextureDefinition t : textures.getTextures())
		{
			textureDefs.put(t.getId(), t);
		}
	}

	public static void main(String[] args) throws IOException
	{
		if (args.length != 2)
		{
			System.err.println("usage: WebExport <OSRS cache directory> <output directory>");
			System.exit(2);
		}
		File cacheDir = new File(args[0]);
		if (!new File(cacheDir, "main_file_cache.dat2").isFile())
		{
			System.err.println("No OSRS cache in " + cacheDir + ". Run the game client once, or pass -PcacheDir=<dir>.");
			System.exit(1);
		}
		File out = new File(args[1]);
		Files.createDirectories(new File(out, "loc").toPath());
		WebExport export = new WebExport(cacheDir);
		export.exportIcons();
		export.exportNpcs();
		export.exportPlayer();
		export.writeData(out);
		export.exportLocations(new File(out, "loc"));
		System.out.println("Exported to " + out.getAbsolutePath());
	}

	// ---------------------------------------------------------------- icons and sprites

	private void exportIcons() throws IOException
	{
		for (UiSprite s : UiSprite.values())
		{
			int item = ExportTables.itemSprite(s);
			icon("ui." + s.name(), item >= 0 ? item(item) : sprite(ExportTables.spriteId(s)));
		}
		for (Gear g : Gear.values())
		{
			icon("gear." + g.name(), item(GearArt.itemId(g)));
			icon("spell." + g.name(), sprite(GearArt.spellSprite(g)));
		}
		for (Passive p : Passive.values())
		{
			int itemId = GearArt.passiveItem(p);
			for (int rank = 0; rank <= p.getMaxRank() + 1; rank++)
			{
				icon("passive." + p.name() + "." + rank, itemId >= 0 ? item(itemId) : sprite(GearArt.passiveSprite(p, rank)));
			}
		}
		for (Perk p : Perk.values())
		{
			int sprite = GearArt.perkSprite(p);
			icon("perk." + p.name(), sprite >= 0 ? sprite(sprite) : item(GearArt.perkItem(p)));
		}
		for (Pet p : Pet.values())
		{
			icon("pet." + p.name(), item(GearArt.petItem(p)));
		}
		for (Location l : Location.values())
		{
			Integer itemId = ExportTables.LOCATION_ICONS.get(l);
			icon("location." + l.name(), itemId == null ? null : item(itemId));
		}
		for (int xp : new int[]{1, 2, 4, 8, 15, 25})
		{
			icon("gem." + xp, item(GearArt.gemItem(xp)));
		}
		// offer and prize icons (GearArt.prizeItem)
		icon("offer.coins", item(ItemID.COINS_250));
		for (Food food : Food.values())
		{
			icon("food." + food.name(), item(GearArt.foodItem(food)));
		}
		for (Boost boost : Boost.values())
		{
			for (int doses = 1; doses <= Boost.DOSES; doses++)
			{
				icon("boost." + boost.name() + "." + doses, item(GearArt.boostItem(boost, doses)));
			}
		}
		icon("prize.coins", item(ItemID.COINS_10000));
		icon("prize.lamp", item(ItemID.MACRO_GENILAMP));
		manifest.put("icons", icons);
	}

	private void icon(String key, String image)
	{
		if (image != null)
		{
			icons.put(key, image);
		}
	}

	/** Exports an item icon; returns its image name or null. */
	private String item(int itemId) throws IOException
	{
		if (itemId < 0)
		{
			return null;
		}
		String name = "i" + itemId;
		if (!images.containsKey(name))
		{
			BufferedImage image = ItemSpriteFactory.createSprite(items, this::model, sprites, textures, itemId, 1, 1, 0x302020, false);
			if (image == null)
			{
				System.out.println("No icon for item " + itemId);
				return null;
			}
			images.put(name, png(image));
		}
		return name;
	}

	private String sprite(int spriteId) throws IOException
	{
		if (spriteId < 0)
		{
			return null;
		}
		String name = "s" + spriteId;
		if (!images.containsKey(name))
		{
			SpriteDefinition def = sprites.findSprite(spriteId, 0);
			if (def == null || def.getWidth() <= 0)
			{
				System.out.println("No sprite " + spriteId);
				return null;
			}
			images.put(name, png(sprites.getSpriteImage(def)));
		}
		return name;
	}

	private static byte[] png(BufferedImage image) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return out.toByteArray();
	}

	// ---------------------------------------------------------------- cache access

	private ModelDefinition model(int id) throws IOException
	{
		Archive archive = models.getArchive(id);
		if (archive == null)
		{
			return null;
		}
		byte[] data = archive.decompress(storage.loadArchive(archive));
		ModelDefinition m = new ModelLoader().load(id, data);
		restoreLabels(m);
		return m;
	}

	/**
	 * The loader groups vertices by label (computeAnimationTables) and drops the per-vertex
	 * labels the export writes; rebuild them from the groups so animations still move the model.
	 */
	private static void restoreLabels(ModelDefinition m)
	{
		int[][] groups = m.getVertexGroups();
		if (m.packedVertexGroups != null || groups == null)
		{
			return;
		}
		int[] labels = new int[m.vertexCount];
		Arrays.fill(labels, -1);
		for (int label = 0; label < groups.length; label++)
		{
			if (groups[label] != null)
			{
				for (int v : groups[label])
				{
					labels[v] = label;
				}
			}
		}
		m.packedVertexGroups = labels;
	}

	private byte[] config(int group, int id)
	{
		ArchiveFiles files = configGroups.computeIfAbsent(group, g ->
		{
			try
			{
				Archive archive = configs.getArchive(g);
				configRevisions.put(g, archive.getRevision());
				return archive.getFiles(storage.loadArchive(archive));
			}
			catch (IOException e)
			{
				throw new UncheckedIOException(e);
			}
		});
		FSFile file = files.findFile(id);
		return file == null ? null : file.getContents();
	}

	private SequenceDefinition sequence(int id)
	{
		if (id < 0)
		{
			return null;
		}
		SequenceDefinition cached = sequences.get(id);
		if (cached != null)
		{
			return cached;
		}
		byte[] data = config(CONFIG_SEQUENCE, id);
		if (data == null)
		{
			return null;
		}
		SequenceDefinition seq = new SequenceLoader().configureForRevision(configRevisions.get(CONFIG_SEQUENCE)).load(id, data);
		sequences.put(id, seq);
		return seq;
	}

	/** True for animations made of classic frames, which the browser can play. */
	private boolean playable(int id)
	{
		SequenceDefinition seq = sequence(id);
		return seq != null && seq.getAnimMayaID() < 0 && seq.frameIDs != null && seq.frameIDs.length > 0;
	}

	/** Keeps a sequence for export (with its frames); returns its id, or -1 if it can't be played. */
	private int keepSequence(int id)
	{
		if (id < 0)
		{
			return -1;
		}
		if (!playable(id))
		{
			sequences.remove(id);
			System.out.println("Skipping animation " + id + " (missing or skeletal)");
			return -1;
		}
		return id;
	}

	private final Map<Integer, FrameDefinition> frameCache = new HashMap<>();
	private final Map<Integer, ArchiveFiles> frameGroups = new HashMap<>();

	private FrameDefinition frame(int frameId) throws IOException
	{
		FrameDefinition cached = frameCache.get(frameId);
		if (cached != null)
		{
			return cached;
		}
		int group = frameId >>> 16;
		ArchiveFiles files = frameGroups.get(group);
		if (files == null)
		{
			Archive archive = frames.getArchive(group);
			files = archive.getFiles(storage.loadArchive(archive));
			frameGroups.put(group, files);
		}
		FSFile file = files.findFile(frameId & 0xffff);
		byte[] contents = file.getContents();
		int framemapId = (contents[0] & 0xff) << 8 | contents[1] & 0xff;
		FramemapDefinition framemap = framemaps.get(framemapId);
		if (framemap == null)
		{
			Archive archive = skeletons.getArchive(framemapId);
			byte[] data = archive.decompress(storage.loadArchive(archive));
			framemap = new FramemapLoader().load(framemapId, data);
			framemaps.put(framemapId, framemap);
		}
		FrameDefinition frame = new FrameLoader().load(framemap, frameId & 0xffff, contents);
		frameCache.put(frameId, frame);
		return frame;
	}

	/** Packed HSL of a texture's average colour, or -1. */
	private int textureHsl(int texture)
	{
		TextureDefinition t = textureDefs.get(texture);
		int hsl = t == null ? 0 : t.missingColor & 0xffff;
		return hsl == 0 ? -1 : hsl;
	}

	// ---------------------------------------------------------------- lighting

	private static ModelDefinition merge(List<ModelDefinition> parts)
	{
		if (parts.size() == 1)
		{
			return parts.get(0);
		}
		ModelDefinition m = new ModelDefinition();
		int nv = 0;
		int nf = 0;
		boolean labels = false;
		boolean types = false;
		boolean alphas = false;
		boolean tex = false;
		for (ModelDefinition p : parts)
		{
			nv += p.vertexCount;
			nf += p.faceCount;
			labels |= p.packedVertexGroups != null;
			types |= p.faceRenderTypes != null;
			alphas |= p.faceTransparencies != null;
			tex |= p.faceTextures != null;
		}
		m.vertexCount = nv;
		m.faceCount = nf;
		m.vertexX = new int[nv];
		m.vertexY = new int[nv];
		m.vertexZ = new int[nv];
		m.packedVertexGroups = labels ? new int[nv] : null;
		m.faceIndices1 = new int[nf];
		m.faceIndices2 = new int[nf];
		m.faceIndices3 = new int[nf];
		m.faceColors = new short[nf];
		m.faceRenderTypes = types ? new byte[nf] : null;
		m.faceTransparencies = alphas ? new byte[nf] : null;
		m.faceTextures = tex ? new short[nf] : null;
		int vo = 0;
		int fo = 0;
		for (ModelDefinition p : parts)
		{
			System.arraycopy(p.vertexX, 0, m.vertexX, vo, p.vertexCount);
			System.arraycopy(p.vertexY, 0, m.vertexY, vo, p.vertexCount);
			System.arraycopy(p.vertexZ, 0, m.vertexZ, vo, p.vertexCount);
			if (labels)
			{
				for (int i = 0; i < p.vertexCount; i++)
				{
					m.packedVertexGroups[vo + i] = p.packedVertexGroups == null ? -1 : p.packedVertexGroups[i];
				}
			}
			for (int f = 0; f < p.faceCount; f++)
			{
				m.faceIndices1[fo + f] = p.faceIndices1[f] + vo;
				m.faceIndices2[fo + f] = p.faceIndices2[f] + vo;
				m.faceIndices3[fo + f] = p.faceIndices3[f] + vo;
				m.faceColors[fo + f] = p.faceColors[f];
				if (types)
				{
					m.faceRenderTypes[fo + f] = p.faceRenderTypes == null ? 0 : p.faceRenderTypes[f];
				}
				if (alphas)
				{
					m.faceTransparencies[fo + f] = p.faceTransparencies == null ? 0 : p.faceTransparencies[f];
				}
				if (tex)
				{
					m.faceTextures[fo + f] = p.faceTextures == null ? -1 : p.faceTextures[f];
				}
			}
			vo += p.vertexCount;
			fo += p.faceCount;
		}
		return m;
	}

	private static int lightHsl(int hsl, int light)
	{
		light = light * (hsl & 127) >> 7;
		return (hsl & 0xff80) + bound2to126(light);
	}

	private static int bound2to126(int light)
	{
		return light < 2 ? 2 : Math.min(126, light);
	}

	/**
	 * Lights a model the way the client's ModelData.light does, then resolves each face to one
	 * ARGB colour (textured faces take the texture's average colour).
	 */
	private LitModel light(ModelDefinition m, int[] light)
	{
		return light(m, light, false);
	}

	/**
	 * @param textured keep textured faces' mapping so the browser can draw the texture itself
	 *                 (worn items: capes, whose lava also flows)
	 */
	private LitModel light(ModelDefinition m, int[] light, boolean textured)
	{
		int ambient = light[0];
		int contrast = light[1];
		int lx = light[2];
		int ly = light[3];
		int lz = light[4];
		m.computeNormals();
		int magnitude = (int) Math.sqrt(lx * lx + ly * ly + lz * lz);
		int k = magnitude * contrast >> 8;
		int faces = m.faceCount;
		float[][] us = null;
		float[][] vs = null;
		if (textured && m.faceTextures != null)
		{
			try
			{
				m.computeTextureUVCoordinates();
				us = m.faceTextureUCoordinates;
				vs = m.faceTextureVCoordinates;
			}
			catch (RuntimeException e)
			{
				// merged models lose their texture mapping; their textured faces stay one colour
			}
		}
		List<Integer> texFaces = new ArrayList<>();
		List<Integer> texIds = new ArrayList<>();
		List<float[]> texUv = new ArrayList<>();
		List<float[]> texLight = new ArrayList<>();
		LitModel out = new LitModel();
		out.x = m.vertexX.clone();
		out.y = m.vertexY.clone();
		out.z = m.vertexZ.clone();
		out.labels = new int[m.vertexCount];
		for (int i = 0; i < m.vertexCount; i++)
		{
			out.labels[i] = m.packedVertexGroups == null ? -1 : m.packedVertexGroups[i];
		}
		out.a = new int[faces];
		out.b = new int[faces];
		out.c = new int[faces];
		out.argb = new int[faces];
		for (int f = 0; f < faces; f++)
		{
			int va = m.faceIndices1[f];
			int vb = m.faceIndices2[f];
			int vc = m.faceIndices3[f];
			out.a[f] = va;
			out.b[f] = vb;
			out.c[f] = vc;
			int type = m.faceRenderTypes == null ? 0 : m.faceRenderTypes[f];
			int alpha = m.faceTransparencies == null ? 0 : m.faceTransparencies[f];
			int texture = m.faceTextures == null ? -1 : m.faceTextures[f];
			if (alpha == -2)
			{
				type = 3;
			}
			if (alpha == -1)
			{
				type = 2;
			}
			int c1;
			int c2 = 0;
			int c3;
			if (type == 2)
			{
				// hidden
				continue;
			}
			int colour = m.faceColors[f] & 0xffff;
			if (type == 3)
			{
				c1 = 128;
				c3 = -1;
			}
			else if (type == 1)
			{
				FaceNormal n = m.faceNormals[f];
				int l = (ly * n.y + lz * n.z + lx * n.x) / (k / 2 + k) + ambient;
				c1 = texture == -1 ? lightHsl(colour, l) : bound2to126(l);
				c3 = -1;
			}
			else
			{
				c1 = vertexLight(m, va, colour, texture, k, ambient, lx, ly, lz);
				c2 = vertexLight(m, vb, colour, texture, k, ambient, lx, ly, lz);
				c3 = vertexLight(m, vc, colour, texture, k, ambient, lx, ly, lz);
			}
			int rgb;
			if (texture != -1 && us != null && f < us.length && us[f] != null && vs[f] != null && texturePixels(texture).length > 0)
			{
				texFaces.add(f);
				texIds.add(texture);
				texUv.add(new float[]{us[f][0], vs[f][0], us[f][1], vs[f][1], us[f][2], vs[f][2]});
				double l1 = (c1 & 0x7f) / TEXTURE_LIGHT;
				double l2 = c3 == -1 ? l1 : (c2 & 0x7f) / TEXTURE_LIGHT;
				double l3 = c3 == -1 ? l1 : (c3 & 0x7f) / TEXTURE_LIGHT;
				texLight.add(new float[]{(float) l1, (float) l2, (float) l3});
				usedTextures.add(texture);
			}
			if (texture != -1)
			{
				int hsl = textureHsl(texture);
				if (hsl >= 0)
				{
					rgb = ExportTables.shade(palette[hsl], (c1 & 0x7f) / TEXTURE_LIGHT);
				}
				else
				{
					int l = Math.min(255, (c1 & 0x7f) * 2);
					rgb = l * 9 / 10 << 16 | l * 8 / 10 << 8 | l * 7 / 10;
				}
			}
			else if (c3 == -1)
			{
				rgb = palette[c1 & 0xffff];
			}
			else
			{
				rgb = ExportTables.average(palette[c1 & 0xffff], palette[c2 & 0xffff], palette[c3 & 0xffff]);
			}
			int a = m.faceTransparencies == null ? 255 : 255 - (m.faceTransparencies[f] & 0xff);
			out.argb[f] = a << 24 | rgb;
		}
		out.texFaces = new int[texFaces.size()];
		out.texIds = new int[texFaces.size()];
		out.texUv = texUv.toArray(new float[0][]);
		out.texLight = texLight.toArray(new float[0][]);
		for (int i = 0; i < out.texFaces.length; i++)
		{
			out.texFaces[i] = texFaces.get(i);
			out.texIds[i] = texIds.get(i);
		}
		return out;
	}

	/** A texture's pixels (RGB, 0 for see-through), as the client loads it; empty if it can't be read. */
	private int[] texturePixels(int texture)
	{
		int[] cached = texturePixels.get(texture);
		if (cached == null)
		{
			TextureDefinition t = textureDefs.get(texture);
			cached = t != null && t.method2680(1.0, TEXTURE_SIZE, sprites) && t.pixels != null ? t.pixels.clone() : new int[0];
			texturePixels.put(texture, cached);
		}
		return cached;
	}

	/** A texture shrunk to {@link #EXPORTED_TEXTURE_SIZE}, a texel see-through when most of its source is. */
	private int[] smallTexture(int texture)
	{
		int[] pixels = texturePixels(texture);
		int size = (int) Math.round(Math.sqrt(pixels.length));
		int step = Math.max(1, size / EXPORTED_TEXTURE_SIZE);
		int out = size / step;
		int[] small = new int[out * out];
		for (int y = 0; y < out; y++)
		{
			for (int x = 0; x < out; x++)
			{
				int r = 0;
				int g = 0;
				int b = 0;
				int seen = 0;
				for (int dy = 0; dy < step; dy++)
				{
					for (int dx = 0; dx < step; dx++)
					{
						int c = pixels[(y * step + dy) * size + x * step + dx];
						if (c != 0)
						{
							r += c >> 16 & 0xff;
							g += c >> 8 & 0xff;
							b += c & 0xff;
							seen++;
						}
					}
				}
				// 0 stays see-through; a seen texel is never pure black 0
				small[y * out + x] = seen * 2 < step * step ? 0 : Math.max(1, (r / seen) << 16 | (g / seen) << 8 | (b / seen));
			}
		}
		return small;
	}

	private static int vertexLight(ModelDefinition m, int v, int colour, int texture, int k, int ambient, int lx, int ly, int lz)
	{
		VertexNormal n = m.vertexNormals[v];
		int l = (ly * n.y + lz * n.z + lx * n.x) / (k * Math.max(1, n.magnitude)) + ambient;
		return texture == -1 ? lightHsl(colour, l) : bound2to126(l);
	}

	private static void recolour(ModelDefinition m, short[] from, short[] to, short[] texFrom, short[] texTo)
	{
		for (int i = 0; from != null && to != null && i < Math.min(from.length, to.length); i++)
		{
			m.recolor(from[i], to[i]);
		}
		for (int i = 0; texFrom != null && texTo != null && m.faceTextures != null && i < Math.min(texFrom.length, texTo.length); i++)
		{
			m.retexture(texFrom[i], texTo[i]);
		}
	}

	private ModelDefinition loadMerged(int... ids) throws IOException
	{
		List<ModelDefinition> parts = new ArrayList<>();
		for (int id : ids)
		{
			if (id < 0)
			{
				continue;
			}
			ModelDefinition m = model(id);
			if (m == null)
			{
				return null;
			}
			parts.add(m);
		}
		return parts.isEmpty() ? null : merge(parts);
	}

	// ---------------------------------------------------------------- NPCs

	private void exportNpcs() throws IOException
	{
		Map<String, Object> npcs = new LinkedHashMap<>();
		for (EnemyKind kind : EnemyKind.values())
		{
			ExportTables.NpcLook look = ExportTables.LOOKS.get(kind);
			byte[] data = look == null ? null : config(CONFIG_NPC, look.npcId);
			if (data == null)
			{
				System.out.println("No NPC for " + kind);
				continue;
			}
			NpcDefinition def = new NpcLoader().configureForRevision(configRevisions.get(CONFIG_NPC)).load(look.npcId, data);
			ModelDefinition model = def.models == null ? null : loadMerged(def.models);
			if (model == null)
			{
				System.out.println("No model for " + kind);
				continue;
			}
			recolour(model, def.recolorToFind, def.recolorToReplace, def.retextureToFind, def.retextureToReplace);
			if (def.widthScale != 128 || def.heightScale != 128)
			{
				model.resize(def.widthScale, def.heightScale, def.widthScale);
			}
			String name = "npc." + kind.name();
			actorModels.put(name, light(model, DEFAULT_LIGHT));
			int walk = look.walkAnimation >= 0 ? look.walkAnimation : def.walkingAnimation;
			int idle = look.idleAnimation >= 0 ? look.idleAnimation : def.standingAnimation;
			Map<String, Object> npc = new LinkedHashMap<>();
			npc.put("model", name);
			npc.put("walk", keepSequence(walk));
			npc.put("idle", keepSequence(idle));
			// bosses many tiles across are drawn at most MAX_NPC_TILES wide
			npc.put("scale", Math.min(1, ExportTables.MAX_NPC_TILES / Math.max(1, def.size)));
			npcs.put(kind.name(), npc);
		}
		manifest.put("npcs", npcs);
	}

	// ---------------------------------------------------------------- the player

	private void exportPlayer() throws IOException
	{
		Map<String, Object> player = new LinkedHashMap<>();
		// the default male body (ExportTables.DEFAULT_KITS), by equipment slot
		Map<String, Object> kits = new LinkedHashMap<>();
		int[] defaults = ExportTables.DEFAULT_KITS[0];
		for (int i = 0; i < ExportTables.BODY_KITS.length; i++)
		{
			if (defaults[i] < 0)
			{
				continue;
			}
			KitDefinition kit = new KitLoader().load(defaults[i], config(CONFIG_KIT, defaults[i]));
			ModelDefinition model = loadMerged(kit.models);
			if (model == null)
			{
				continue;
			}
			recolour(model, kit.recolorToFind, kit.recolorToReplace, kit.retextureToFind, kit.retextureToReplace);
			String name = "kit." + ExportTables.BODY_KITS[i].name();
			actorModels.put(name, light(model, PLAYER_LIGHT));
			kits.put(String.valueOf(ExportTables.BODY_KITS[i].getIndex()), name);
		}
		player.put("kits", kits);

		Map<String, Object> gear = new LinkedHashMap<>();
		Map<Style, Integer> fallbackAttacks = new HashMap<>();
		for (Gear g : Gear.values())
		{
			int attack = GearArt.attackAnimation(g);
			if (g.getSlot() == Slot.WEAPON && playable(attack))
			{
				fallbackAttacks.putIfAbsent(g.getStyle(), attack);
			}
		}
		for (Gear g : Gear.values())
		{
			Map<String, Object> entry = new LinkedHashMap<>();
			KitType kit = ExportTables.kitFor(g.getSlot());
			ItemDefinition def = items.getItem(GearArt.itemId(g));
			if (kit != null && def != null && def.maleModel0 >= 0)
			{
				ModelDefinition model = loadMerged(def.maleModel0, def.maleModel1, def.maleModel2);
				if (model != null)
				{
					if (def.maleOffset != 0)
					{
						model.move(0, def.maleOffset, 0);
					}
					recolour(model, def.colorFind, def.colorReplace, def.textureFind, def.textureReplace);
					String name = "gear." + g.name();
					actorModels.put(name, light(model, PLAYER_LIGHT, true));
					entry.put("model", name);
				}
				entry.put("slot", kit.getIndex());
				List<Integer> hides = new ArrayList<>();
				for (int pos : new int[]{def.wearPos2, def.wearPos3})
				{
					if (pos >= 0 && pos != kit.getIndex())
					{
						hides.add(pos);
					}
				}
				if (g.hidesHair())
				{
					hides.add(KitType.HAIR.getIndex());
				}
				if (g.hidesJaw())
				{
					hides.add(KitType.JAW.getIndex());
				}
				entry.put("hides", hides);
			}
			if (g.getSlot() == Slot.WEAPON)
			{
				int attack = GearArt.attackAnimation(g);
				entry.put("attack", playable(attack) ? keepSequence(attack) : keepSequence(fallbackAttacks.getOrDefault(g.getStyle(), -1)));
			}
			gear.put(g.name(), entry);
		}
		player.put("gear", gear);
		player.put("punch", keepSequence(GearArt.attackAnimation(null)));
		player.put("ready", keepSequence(GearArt.READY));
		player.put("walk", keepSequence(GearArt.WALK));
		manifest.put("player", player);
		// the item ids PlayerComposition would hold, for reference
		manifest.put("itemOffset", PlayerComposition.ITEM_OFFSET);
	}

	// ---------------------------------------------------------------- writing

	private void writeData(File out) throws IOException
	{
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		manifest.put("modelsVersion", MODELS_VERSION);
		String manifestJson = gson.toJson(manifest);
		Files.write(new File(out, "manifest.json").toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(manifest)
			.getBytes(StandardCharsets.UTF_8));

		StringBuilder js = new StringBuilder();
		js.append("// Generated by WebExport from the OSRS cache. Old School RuneScape artwork is the property of Jagex Ltd.\n");
		js.append("window.OSRS_DATA = {\n");
		js.append("manifest: ").append(gson.toJson(manifestJson)).append(",\n");
		js.append("fonts: {\n");
		js.append("regular: \"").append(resource("net/runelite/client/ui/runescape.ttf")).append("\",\n");
		js.append("bold: \"").append(resource("net/runelite/client/ui/runescape_bold.ttf")).append("\",\n");
		js.append("small: \"").append(resource("net/runelite/client/ui/runescape_small.ttf")).append("\"\n");
		js.append("},\n");
		js.append("images: {\n");
		boolean first = true;
		for (Map.Entry<String, byte[]> e : images.entrySet())
		{
			js.append(first ? "" : ",\n").append('"').append(e.getKey()).append("\": \"")
				.append(Base64.getEncoder().encodeToString(e.getValue())).append('"');
			first = false;
		}
		js.append("\n},\n");
		js.append("models: \"").append(Base64.getEncoder().encodeToString(gzip(actorBlob()))).append("\"\n");
		js.append("};\n");
		Files.write(new File(out, "data.js").toPath(), js.toString().getBytes(StandardCharsets.UTF_8));
		System.out.println(images.size() + " images, " + actorModels.size() + " models, " + sequences.size() + " animations");
	}

	private static String resource(String path) throws IOException
	{
		try (InputStream in = WebExport.class.getClassLoader().getResourceAsStream(path))
		{
			if (in == null)
			{
				throw new IOException("Missing resource " + path);
			}
			return Base64.getEncoder().encodeToString(in.readAllBytes());
		}
	}

	private static byte[] gzip(byte[] data) throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream gz = new GZIPOutputStream(bytes))
		{
			gz.write(data);
		}
		return bytes.toByteArray();
	}

	/**
	 * Animations, skeletons and lit models, read by the browser's ActorData:
	 * version; sequences (id, frame lengths, frames as framemap + transforms); framemaps
	 * (id, per group: type and labels); models (name, vertices with labels, faces with ARGB).
	 */
	private byte[] actorBlob() throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		out.writeInt(MODELS_VERSION);

		// frames are loaded first so every framemap they use is known
		Map<Integer, List<FrameDefinition>> seqFrames = new LinkedHashMap<>();
		for (SequenceDefinition seq : sequences.values())
		{
			List<FrameDefinition> list = new ArrayList<>();
			for (int id : seq.frameIDs)
			{
				list.add(frame(id));
			}
			seqFrames.put(seq.getId(), list);
		}
		out.writeInt(sequences.size());
		for (SequenceDefinition seq : sequences.values())
		{
			List<FrameDefinition> list = seqFrames.get(seq.getId());
			out.writeInt(seq.getId());
			out.writeInt(list.size());
			for (int i = 0; i < list.size(); i++)
			{
				FrameDefinition f = list.get(i);
				out.writeInt(seq.frameLengths == null || i >= seq.frameLengths.length ? 1 : seq.frameLengths[i]);
				out.writeInt(f.framemap.id);
				out.writeInt(f.translatorCount);
				for (int t = 0; t < f.translatorCount; t++)
				{
					out.writeShort(f.indexFrameIds[t]);
					out.writeInt(f.translator_x[t]);
					out.writeInt(f.translator_y[t]);
					out.writeInt(f.translator_z[t]);
				}
			}
		}

		out.writeInt(framemaps.size());
		for (FramemapDefinition fm : framemaps.values())
		{
			out.writeInt(fm.id);
			out.writeInt(fm.length);
			for (int i = 0; i < fm.length; i++)
			{
				out.writeByte(fm.types[i]);
				out.writeShort(fm.frameMaps[i].length);
				for (int label : fm.frameMaps[i])
				{
					out.writeShort(label);
				}
			}
		}

		out.writeInt(actorModels.size());
		for (Map.Entry<String, LitModel> e : actorModels.entrySet())
		{
			out.writeUTF(e.getKey());
			LitModel m = e.getValue();
			writeVertices(out, m.x, m.y, m.z);
			for (int label : m.labels)
			{
				out.writeShort(label);
			}
			writeFaces(out, m.a, m.b, m.c, m.argb);
			writeTexturedFaces(out, m);
		}

		// the textures those faces use: id, edge, scrolling (direction, speed), RGB texels
		out.writeInt(usedTextures.size());
		for (int id : usedTextures)
		{
			TextureDefinition t = textureDefs.get(id);
			int[] small = smallTexture(id);
			out.writeInt(id);
			out.writeInt((int) Math.round(Math.sqrt(small.length)));
			out.writeByte(t.animationSpeed > 0 ? t.animationDirection : 0);
			out.writeByte(t.animationSpeed);
			for (int texel : small)
			{
				out.writeInt(texel);
			}
		}
		out.flush();
		return bytes.toByteArray();
	}

	private static void writeVertices(DataOutputStream out, int[] x, int[] y, int[] z) throws IOException
	{
		out.writeInt(x.length);
		for (int i = 0; i < x.length; i++)
		{
			out.writeInt(x[i]);
			out.writeInt(y[i]);
			out.writeInt(z[i]);
		}
	}

	/**
	 * Textured faces: count, then per face its index among the written faces, its texture, u and
	 * v at each corner, and the light at each corner.
	 */
	private static void writeTexturedFaces(DataOutputStream out, LitModel m) throws IOException
	{
		int[] written = new int[m.argb.length];
		int n = 0;
		for (int f = 0; f < m.argb.length; f++)
		{
			written[f] = m.argb[f] >>> 24 != 0 ? n++ : -1;
		}
		List<Integer> kept = new ArrayList<>();
		for (int i = 0; i < m.texFaces.length; i++)
		{
			if (written[m.texFaces[i]] >= 0)
			{
				kept.add(i);
			}
		}
		out.writeInt(kept.size());
		for (int i : kept)
		{
			out.writeInt(written[m.texFaces[i]]);
			out.writeShort(m.texIds[i]);
			for (float uv : m.texUv[i])
			{
				out.writeFloat(uv);
			}
			for (float l : m.texLight[i])
			{
				out.writeFloat(l);
			}
		}
	}

	private static void writeFaces(DataOutputStream out, int[] a, int[] b, int[] c, int[] argb) throws IOException
	{
		// faces without colour are dropped; the browser draws only visible ones
		int visible = 0;
		for (int colour : argb)
		{
			visible += colour >>> 24 != 0 ? 1 : 0;
		}
		out.writeInt(visible);
		for (int f = 0; f < argb.length; f++)
		{
			if (argb[f] >>> 24 == 0)
			{
				continue;
			}
			out.writeInt(a[f]);
			out.writeInt(b[f]);
			out.writeInt(c[f]);
			out.writeInt(argb[f]);
		}
	}

	// ---------------------------------------------------------------- map locations

	/** Map data read from the cache. */
	private final class StoreMapSource implements MapSource
	{
		private final Map<Integer, MapSquare> squares = new HashMap<>();
		private final Map<Integer, FloorDefinition> underlays = new HashMap<>();
		private final Map<Integer, FloorDefinition> overlays = new HashMap<>();

		@Override
		public MapSquare square(int region)
		{
			return squares.computeIfAbsent(region, r ->
			{
				try
				{
					Archive archive = maps.getArchive(r);
					if (archive == null)
					{
						return null;
					}
					ArchiveFiles files = archive.getFiles(storage.loadArchive(archive));
					FSFile terrain = files.findFile(0);
					FSFile locations = files.findFile(1);
					return terrain == null || locations == null ? null : MapSquare.decode(terrain.getContents(), locations.getContents());
				}
				catch (IOException | RuntimeException e)
				{
					System.out.println("Unreadable map square " + r + ": " + e);
					return null;
				}
			});
		}

		@Override
		public ObjectDefinition object(int id)
		{
			byte[] data = config(CONFIG_OBJECT, id);
			return data == null ? null : ObjectDefinition.decode(id, data);
		}

		@Override
		public FloorDefinition underlay(int id)
		{
			return underlays.computeIfAbsent(id, i -> FloorDefinition.decode(config(CONFIG_UNDERLAY, i), false));
		}

		@Override
		public FloorDefinition overlay(int id)
		{
			return overlays.computeIfAbsent(id, i -> FloorDefinition.decode(config(CONFIG_OVERLAY, i), true));
		}

		@Override
		public int textureColour(int texture)
		{
			int hsl = textureHsl(texture);
			return hsl >= 0 ? palette[hsl] : -1;
		}
	}

	private void exportLocations(File dir) throws IOException
	{
		StoreMapSource source = new StoreMapSource();
		for (Location location : Location.values())
		{
			AreaBuilder.BuiltArea built = AreaBuilder.build(source, location.getWorldX(), location.getWorldY(), Location.SIZE);
			if (built == null)
			{
				System.out.println("No usable ground at " + location);
				continue;
			}
			Map<Long, Integer> meshIds = new HashMap<>();
			Map<Integer, ObjectDefinition> definitions = new HashMap<>();
			List<MeshSnapshot> meshes = new ArrayList<>();
			ByteArrayOutputStream propBytes = new ByteArrayOutputStream();
			DataOutputStream props = new DataOutputStream(propBytes);
			int propCount = 0;
			for (AreaBuilder.Placement p : built.placements)
			{
				long key = (long) p.objectId << 8 | p.type << 2 | p.orientation;
				Integer id = meshIds.get(key);
				if (id == null)
				{
					ObjectDefinition def = definitions.computeIfAbsent(p.objectId, source::object);
					MeshSnapshot mesh = def == null ? null : scenery(def, p.type, p.orientation);
					id = mesh == null ? -1 : meshes.size();
					if (mesh != null)
					{
						meshes.add(mesh);
					}
					meshIds.put(key, id);
				}
				if (id >= 0)
				{
					props.writeFloat((float) p.x);
					props.writeFloat((float) p.y);
					props.writeInt(id);
					propCount++;
				}
			}

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			out.writeInt(MODELS_VERSION);
			out.writeInt(AreaBuilder.PIXELS_PER_TILE);
			Arena arena = built.arena;
			out.writeInt(arena.getTilesX());
			out.writeInt(arena.getTilesY());
			out.writeInt(arena.getStartTileX());
			out.writeInt(arena.getStartTileY());
			for (int ty = 0; ty < arena.getTilesY(); ty++)
			{
				for (int tx = 0; tx < arena.getTilesX(); tx++)
				{
					out.writeByte((arena.isWalkable(tx, ty) ? 0 : 1) | (arena.isPropTile(tx, ty) ? 2 : 0));
					out.writeByte(arena.getWalls(tx, ty));
				}
			}
			out.writeInt(meshes.size());
			for (MeshSnapshot m : meshes)
			{
				writeMesh(out, m);
			}
			out.writeInt(propCount);
			out.write(propBytes.toByteArray());
			out.flush();

			String js = "// Generated by WebExport from the OSRS cache. Old School RuneScape artwork is the property of Jagex Ltd.\n"
				+ "OSRS_LOCATION(\"" + location.name() + "\", \"" + Base64.getEncoder().encodeToString(png(built.ground)) + "\", \""
				+ Base64.getEncoder().encodeToString(gzip(bytes.toByteArray())) + "\");\n";
			Files.write(new File(dir, location.name() + ".js").toPath(), js.getBytes(StandardCharsets.UTF_8));
			System.out.println(location + ": " + arena.getWalkableTiles() + " walkable tiles, " + propCount + " props, "
				+ meshes.size() + " meshes, " + js.length() / 1024 + " KB");
		}
	}

	private static void writeMesh(DataOutputStream out, MeshSnapshot m) throws IOException
	{
		int n = m.vertexCount();
		int[] x = new int[n];
		int[] y = new int[n];
		int[] z = new int[n];
		for (int i = 0; i < n; i++)
		{
			x[i] = Math.round(m.vertexX(i));
			y[i] = Math.round(m.vertexY(i));
			z[i] = Math.round(m.vertexZ(i));
		}
		writeVertices(out, x, y, z);
		int faces = m.faceCount();
		int[] a = new int[faces];
		int[] b = new int[faces];
		int[] c = new int[faces];
		int[] argb = new int[faces];
		for (int f = 0; f < faces; f++)
		{
			a[f] = m.faceA(f);
			b[f] = m.faceB(f);
			c[f] = m.faceC(f);
			argb[f] = m.faceArgb(f);
		}
		writeFaces(out, a, b, c, argb);
	}

	/** The object's models for a placement, lit and turned. */
	private MeshSnapshot scenery(ObjectDefinition def, int type, int orientation) throws IOException
	{
		if (type == WALL_L)
		{
			MeshSnapshot a = sceneryPart(def, WALL_L, orientation + 4, 0);
			MeshSnapshot b = sceneryPart(def, WALL_L, orientation + 1 & 3, 0);
			return a == null ? b : b == null ? a : MeshSnapshot.concat(a, b);
		}
		if (type == CENTREPIECE_DIAGONAL)
		{
			return sceneryPart(def, CENTREPIECE, orientation, Math.PI / 4);
		}
		return sceneryPart(def, type, orientation, 0);
	}

	private MeshSnapshot sceneryPart(ObjectDefinition def, int type, int orientation, double extraTurn) throws IOException
	{
		List<Integer> ids = new ArrayList<>();
		if (def.modelTypes == null)
		{
			if (type != CENTREPIECE)
			{
				return null;
			}
			for (int id : def.models)
			{
				ids.add(id);
			}
		}
		else
		{
			for (int i = 0; i < def.models.length; i++)
			{
				if (def.modelTypes[i] == type)
				{
					ids.add(def.models[i]);
				}
			}
		}
		if (ids.isEmpty())
		{
			return null;
		}
		ModelDefinition data = loadMerged(ids.stream().mapToInt(Integer::intValue).toArray());
		if (data == null)
		{
			return null;
		}
		recolour(data, def.recolorFrom, def.recolorTo, null, null);
		if (def.scaleX != 128 || def.scaleHeight != 128 || def.scaleY != 128)
		{
			data.resize(def.scaleX, def.scaleHeight, def.scaleY);
		}
		if (def.offsetX != 0 || def.offsetHeight != 0 || def.offsetY != 0)
		{
			data.move(def.offsetX, def.offsetHeight, def.offsetY);
		}
		LitModel lit = light(data, new int[]{def.ambient + DEFAULT_LIGHT[0], def.contrast + DEFAULT_LIGHT[1],
			DEFAULT_LIGHT[2], DEFAULT_LIGHT[3], DEFAULT_LIGHT[4]});
		boolean mirror = def.mirrored ^ orientation > 3;
		return ExportTables.turn(lit.toMesh(), mirror, orientation & 3, extraTurn);
	}
}
