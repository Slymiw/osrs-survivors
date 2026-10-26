package com.osrssurvivors;

import com.osrssurvivors.game.EnemyKind;
import com.osrssurvivors.game.Location;
import com.osrssurvivors.game.Slot;
import com.osrssurvivors.render.MeshSnapshot;
import com.osrssurvivors.render.UiSprite;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.kit.KitType;

/**
 * Which cache artwork the game uses besides gear (GearArt): NPC models and animations per
 * monster, location icons, interface sprites, the default player body, and the model helpers
 * the export shares with the map code.
 */
final class ExportTables
{
	private ExportTables()
	{
	}

	static final class NpcLook
	{
		final int npcId;
		final int walkAnimation;
		final int idleAnimation;

		NpcLook(int npcId, int walkAnimation, int idleAnimation)
		{
			this.npcId = npcId;
			this.walkAnimation = walkAnimation;
			this.idleAnimation = idleAnimation;
		}

		/** An NPC whose animations are read from its cache definition. */
		NpcLook(int npcId)
		{
			this(npcId, -1, -1);
		}
	}

	static final double MAX_NPC_TILES = 3;

	static final Map<EnemyKind, NpcLook> LOOKS = new EnumMap<>(EnemyKind.class);

	static
	{
		LOOKS.put(EnemyKind.RAT, new NpcLook(NpcID.GIANTRAT, AnimationID.GIANTRAT_WALK, AnimationID.GIANTRAT_READY));
		LOOKS.put(EnemyKind.ZOMBIE, new NpcLook(NpcID.ZOMBIE_UNARMED, AnimationID.ZOMBIE_WALK, AnimationID.ZOMBIE_READY));
		LOOKS.put(EnemyKind.SKELETON, new NpcLook(NpcID.SKELETON_UNARMED, AnimationID.SKELETON_WALK, AnimationID.SKELETON_READY));
		LOOKS.put(EnemyKind.MOSS_GIANT, new NpcLook(NpcID.MOSSGIANT, AnimationID.GIANT_WALK, AnimationID.GIANT_READY));
		LOOKS.put(EnemyKind.GREATER_DEMON, new NpcLook(NpcID.GREATER_DEMON, AnimationID.DEMON_WALK, AnimationID.DEMON_READY));
		LOOKS.put(EnemyKind.GREEN_DRAGON, new NpcLook(NpcID.GREEN_DRAGON, AnimationID.DRAGON_WALK, AnimationID.DRAGON_READY));
		LOOKS.put(EnemyKind.CHICKEN, new NpcLook(NpcID.CHICKEN));
		LOOKS.put(EnemyKind.GOBLIN, new NpcLook(NpcID.GOBLIN_UNARMED_MELEE_1));
		LOOKS.put(EnemyKind.MAN, new NpcLook(NpcID.MAN));
		LOOKS.put(EnemyKind.MONKEY, new NpcLook(NpcID.MONKEY));
		LOOKS.put(EnemyKind.CRAWLING_HAND, new NpcLook(NpcID.SLAYER_CRAWLING_HAND_1));
		LOOKS.put(EnemyKind.COW, new NpcLook(NpcID.COW));
		LOOKS.put(EnemyKind.GIANT_SPIDER, new NpcLook(NpcID.GIANTSPIDER1));
		LOOKS.put(EnemyKind.GUARD, new NpcLook(NpcID.FAI_VARROCK_GUARD));
		LOOKS.put(EnemyKind.DWARF, new NpcLook(NpcID.DWARF_NORMAL));
		LOOKS.put(EnemyKind.BARBARIAN, new NpcLook(NpcID.BARBARIAN));
		LOOKS.put(EnemyKind.SCORPION, new NpcLook(NpcID.SCORPION));
		LOOKS.put(EnemyKind.BANDIT, new NpcLook(NpcID.FOURDIAMONDS_SWORD_BANDIT_1));
		LOOKS.put(EnemyKind.WOLF, new NpcLook(NpcID.WOLF));
		LOOKS.put(EnemyKind.GHOUL, new NpcLook(NpcID.GHOUL));
		LOOKS.put(EnemyKind.JUNGLE_SPIDER, new NpcLook(NpcID.JUNGLE_SPIDER));
		LOOKS.put(EnemyKind.REVENANT_IMP, new NpcLook(NpcID.WILD_CAVE_IMP));
		LOOKS.put(EnemyKind.DARK_WIZARD, new NpcLook(NpcID.BEARDED_DARK_WIZARD));
		LOOKS.put(EnemyKind.CHAOS_DRUID, new NpcLook(NpcID.CHAOS_DRUID));
		LOOKS.put(EnemyKind.SKELETON_MAGE, new NpcLook(NpcID.SKELETONMAGE));
		LOOKS.put(EnemyKind.ARCHER, new NpcLook(NpcID.ARDOUGNE_ARCHER));
		LOOKS.put(EnemyKind.WHITE_KNIGHT, new NpcLook(NpcID.WHITE_KNIGHT));
		LOOKS.put(EnemyKind.BLACK_KNIGHT, new NpcLook(NpcID.BLACK_KNIGHT));
		LOOKS.put(EnemyKind.ICE_WARRIOR, new NpcLook(NpcID.ICEWARRIOR));
		LOOKS.put(EnemyKind.KALPHITE_WORKER, new NpcLook(NpcID.KALPHITE_WORKER));
		LOOKS.put(EnemyKind.BANSHEE, new NpcLook(NpcID.SLAYER_BANSHEE_1));
		LOOKS.put(EnemyKind.CROCODILE, new NpcLook(NpcID.ICS_LITTLE_CROC));
		LOOKS.put(EnemyKind.HILL_GIANT, new NpcLook(NpcID.GIANT));
		LOOKS.put(EnemyKind.ICE_GIANT, new NpcLook(NpcID.ICEGIANT));
		LOOKS.put(EnemyKind.JOGRE, new NpcLook(NpcID.JOGRE));
		LOOKS.put(EnemyKind.WEREWOLF, new NpcLook(NpcID.GODWARS_ANCIENT_WEREWOLF1));
		LOOKS.put(EnemyKind.FERAL_VAMPYRE, new NpcLook(NpcID.VAMPIRE_FLYER));
		LOOKS.put(EnemyKind.KALPHITE_SOLDIER, new NpcLook(NpcID.KALPHITE_SOLDIER));
		LOOKS.put(EnemyKind.FIRE_GIANT, new NpcLook(NpcID.FIREGIANT));
		LOOKS.put(EnemyKind.BLOODVELD, new NpcLook(NpcID.SLAYER_BLOODVELD));
		LOOKS.put(EnemyKind.LESSER_DEMON, new NpcLook(NpcID.LESSER_DEMON));
		LOOKS.put(EnemyKind.REVENANT_KNIGHT, new NpcLook(NpcID.WILD_CAVE_KNIGHT));
		LOOKS.put(EnemyKind.ABYSSAL_DEMON, new NpcLook(NpcID.SLAYER_ABYSSAL));
		LOOKS.put(EnemyKind.BLACK_DEMON, new NpcLook(NpcID.BLACK_DEMON));
		LOOKS.put(EnemyKind.CHAOS_ELEMENTAL, new NpcLook(NpcID.CHAOSELEMENTAL));
		LOOKS.put(EnemyKind.GIANT_MOLE, new NpcLook(NpcID.MOLE_GIANT));
		LOOKS.put(EnemyKind.SARACHNIS, new NpcLook(NpcID.SARACHNIS));
		LOOKS.put(EnemyKind.DAGANNOTH_REX, new NpcLook(NpcID.DAGCAVE_MELEE_BOSS));
		LOOKS.put(EnemyKind.SCORPIA, new NpcLook(NpcID.SCORPIA));
		LOOKS.put(EnemyKind.KALPHITE_QUEEN, new NpcLook(NpcID.KALPHITE_QUEEN));
		LOOKS.put(EnemyKind.KING_BLACK_DRAGON, new NpcLook(NpcID.KING_DRAGON));
		LOOKS.put(EnemyKind.GENERAL_GRAARDOR, new NpcLook(NpcID.GODWARS_BANDOS_AVATAR));
		LOOKS.put(EnemyKind.COMMANDER_ZILYANA, new NpcLook(NpcID.GODWARS_SARADOMIN_AVATAR));
		LOOKS.put(EnemyKind.KRIL_TSUTSAROTH, new NpcLook(NpcID.GODWARS_ZAMORAK_AVATAR));
		LOOKS.put(EnemyKind.KREEARRA, new NpcLook(NpcID.GODWARS_ARMADYL_AVATAR));
		LOOKS.put(EnemyKind.CERBERUS, new NpcLook(NpcID.CERBERUS_ATTACKING));
		LOOKS.put(EnemyKind.ZULRAH, new NpcLook(NpcID.SNAKEBOSS_BOSS_RANGED));
		LOOKS.put(EnemyKind.VORKATH, new NpcLook(NpcID.VORKATH));
		LOOKS.put(EnemyKind.TZTOK_JAD, new NpcLook(NpcID.TZHAAR_FIGHTCAVE_SWARM_BOSS));
		LOOKS.put(EnemyKind.CORPOREAL_BEAST, new NpcLook(NpcID.CORP_BEAST));
		LOOKS.put(EnemyKind.ALCHEMICAL_HYDRA, new NpcLook(NpcID.HYDRABOSS));
		LOOKS.put(EnemyKind.THE_NIGHTMARE, new NpcLook(NpcID.NIGHTMARE_PHASE_01));
		LOOKS.put(EnemyKind.PHANTOM_MUSPAH, new NpcLook(NpcID.MUSPAH));
		LOOKS.put(EnemyKind.DUKE_SUCELLUS, new NpcLook(NpcID.DUKE_SUCELLUS_AWAKE));
		LOOKS.put(EnemyKind.VARDORVIS, new NpcLook(NpcID.VARDORVIS));
		LOOKS.put(EnemyKind.THE_LEVIATHAN, new NpcLook(NpcID.LEVIATHAN));
		LOOKS.put(EnemyKind.THE_WHISPERER, new NpcLook(NpcID.WHISPERER));
		LOOKS.put(EnemyKind.ARAXXOR, new NpcLook(NpcID.ARAXXOR));
		LOOKS.put(EnemyKind.NEX, new NpcLook(NpcID.NEX));
		LOOKS.put(EnemyKind.VERZIK_VITUR, new NpcLook(NpcID.VERZIK_PHASE3));
		LOOKS.put(EnemyKind.YAMA, new NpcLook(NpcID.YAMA));
		LOOKS.put(EnemyKind.ICE_DEMON, new NpcLook(NpcID.RAIDS_ICEDEMON_COMBAT));
		LOOKS.put(EnemyKind.MUTTADILE, new NpcLook(NpcID.RAIDS_DOGODILE));
		LOOKS.put(EnemyKind.VANGUARD, new NpcLook(NpcID.RAIDS_VANGUARD_MELEE));
		LOOKS.put(EnemyKind.VESPULA, new NpcLook(NpcID.RAIDS_VESPULA_WALKING));
		LOOKS.put(EnemyKind.TEKTON, new NpcLook(NpcID.RAIDS_TEKTON_WALKING_STANDARD));
		LOOKS.put(EnemyKind.VASA_NISTIRIO, new NpcLook(NpcID.RAIDS_VASANISTIRIO_WALKING));
		LOOKS.put(EnemyKind.BA_BA, new NpcLook(NpcID.TOA_BABA));
		LOOKS.put(EnemyKind.AKKHA, new NpcLook(NpcID.AKKHA_MELEE));
		LOOKS.put(EnemyKind.KEPHRI, new NpcLook(NpcID.TOA_KEPHRI_BOSS_WEAK));
		LOOKS.put(EnemyKind.TUMEKENS_WARDEN, new NpcLook(NpcID.TOA_WARDEN_TUMEKEN_PHASE3));
		LOOKS.put(EnemyKind.DAGONHAI_ELDER, new NpcLook(NpcID.TFA_DAGONHAI_LEADER));
		LOOKS.put(EnemyKind.THE_SMORKS, new NpcLook(NpcID.TFA_SMORK));
		LOOKS.put(EnemyKind.BORK, new NpcLook(NpcID.TFA_BORK));
		LOOKS.put(EnemyKind.DREADNOTH, new NpcLook(NpcID.TFA_JELLY_BOSS));
		LOOKS.put(EnemyKind.XARTH, new NpcLook(NpcID.TFA_XARTH));
		LOOKS.put(EnemyKind.MOIA, new NpcLook(NpcID.TFA_MOIA));
		LOOKS.put(EnemyKind.EZEKIEL, new NpcLook(NpcID.TFA_EZEKIEL));
		LOOKS.put(EnemyKind.ARCHIVE_GUARDIAN, new NpcLook(NpcID.TFA_GUARDIAN));
		LOOKS.put(EnemyKind.OCELLUS, new NpcLook(NpcID.TFA_OCELLUS_P1));
	}

