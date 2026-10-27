package com.osrssurvivors;

import com.osrssurvivors.game.BossDrop;
import com.osrssurvivors.game.EnemyKind;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.Passive;
import com.osrssurvivors.game.Slot;
import com.osrssurvivors.game.Style;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.SpriteID;

/**
 * Which OSRS sound effect (cache index 4) the game plays for what: the keys
 * {@link com.osrssurvivors.audio.SoundCues} asks for, each tied to a sound id.
 *
 * <p>Where the sounds' meanings come from: Jagex's own config names for sound ids, which the OSRS
 * Wiki lists ("List of sound IDs", names sent by mistake in February 2025); RuneLite's
 * {@code SoundEffectID}; and the cache itself, whose animations carry their sounds (the
 * {@code HUMAN_*_WITHSOUND} copies of the player's attacks name 2500 for a sword slash, 2567 for a
 * crush, 2562 for a spear, 2549 for a stab; monsters' copies of player animations name 2693 for a
 * bow, 213 for Water Wave, 512 for a human dying). Each name is given beside its id. Every sound
 * was decoded and its spectrogram checked against what its name says (see README, Audio).
 */
final class SoundTable
{
	private SoundTable()
	{
	}

	/** A sound id with the name Jagex gave it. */
	static final class Sfx
	{
		final int id;
		final String name;

		Sfx(int id, String name)
		{
			this.id = id;
			this.name = name;
		}
	}

	private static Sfx s(int id, String name)
	{
		return new Sfx(id, name);
	}

	// ---------------------------------------------------------------- the sounds used

	static final Sfx HACKSWORD_SLASH = s(2500, "hacksword_slash");
	static final Sfx STABSWORD_STAB = s(2549, "stabsword_stab");
	static final Sfx STABSWORD_SLASH = s(2548, "stabsword_slash");
	static final Sfx WARHAMMER_CRUSH = s(2567, "warhammer_crush");
	static final Sfx STAFF_STAB = s(2562, "staff_stab");
	static final Sfx WHIP = s(2720, "whip");
	static final Sfx SCYTHE_SLASH = s(2524, "scythe_slash");
	static final Sfx GRANITE_MAUL = s(2714, "granite_maul");
	static final Sfx DHAROK_AXE_SLASH = s(1321, "dharok_axe_slash");
	static final Sfx TORAG_CRUSH = s(1332, "torag_crush");
	static final Sfx VERAC_CRUSH = s(1335, "verac_crush");
	static final Sfx GODSWORD_SLASH = s(3847, "godwars_godsword_slash");
	static final Sfx DRAGONCLAWS = s(4139, "dragonclaws_normal");
	static final Sfx UNARMED_PUNCH = s(2566, "unarmed_punch");
	static final Sfx ARROW_LAUNCH = s(2693, "arrowlaunch2");
	static final Sfx CROSSBOW = s(2695, "crossbow");
	static final Sfx DART = s(2696, "dart");
	static final Sfx CHINCHOMPA_ATTACK = s(359, "chinchompa_attack");
	static final Sfx CHINCHOMPA_EXPLODE = s(360, "chinchompa_explode");
	static final Sfx SHADOW_ORB = s(6412, "toa_shadow_weapon_orb_02");

	static final Sfx ICE_CAST = s(171, "ice_cast");
	static final Sfx ICE_BARRAGE_IMPACT = s(168, "ice_barrage_impact");
	static final Sfx BLOOD_CAST = s(106, "blood_cast");
	static final Sfx BLOOD_BARRAGE_IMPACT = s(102, "blood_barrage_impact");
	static final Sfx CANNON_FIRE = s(1667, "mcannon_fire");
	static final Sfx SKELETON_ATTACK = s(776, "skeleton_attack");
	static final Sfx SARADOMIN_STRIKE = s(1659, "saradomin_strike");
	static final Sfx DRAGONBREATH = s(585, "dragonbreath");
	static final Sfx WINDSTRIKE_CAST = s(220, "windstrike_cast_and_fire");

	static final Sfx HUMAN_HIT = s(513, "human_hit");
	static final Sfx HUMAN_DEATH = s(512, "human_death");
	static final Sfx REDEMPTION_HEAL = s(2681, "redemption_heal");
	static final Sfx RETRIBUTION = s(2682, "retribution");