	/** Item shown on each location's card. */
	static final Map<Location, Integer> LOCATION_ICONS = new EnumMap<>(Location.class);

	static
	{
		LOCATION_ICONS.put(Location.LUMBRIDGE, ItemID.POH_TABLET_LUMBRIDGETELEPORT);
		LOCATION_ICONS.put(Location.DRAYNOR, ItemID.GARLIC);
		LOCATION_ICONS.put(Location.VARROCK, ItemID.POH_TABLET_VARROCKTELEPORT);
		LOCATION_ICONS.put(Location.FALADOR, ItemID.POH_TABLET_FALADORTELEPORT);
		LOCATION_ICONS.put(Location.AL_KHARID, ItemID.SHANTAY_PASS);
		LOCATION_ICONS.put(Location.ARDOUGNE, ItemID.POH_TABLET_ARDOUGNETELEPORT);
		LOCATION_ICONS.put(Location.KARAMJA, ItemID.BANANA);
		LOCATION_ICONS.put(Location.CANIFIS, ItemID.ECTOPHIAL);
		LOCATION_ICONS.put(Location.WILDERNESS, ItemID.SKULL);
	}

	/** The item whose icon is used for a sprite, or -1 when it is an interface sprite. */
	static int itemSprite(UiSprite which)
	{
		switch (which)
		{
			case CASKET:
				return ItemID.CASKET;
			case CANNON:
				return ItemID.TWPART1;
			case CANNONBALL:
				return ItemID.MCANNONBALL;
			case CHINCHOMPA:
				return ItemID.CHINCHOMPA_BIG_CAPTURED;
			default:
				return -1;
		}
	}

	static int spriteId(UiSprite which)
	{
		switch (which)
		{
			case ICE_SPELL:
				return SpriteID.Magicon2.ICE_BARRAGE;
			case ENEMY_SPELL:
				return SpriteID.Magicon.FIRE_STRIKE;
			case HITSPLAT_DAMAGE:
			case SPECIAL_HITSPLAT:
				return SpriteID.Hitmark._1;
			case HEALTH_FRONT:
				return SpriteID.StandardHealth30.FRONT;
			case HEALTH_BACK:
				return SpriteID.StandardHealth30.BACK;
			case ORB_FRAME:
				return SpriteID.OrbFrame.FRAME;
			case ORB_EMPTY:
				return SpriteID.OrbFiller.EMPTY;
			case ORB_HP_FILL:
				return SpriteID.OrbFiller.HITPOINTS;
			case ORB_HP_ICON:
				return SpriteID.OrbIcon.HITPOINTS;
			case SPELL_PROJECTILE:
				return SpriteID.Magicon.WIND_STRIKE;
			case XP_SKILL_ICON:
				return SpriteID.Staticons.MAGIC;
			case EQUIPMENT_SLOT:
				return SpriteID.Miscgraphics.EQUIPMENT_SLOT_TILE;
			case BORDER_TOP_LEFT:
				return SpriteID.Steelborder.TOP_LEFT;
			case BORDER_TOP_RIGHT:
				return SpriteID.Steelborder.TOP_RIGHT;
			case BORDER_BOTTOM_LEFT:
				return SpriteID.Steelborder.BOTTOM_LEFT;
			case BORDER_BOTTOM_RIGHT:
				return SpriteID.Steelborder.BOTTOM_RIGHT;
			case BUTTON_TOP_LEFT:
				return SpriteID.V2StoneButton.TOP_LEFT;
			case BUTTON_TOP_RIGHT:
				return SpriteID.V2StoneButton.TOP_RIGHT;
			case BUTTON_BOTTOM_LEFT:
				return SpriteID.V2StoneButton.BOTTOM_LEFT;
			case BUTTON_BOTTOM_RIGHT:
				return SpriteID.V2StoneButton.BOTTOM_RIGHT;
			case BUTTON_LEFT:
				return SpriteID.V2StoneButton.LEFT;
			case BUTTON_TOP:
				return SpriteID.V2StoneButton.TOP;
			case BUTTON_RIGHT:
				return SpriteID.V2StoneButton.RIGHT;
			case BUTTON_BOTTOM:
				return SpriteID.V2StoneButton.BOTTOM;
			default:
				throw new IllegalArgumentException(which.name());
		}
	}