	static final Sfx FOUND_GEM = s(2655, "found_gem");
	static final Sfx PICK = s(2582, "pick2");
	static final Sfx PUT_DOWN = s(2739, "put_down");
	static final Sfx CASKET_OPEN = s(50, "casket_open");
	static final Sfx HORN = s(3302, "barbassault_horn");
	static final Sfx GONG = s(3879, "godwars_gong");
	static final Sfx RELIC_HIGH = s(4214, "relic_unlock_high_sound_1");
	static final Sfx RELIC_ALL = s(4217, "relic_unlock_all_1");
	static final Sfx TWUNKLES = s(2331, "trailblazer_unlock_twunkles");
	static final Sfx COINS = s(2115, "coins");
	static final Sfx GE_COLLECT_COINS = s(3924, "ge_collect_coins");
	static final Sfx GE_TRADE_OK = s(3925, "ge_trade_ok");
	static final Sfx DICE_ROLL = s(1474, "dice_roll");
	static final Sfx LEVER = s(2400, "lever");
	static final Sfx UNLOCK = s(2419, "unlock");
	static final Sfx EAT = s(2393, "eat");
	static final Sfx DRINK = s(2390, "drink");
	static final Sfx INTERFACE_SELECT = s(2266, "interface_select1");
	static final Sfx INTERFACE_WRONG = s(2268, "interface_wrong");

	// ---------------------------------------------------------------- weapons, by how they attack

	/** A weapon's swing or shot, from the attack animation the game already plays for it. */
	private static final Map<Integer, Sfx> BY_ANIMATION = new LinkedHashMap<>();
	/** A spell's cast and its landing, from the spell icon the weapon fires. */
	private static final Map<Integer, Sfx[]> BY_SPELL = new LinkedHashMap<>();

	static
	{
		BY_ANIMATION.put(AnimationID.HUMAN_SWORD_SLASH, HACKSWORD_SLASH);
		BY_ANIMATION.put(AnimationID.HUMAN_SWORD_STAB, STABSWORD_STAB);
		BY_ANIMATION.put(AnimationID.GHRAZI_RAPIER_ATTACK, STABSWORD_STAB);
		BY_ANIMATION.put(AnimationID.HUMAN_AXE_HACK, STABSWORD_SLASH);
		BY_ANIMATION.put(AnimationID.HUMAN_BLUNT_POUND, WARHAMMER_CRUSH);
		BY_ANIMATION.put(AnimationID.BARROW_GUTHAN_CRUSH, STAFF_STAB);
		BY_ANIMATION.put(AnimationID.SLAYER_ABYSSAL_WHIP_ATTACK, WHIP);
		BY_ANIMATION.put(AnimationID.SCYTHE_OF_VITUR_ATTACK, SCYTHE_SLASH);
		BY_ANIMATION.put(AnimationID.SLAYER_GRANITE_MAUL_ATTACK, GRANITE_MAUL);
		BY_ANIMATION.put(AnimationID.BARROW_DHAROK_SLASH, DHAROK_AXE_SLASH);
		BY_ANIMATION.put(AnimationID.BARROW_TORAG_CRUSH, TORAG_CRUSH);
		BY_ANIMATION.put(AnimationID.DCLAWS_HUMAN_ATTACK, DRAGONCLAWS);
		BY_ANIMATION.put(AnimationID.HUMAN_BOW, ARROW_LAUNCH);
		BY_ANIMATION.put(AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD, CROSSBOW);
		BY_ANIMATION.put(AnimationID.BARROWS_REPEATING_CROSSBOW_FIRE, CROSSBOW);
		BY_ANIMATION.put(AnimationID.TFA_HUMAN_ASCENSION_CROSSBOW_FIRE_RIGHT, CROSSBOW);
		BY_ANIMATION.put(AnimationID.SNAKEBOSS_BLOWPIPE_ATTACK, DART);
		BY_ANIMATION.put(AnimationID.HUMAN_CHINCHOMPA_ATTACK, CHINCHOMPA_ATTACK);
		BY_ANIMATION.put(AnimationID.TFA_HUMAN_OBLIGATOR_ATTACK, WARHAMMER_CRUSH);
		BY_ANIMATION.put(AnimationID.TOA_SOT_CAST_B, SHADOW_ORB);

		spell(SpriteID.Magicon.WIND_STRIKE, 220, "windstrike_cast_and_fire", 221, "windstrike_hit");
		spell(SpriteID.Magicon.WIND_BLAST, 216, "windblast_cast_and_fire", 217, "windblast_hit");
		spell(SpriteID.Magicon.WIND_WAVE, 222, "windwave_cast_and_fire", 223, "windwave_hit");
		spell(SpriteID.Magicon.WATER_BLAST, 207, "waterblast_cast_and_fire", 208, "waterblast_hit");
		spell(SpriteID.Magicon.WATER_WAVE, 213, "waterwave_cast_and_fire", 214, "waterwave_hit");
		spell(SpriteID.Magicon.EARTH_BLAST, 128, "earthblast_cast_and_fire", 129, "earthblast_hit");
		spell(SpriteID.Magicon.EARTH_WAVE, 134, "earthwave_cast_and_fire", 135, "earthwave_hit");
		spell(SpriteID.Magicon.FIRE_BOLT, 157, "firebolt_cast_and_fire", 158, "firebolt_hit");
		spell(SpriteID.Magicon.FIRE_BLAST, 155, "fireblast_cast_and_fire", 156, "fireblast_hit");
		spell(SpriteID.Magicon.FIRE_WAVE, 162, "firewave_cast_and_fire", 163, "firewave_hit");
		spell(SpriteID.Magicon2.ICE_BARRAGE, 171, "ice_cast", 168, "ice_barrage_impact");
	}