	/** Default body parts by gender, used where the player's own are hidden by worn items. */
	static final int[][] DEFAULT_KITS = {
		// hair, jaw, torso, arms, hands, legs, boots
		{0, 10, 18, 26, 33, 36, 42},
		{45, -1, 56, 61, 67, 70, 79},
	};
	static final KitType[] BODY_KITS = {
		KitType.HAIR, KitType.JAW, KitType.TORSO, KitType.ARMS, KitType.HANDS, KitType.LEGS, KitType.BOOTS,
	};

	static KitType kitFor(Slot slot)
	{
		switch (slot)
		{
			case WEAPON:
				return KitType.WEAPON;
			case HEAD:
				return KitType.HEAD;
			case BODY:
				return KitType.TORSO;
			case LEGS:
				return KitType.LEGS;
			case SHIELD:
				return KitType.SHIELD;
			case CAPE:
				return KitType.CAPE;
			case NECK:
				return KitType.AMULET;
			case HANDS:
				return KitType.HANDS;
			case FEET:
				return KitType.BOOTS;
			default:
				// rings aren't visible
				return null;
		}
	}

	/**
	 * Mirrors (north-south) and turns a mesh. Quarter turns go west to north to east to south,
	 * like the game's orientations.
	 */
	static MeshSnapshot turn(MeshSnapshot m, boolean mirror, int quarters, double extra)
	{
		int n = m.vertexCount();
		float[] x = new float[n];
		float[] z = new float[n];
		double cos = Math.cos(extra);
		double sin = Math.sin(extra);
		for (int i = 0; i < n; i++)
		{
			float vx = m.vertexX(i);
			float vz = mirror ? -m.vertexZ(i) : m.vertexZ(i);
			float rx;
			float rz;
			switch (quarters)
			{
				case 1:
					rx = vz;
					rz = -vx;
					break;
				case 2:
					rx = -vx;
					rz = -vz;
					break;
				case 3:
					rx = -vz;
					rz = vx;
					break;
				default:
					rx = vx;
					rz = vz;
					break;
			}
			x[i] = (float) (rx * cos + rz * sin);
			z[i] = (float) (-rx * sin + rz * cos);
		}
		return m.withPositions(x, z);
	}

	static int shade(int rgb, double factor)
	{
		double k = Math.max(0.3, Math.min(1.3, factor));
		int r = (int) Math.min(255, (rgb >> 16 & 0xff) * k);
		int g = (int) Math.min(255, (rgb >> 8 & 0xff) * k);
		int b = (int) Math.min(255, (rgb & 0xff) * k);
		return r << 16 | g << 8 | b;
	}

	static int average(int a, int b, int c)
	{
		int r = ((a >> 16 & 0xff) + (b >> 16 & 0xff) + (c >> 16 & 0xff)) / 3;
		int g = ((a >> 8 & 0xff) + (b >> 8 & 0xff) + (c >> 8 & 0xff)) / 3;
		int bl = ((a & 0xff) + (b & 0xff) + (c & 0xff)) / 3;
		return r << 16 | g << 8 | bl;
	}
}