	private static void spell(int sprite, int cast, String castName, int hit, String hitName)
	{
		BY_SPELL.put(sprite, new Sfx[]{s(cast, castName), s(hit, hitName)});
	}

	/** The attack sound for a weapon: its spell's cast, else its animation's sound. */
	static Sfx attack(Gear g)
	{
		Sfx[] spell = BY_SPELL.get(GearArt.spellSprite(g));
		int animation = GearArt.attackAnimation(g);
		if (g.getMissile() == Gear.Missile.SPELL && animation != AnimationID.TOA_SOT_CAST_B)
		{
			return spell != null ? spell[0] : WINDSTRIKE_CAST;
		}
		Sfx s = BY_ANIMATION.get(animation);
		if (s != null)
		{
			return s;
		}
		switch (g.getMissile())
		{
			case ARROW:
				return ARROW_LAUNCH;
			case BOLT:
				return CROSSBOW;
			case DART:
				return DART;
			case CHINCHOMPA:
				return CHINCHOMPA_ATTACK;
			default:
				return HACKSWORD_SLASH;
		}
	}

	/** A spell weapon's landing sound, or null for weapons that have none. */
	static Sfx impact(Gear g)
	{
		if (g.getMissile() != Gear.Missile.SPELL)
		{
			return null;
		}
		Sfx[] spell = BY_SPELL.get(GearArt.spellSprite(g));
		return spell != null ? spell[1] : s(221, "windstrike_hit");
	}

	/** The game's sound for putting on a piece of gear, by what kind of thing it is. */
	static Sfx equip(Gear g)
	{
		String n = g.name();
		Slot slot = g.getSlot();
		switch (slot)
		{
			case WEAPON:
				if (g.getStyle() == Style.RANGED)
				{
					return s(2244, "equip_ranged");
				}
				if (g.getStyle() == Style.MAGIC)
				{
					return n.contains("STAFF") ? s(2230, "equip_elemental_staff") : s(226, "equip_magic");
				}
				if (n.contains("WHIP") || n.contains("TENTACLE"))
				{
					return s(2249, "equip_whip");
				}
				if (n.contains("MAUL") || n.contains("HAMMER") || n.contains("MACE") || n.contains("FLAIL") || n.contains("KET_OM")
					|| n.contains("OBLIGATOR"))
				{
					return s(2233, "equip_blunt");
				}
				if (n.contains("AXE"))
				{
					return s(2232, "equip_baxe");
				}
				return s(2248, "equip_sword");
			case HEAD:
				return s(2240, "equip_helmet");
			case BODY:
				return metal(n) ? s(2239, "equip_metal_body") : g.getStyle() == Style.RANGED ? s(2241, "equip_leather") : s(2234, "equip_body");
			case LEGS:
				return metal(n) ? s(2243, "equip_metal_legs") : s(2242, "equip_legs");
			case SHIELD:
				return g.getStyle() == Style.MAGIC ? s(226, "equip_magic") : s(2245, "equip_shield");
			case HANDS:
				return s(2236, "equip_hands");
			case FEET:
				return s(2237, "equip_feet");
			default:
				return s(2238, "equip_fun");
		}
	}

	private static boolean metal(String n)
	{
		return n.contains("PLATE") || n.contains("CHAIN") || n.contains("BANDOS") || n.contains("TORVA") || n.contains("TORAG")
			|| n.contains("DHAROK") || n.contains("VERAC") || n.contains("GUTHAN") || n.contains("INQUISITOR")
			|| n.contains("JUSTICIAR") || n.contains("OBSIDIAN") || n.contains("FIGHTER") || n.contains("CHESTPLATE")
			|| n.contains("TASSETS");
	}

	// ---------------------------------------------------------------- monsters

	/** A monster's grunt when hit and its death, by kind; kinds missing here sound like people. */
	static final Map<EnemyKind, Sfx[]> MONSTERS = new EnumMap<>(EnemyKind.class);

	private static void monster(EnemyKind kind, int hit, String hitName, int death, String deathName)
	{
		MONSTERS.put(kind, new Sfx[]{s(hit, hitName), s(death, deathName)});
	}

	private static void monsters(int hit, String hitName, int death, String deathName, EnemyKind... kinds)
	{
		for (EnemyKind k : kinds)
		{
			monster(k, hit, hitName, death, deathName);
		}
	}

	static
	{
		monster(EnemyKind.CHICKEN, 357, "chicken_hit", 356, "chicken_death");
		monster(EnemyKind.RAT, 713, "rat_hit", 711, "rat_death");
		monsters(472, "goblin_hit", 471, "goblin_death", EnemyKind.GOBLIN, EnemyKind.THE_SMORKS);
		monsters(634, "monkey_hit", 632, "monkey_death", EnemyKind.MONKEY, EnemyKind.BA_BA);
		monster(EnemyKind.CRAWLING_HAND, 379, "hand_hit", 378, "hand_death");
		monster(EnemyKind.COW, 371, "cow_hit", 370, "cow_death");
		monsters(3607, "big_spider_hit", 3606, "big_spider_death", EnemyKind.GIANT_SPIDER, EnemyKind.JUNGLE_SPIDER,
			EnemyKind.SARACHNIS, EnemyKind.ARAXXOR);
		monster(EnemyKind.ZOMBIE, 923, "zombie_hit", 922, "zombie_death");
		monsters(779, "skelly_hit", 777, "skeleton_death", EnemyKind.SKELETON, EnemyKind.SKELETON_MAGE);
		monster(EnemyKind.DWARF, 419, "dwarf_hit", 418, "dwarf_death");
		monster(EnemyKind.BARBARIAN, 504, "barbarian_hit", 502, "barbarian_death");
		monsters(3612, "scorpion_hit", 3610, "scorpion_death", EnemyKind.SCORPION, EnemyKind.SCORPIA, EnemyKind.KALPHITE_WORKER,
			EnemyKind.KALPHITE_SOLDIER);
		monster(EnemyKind.KALPHITE_QUEEN, 554, "kalphite_flyingqueen_hit", 564, "kalthite_queen_death");
		monster(EnemyKind.WOLF, 912, "wolf_hit", 911, "wolf_death");
		monster(EnemyKind.GHOUL, 444, "ghoul_hit", 443, "ghoul_death");
		monster(EnemyKind.REVENANT_IMP, 536, "imp_hit", 535, "imp_death");
		monsters(439, "ghost_hit", 438, "ghost_death", EnemyKind.REVENANT_KNIGHT, EnemyKind.THE_WHISPERER);
		monster(EnemyKind.ICE_WARRIOR, 530, "ice_warrior_hit", 529, "ice_warrior_death");
		monster(EnemyKind.BANSHEE, 286, "banshee_hit", 285, "banshee_death");
		monsters(388, "crocodile_hit", 387, "crocodile_death", EnemyKind.CROCODILE, EnemyKind.MUTTADILE);
		monsters(451, "giant_hit", 450, "giant_death", EnemyKind.HILL_GIANT, EnemyKind.MOSS_GIANT, EnemyKind.ICE_GIANT,
			EnemyKind.FIRE_GIANT, EnemyKind.TEKTON, EnemyKind.DUKE_SUCELLUS, EnemyKind.YAMA);
		monster(EnemyKind.JOGRE, 917, "zogre_hit", 916, "zogre_death");
		monster(EnemyKind.WEREWOLF, 491, "half_werewolf_hit", 490, "half_werewolf_death");
		monsters(884, "vampire_hit", 882, "vampire_death", EnemyKind.FERAL_VAMPYRE, EnemyKind.VERZIK_VITUR, EnemyKind.VARDORVIS);
		monster(EnemyKind.BLOODVELD, 314, "bloodveld_hit", 313, "bloodveld_death");
		monsters(404, "demon_hit", 403, "demon_death", EnemyKind.LESSER_DEMON, EnemyKind.GREATER_DEMON, EnemyKind.BLACK_DEMON,
			EnemyKind.ICE_DEMON);
		monster(EnemyKind.ABYSSAL_DEMON, 278, "abyssal_hit", 277, "abyssal_death");
		monster(EnemyKind.KRIL_TSUTSAROTH, 402, "demon_champion_hit", 3864, "godwars_zamorak_avatar_death");
		monsters(410, "dragon_hit", 409, "dragon_death", EnemyKind.GREEN_DRAGON, EnemyKind.KING_BLACK_DRAGON);
		monster(EnemyKind.VORKATH, 410, "dragon_hit", 1523, "vorkath_death");
		monster(EnemyKind.CHAOS_ELEMENTAL, 351, "chaos_elemental_hit", 347, "chaos_elemental_death");
		monster(EnemyKind.GIANT_MOLE, 1646, "mole_hit", 1645, "mole_death");
		monsters(1622, "dagganoth_hit", 1621, "dagganoth_death", EnemyKind.DAGANNOTH_REX, EnemyKind.DREADNOTH);
		monster(EnemyKind.GENERAL_GRAARDOR, 3867, "godwars_bandos_avatar_hit", 3838, "godwars_bandos_avatar_death");
		monster(EnemyKind.COMMANDER_ZILYANA, 3858, "godwars_saradomin_avatar_hit", 3860, "godwars_saradomin_avatar_death");
		monster(EnemyKind.KREEARRA, 3845, "godwars_armadyl_hit", 3839, "godwars_armadyl_avatar_death");
		monster(EnemyKind.CERBERUS, 1185, "skeletal_hellhound_hit", 1188, "skeletal_hellhound_death");
		monsters(800, "snake_hit", 798, "snake_death", EnemyKind.ZULRAH, EnemyKind.THE_LEVIATHAN);
		monster(EnemyKind.TZTOK_JAD, 257, "tzhaar_ket_hit", 256, "tzhaar_ket_death");
		monsters(391, "dark_beast_hit", 390, "dark_beast_death", EnemyKind.CORPOREAL_BEAST, EnemyKind.VANGUARD, EnemyKind.VASA_NISTIRIO,
			EnemyKind.THE_NIGHTMARE, EnemyKind.PHANTOM_MUSPAH, EnemyKind.TUMEKENS_WARDEN, EnemyKind.AKKHA, EnemyKind.ARCHIVE_GUARDIAN,
			EnemyKind.OCELLUS, EnemyKind.XARTH);
		monster(EnemyKind.ALCHEMICAL_HYDRA, 4068, "hydra_boss_stage_1_hit", 4075, "hydra_boss_stage_1_death");
		monsters(3612, "scorpion_hit", 6461, "toa_kephri_death_03", EnemyKind.KEPHRI, EnemyKind.VESPULA);
		monster(EnemyKind.NEX, 513, "human_hit", 5180, "nex2021_death_01");
		monster(EnemyKind.BORK, 650, "orc_hit", 649, "orc_death");
		monster(EnemyKind.MOIA, 506, "female_hit", 505, "female_death");
	}

	/** Every key the game asks for, with its sound. */
	static Map<String, Sfx> build()
	{
		Map<String, Sfx> t = new LinkedHashMap<>();
		t.put("attack.FISTS", UNARMED_PUNCH);
		for (Gear g : Gear.values())
		{
			if (g.getSlot() == Slot.WEAPON)
			{
				t.put("attack." + g.name(), attack(g));
				Sfx impact = impact(g);
				if (impact != null)
				{
					t.put("impact." + g.name(), impact);
				}
			}
			t.put("equip." + g.name(), equip(g));
		}
		for (EnemyKind k : EnemyKind.values())
		{
			Sfx[] m = MONSTERS.getOrDefault(k, new Sfx[]{HUMAN_HIT, HUMAN_DEATH});
			t.put("hit." + k.name(), m[0]);
			t.put("death." + k.name(), m[1]);
		}
		t.put("shot." + EnemyKind.Attack.ARROW.name(), ARROW_LAUNCH);
		t.put("shot." + EnemyKind.Attack.MAGIC.name(), WINDSTRIKE_CAST);

		t.put("ability." + Passive.CANNON.name(), CANNON_FIRE);
		t.put("ability." + Passive.THRALLS.name(), SKELETON_ATTACK);
		t.put("ability." + Passive.CHINCHOMPAS.name(), CHINCHOMPA_ATTACK);
		t.put("land." + Passive.CHINCHOMPAS.name(), CHINCHOMPA_EXPLODE);
		t.put("land." + Passive.ICE_BARRAGE.name(), ICE_BARRAGE_IMPACT);
		t.put("land." + Passive.BLOOD_BARRAGE.name(), BLOOD_BARRAGE_IMPACT);
		t.put("land." + Passive.GOD_SPELLS.name(), SARADOMIN_STRIKE);
		t.put("land." + Passive.DRAGONFIRE.name(), DRAGONBREATH);

		// prayers as they're chosen, from RuneLite's SoundEffectID
		t.put("prayer." + Passive.MELEE_PRAYER.name(), s(2691, "ultimate_strength"));
		t.put("prayer." + Passive.RANGED_PRAYER.name(), s(2665, "eagle_eye"));
		t.put("prayer." + Passive.MAGIC_PRAYER.name(), s(2669, "mystic_might"));
		t.put("prayer." + Passive.PROTECTION.name(), s(2687, "steel_skin"));
		t.put("prayer." + Passive.PROTECT_MELEE.name(), s(2676, "protect_from_melee"));
		t.put("prayer." + Passive.PROTECT_MISSILES.name(), s(2677, "protect_from_missiles"));
		t.put("prayer." + Passive.RAPID_HEAL.name(), s(2678, "rapid_heal"));
		t.put("prayer." + Passive.REDEMPTION.name(), s(2680, "redemption"));
		t.put("prayer." + Passive.RETRIBUTION.name(), RETRIBUTION);
		t.put("proc." + Passive.REDEMPTION.name(), REDEMPTION_HEAL);
		t.put("proc." + Passive.RETRIBUTION.name(), RETRIBUTION);

		t.put("event.PLAYER_HURT", HUMAN_HIT);
		t.put("event.PLAYER_DEATH", HUMAN_DEATH);
		t.put("event.REVIVE", REDEMPTION_HEAL);
		t.put("event.XP", PICK);
		t.put("event.CASKET", CASKET_OPEN);
		t.put("event.LEVEL_UP", FOUND_GEM);
		t.put("event.CHOOSE", INTERFACE_SELECT);
		t.put("event.REROLL", DICE_ROLL);
		t.put("event.CHAMPION_WARNING", HORN);
		t.put("event.CHAMPION_ARRIVES", GONG);
		t.put("event.CHAMPION_DEATH", RELIC_HIGH);
		t.put("event.REELS_START", LEVER);
		t.put("event.REEL_STOP", UNLOCK);
		t.put("event.WIN." + BossDrop.Rarity.COMMON.name(), COINS);
		t.put("event.WIN." + BossDrop.Rarity.UNCOMMON.name(), RELIC_HIGH);
		t.put("event.WIN." + BossDrop.Rarity.RARE.name(), TWUNKLES);
		t.put("event.WIN." + BossDrop.Rarity.JACKPOT.name(), RELIC_ALL);
		t.put("event.WIN." + BossDrop.Rarity.MEGARARE.name(), RELIC_ALL);
		t.put("event.CLAIM", GE_COLLECT_COINS);
		t.put("event.COMBINATION", TWUNKLES);
		t.put("event.EQUIP", s(2238, "equip_fun"));
		t.put("event.EAT", EAT);
		t.put("event.DRINK", DRINK);
		t.put("event.DROP", PUT_DOWN);

		t.put("ui.click", INTERFACE_SELECT);
		t.put("ui.error", INTERFACE_WRONG);
		t.put("ui.buy", GE_TRADE_OK);
		t.put("ui.unlock", UNLOCK);
		return t;
	}
}
