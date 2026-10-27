package com.osrssurvivors;

import com.osrssurvivors.game.Boost;
import com.osrssurvivors.game.BossDrop;
import com.osrssurvivors.game.Food;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.Passive;
import com.osrssurvivors.game.Perk;
import com.osrssurvivors.game.Pet;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;

/**
 * Ties the game's gear, passives, upgrades, pets and boss drops to the client's own content: item
 * ids (worn models and icons), attack animations, spell icons and prayer/skill sprites.
 */
final class GearArt
{
	static final Map<Gear, Integer> ITEMS = new EnumMap<>(Gear.class);
	private static final Map<Gear, Integer> ATTACKS = new EnumMap<>(Gear.class);
	private static final Map<Gear, Integer> SPELLS = new EnumMap<>(Gear.class);

	static final int PUNCH = AnimationID.HUMAN_UNARMEDPUNCH;
	static final int READY = AnimationID.HUMAN_READY;
	static final int WALK = AnimationID.HUMAN_WALK_F;

	private GearArt()
	{
	}

	private static void item(Gear gear, int itemId)
	{
		ITEMS.put(gear, itemId);
	}

	private static void weapon(Gear gear, int itemId, int attack)
	{
		ITEMS.put(gear, itemId);
		ATTACKS.put(gear, attack);
	}

	private static void staff(Gear gear, int itemId, int attack, int spellSprite)
	{
		weapon(gear, itemId, attack);
		SPELLS.put(gear, spellSprite);
	}

	static
	{
		weapon(Gear.BRONZE_SCIMITAR, ItemID.BRONZE_SCIMITAR, AnimationID.HUMAN_SWORD_SLASH);
		weapon(Gear.IRON_SCIMITAR, ItemID.IRON_SCIMITAR, AnimationID.HUMAN_SWORD_SLASH);
		weapon(Gear.STEEL_SCIMITAR, ItemID.STEEL_SCIMITAR, AnimationID.HUMAN_SWORD_SLASH);
		weapon(Gear.MITHRIL_SCIMITAR, ItemID.MITHRIL_SCIMITAR, AnimationID.HUMAN_SWORD_SLASH);
		weapon(Gear.ADAMANT_SCIMITAR, ItemID.ADAMANT_SCIMITAR, AnimationID.HUMAN_SWORD_SLASH);
		weapon(Gear.RUNE_SCIMITAR, ItemID.RUNE_SCIMITAR, AnimationID.HUMAN_SWORD_SLASH);
		weapon(Gear.DRAGON_SCIMITAR, ItemID.DRAGON_SCIMITAR, AnimationID.HUMAN_SWORD_SLASH);
		weapon(Gear.ABYSSAL_WHIP, ItemID.ABYSSAL_WHIP, AnimationID.SLAYER_ABYSSAL_WHIP_ATTACK);
		weapon(Gear.GHRAZI_RAPIER, ItemID.GHRAZI_RAPIER, AnimationID.GHRAZI_RAPIER_ATTACK);
		weapon(Gear.SCYTHE_OF_VITUR, ItemID.SCYTHE_OF_VITUR, AnimationID.SCYTHE_OF_VITUR_ATTACK);

		weapon(Gear.SHORTBOW, ItemID.SHORTBOW, AnimationID.HUMAN_BOW);
		weapon(Gear.OAK_SHORTBOW, ItemID.OAK_SHORTBOW, AnimationID.HUMAN_BOW);
		weapon(Gear.WILLOW_SHORTBOW, ItemID.WILLOW_SHORTBOW, AnimationID.HUMAN_BOW);
		weapon(Gear.MAPLE_SHORTBOW, ItemID.MAPLE_SHORTBOW, AnimationID.HUMAN_BOW);
		weapon(Gear.YEW_SHORTBOW, ItemID.YEW_SHORTBOW, AnimationID.HUMAN_BOW);
		weapon(Gear.MAGIC_SHORTBOW, ItemID.MAGIC_SHORTBOW, AnimationID.HUMAN_BOW);
		weapon(Gear.RUNE_CROSSBOW, ItemID.XBOWS_CROSSBOW_RUNITE, AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD);
		weapon(Gear.ARMADYL_CROSSBOW, ItemID.ACB, AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD);
		weapon(Gear.TOXIC_BLOWPIPE, ItemID.TOXIC_BLOWPIPE, AnimationID.SNAKEBOSS_BLOWPIPE_ATTACK);
		weapon(Gear.TWISTED_BOW, ItemID.TWISTED_BOW, AnimationID.HUMAN_BOW);

		staff(Gear.STAFF_OF_AIR, ItemID.STAFF_OF_AIR, AnimationID.HUMAN_CASTSTRIKE_STAFF, SpriteID.Magicon.WIND_STRIKE);
		staff(Gear.STAFF_OF_FIRE, ItemID.STAFF_OF_FIRE, AnimationID.HUMAN_CASTSTRIKE_STAFF, SpriteID.Magicon.FIRE_BOLT);
		staff(Gear.MYSTIC_FIRE_STAFF, ItemID.MYSTIC_FIRE_STAFF, AnimationID.HUMAN_CASTSTRIKE_STAFF, SpriteID.Magicon.FIRE_BLAST);
		staff(Gear.ANCIENT_STAFF, ItemID.STAFF_OF_ZAROS, AnimationID.ZAROS_VERTICAL_CASTING, SpriteID.Magicon.WATER_WAVE);
		staff(Gear.TRIDENT_OF_THE_SEAS, ItemID.TOTS_CHARGED, AnimationID.HUMAN_CASTWAVE_STAFF, SpriteID.Magicon.WATER_BLAST);
		staff(Gear.TRIDENT_OF_THE_SWAMP, ItemID.TOXIC_TOTS_CHARGED, AnimationID.HUMAN_CASTWAVE_STAFF, SpriteID.Magicon.EARTH_WAVE);
		staff(Gear.SANGUINESTI_STAFF, ItemID.SANGUINESTI_STAFF, AnimationID.HUMAN_CASTWAVE_STAFF, SpriteID.Magicon.FIRE_WAVE);
		staff(Gear.TUMEKENS_SHADOW, ItemID.TUMEKENS_SHADOW, AnimationID.TOA_SOT_CAST_B, SpriteID.Magicon.WIND_WAVE);

		item(Gear.BRONZE_FULL_HELM, ItemID.BRONZE_FULL_HELM);
		item(Gear.IRON_FULL_HELM, ItemID.IRON_FULL_HELM);
		item(Gear.STEEL_FULL_HELM, ItemID.STEEL_FULL_HELM);
		item(Gear.MITHRIL_FULL_HELM, ItemID.MITHRIL_FULL_HELM);
		item(Gear.ADAMANT_FULL_HELM, ItemID.ADAMANT_FULL_HELM);
		item(Gear.RUNE_FULL_HELM, ItemID.RUNE_FULL_HELM);
		item(Gear.DRAGON_FULL_HELM, ItemID.BRUT_DRAGON_FULL_HELM);
		item(Gear.NEITIZNOT_FACEGUARD, ItemID.NEITIZNOT_FACEGUARD);
		item(Gear.TORVA_FULL_HELM, ItemID.TORVA_HELM);
		item(Gear.BRONZE_PLATEBODY, ItemID.BRONZE_PLATEBODY);
		item(Gear.IRON_PLATEBODY, ItemID.IRON_PLATEBODY);
		item(Gear.STEEL_PLATEBODY, ItemID.STEEL_PLATEBODY);
		item(Gear.MITHRIL_PLATEBODY, ItemID.MITHRIL_PLATEBODY);
		item(Gear.ADAMANT_PLATEBODY, ItemID.ADAMANT_PLATEBODY);
		item(Gear.RUNE_PLATEBODY, ItemID.RUNE_PLATEBODY);
		item(Gear.DRAGON_PLATEBODY, ItemID.DRAGON_PLATEBODY);
		item(Gear.BANDOS_CHESTPLATE, ItemID.BANDOS_CHESTPLATE);
		item(Gear.TORVA_PLATEBODY, ItemID.TORVA_CHEST);
		item(Gear.BRONZE_PLATELEGS, ItemID.BRONZE_PLATELEGS);
		item(Gear.IRON_PLATELEGS, ItemID.IRON_PLATELEGS);
		item(Gear.STEEL_PLATELEGS, ItemID.STEEL_PLATELEGS);
		item(Gear.MITHRIL_PLATELEGS, ItemID.MITHRIL_PLATELEGS);
		item(Gear.ADAMANT_PLATELEGS, ItemID.ADAMANT_PLATELEGS);
		item(Gear.RUNE_PLATELEGS, ItemID.RUNE_PLATELEGS);
		item(Gear.DRAGON_PLATELEGS, ItemID.DRAGON_PLATELEGS);
		item(Gear.BANDOS_TASSETS, ItemID.BANDOS_SKIRT);
		item(Gear.TORVA_PLATELEGS, ItemID.TORVA_LEGS);

		item(Gear.LEATHER_COWL, ItemID.LEATHER_COWL);
		item(Gear.COIF, ItemID.COIF);
		item(Gear.ROBIN_HOOD_HAT, ItemID.ROBINHOODHAT);
		item(Gear.ARMADYL_HELMET, ItemID.ARMADYL_HELMET);
		item(Gear.MASORI_MASK, ItemID.MASORI_MASK);
		item(Gear.LEATHER_BODY, ItemID.LEATHER_ARMOUR);
		item(Gear.STUDDED_BODY, ItemID.STUDDED_BODY);
		item(Gear.GREEN_DHIDE_BODY, ItemID.DRAGONHIDE_BODY);
		item(Gear.BLUE_DHIDE_BODY, ItemID.BLUE_DRAGONHIDE_BODY);
		item(Gear.RED_DHIDE_BODY, ItemID.RED_DRAGONHIDE_BODY);
		item(Gear.BLACK_DHIDE_BODY, ItemID.BLACK_DRAGONHIDE_BODY);
		item(Gear.ARMADYL_CHESTPLATE, ItemID.ARMADYL_CHESTPLATE);
		item(Gear.MASORI_BODY, ItemID.MASORI_BODY);
		item(Gear.LEATHER_CHAPS, ItemID.LEATHER_CHAPS);
		item(Gear.STUDDED_CHAPS, ItemID.STUDDED_CHAPS);
		item(Gear.GREEN_DHIDE_CHAPS, ItemID.DRAGONHIDE_CHAPS);
		item(Gear.BLUE_DHIDE_CHAPS, ItemID.BLUE_DRAGONHIDE_CHAPS);
		item(Gear.RED_DHIDE_CHAPS, ItemID.RED_DRAGONHIDE_CHAPS);
		item(Gear.BLACK_DHIDE_CHAPS, ItemID.BLACK_DRAGONHIDE_CHAPS);
		item(Gear.ARMADYL_CHAINSKIRT, ItemID.ARMADYL_SKIRT);
		item(Gear.MASORI_CHAPS, ItemID.MASORI_CHAPS);

		item(Gear.WIZARD_HAT, ItemID.BLUEWIZHAT);
		item(Gear.MYSTIC_HAT, ItemID.MYSTIC_HAT);
		item(Gear.INFINITY_HAT, ItemID.MAGICTRAINING_INFINITYHAT);
		item(Gear.AHRIMS_HOOD, ItemID.BARROWS_AHRIM_HEAD);
		item(Gear.ANCESTRAL_HAT, ItemID.ANCESTRAL_HAT);
		item(Gear.WIZARD_ROBE, ItemID.WIZARDS_ROBE);
		item(Gear.MYSTIC_ROBE_TOP, ItemID.MYSTIC_ROBE_TOP);
		item(Gear.INFINITY_TOP, ItemID.MAGICTRAINING_INFINITYTOP);
		item(Gear.AHRIMS_ROBETOP, ItemID.BARROWS_AHRIM_BODY);
		item(Gear.ANCESTRAL_ROBE_TOP, ItemID.ANCESTRAL_ROBE_TOP);
		item(Gear.WIZARD_SKIRT, ItemID.BLUE_SKIRT);
		item(Gear.MYSTIC_ROBE_BOTTOM, ItemID.MYSTIC_ROBE_BOTTOM);
		item(Gear.INFINITY_BOTTOMS, ItemID.MAGICTRAINING_INFINITYBOTTOM);
		item(Gear.AHRIMS_ROBESKIRT, ItemID.BARROWS_AHRIM_LEGS);
		item(Gear.ANCESTRAL_ROBE_BOTTOM, ItemID.ANCESTRAL_ROBE_BOTTOM);

		item(Gear.BRONZE_DEFENDER, ItemID.BRONZE_PARRYINGDAGGER);
		item(Gear.IRON_DEFENDER, ItemID.IRON_PARRYINGDAGGER);
		item(Gear.STEEL_DEFENDER, ItemID.STEEL_PARRYINGDAGGER);
		item(Gear.MITHRIL_DEFENDER, ItemID.MITHRIL_PARRYINGDAGGER);
		item(Gear.ADAMANT_DEFENDER, ItemID.ADAMANT_PARRYINGDAGGER);
		item(Gear.RUNE_DEFENDER, ItemID.RUNE_PARRYINGDAGGER);
		item(Gear.DRAGON_DEFENDER, ItemID.DRAGON_PARRYINGDAGGER);
		item(Gear.AVERNIC_DEFENDER, ItemID.INFERNAL_DEFENDER);
		item(Gear.MAGES_BOOK, ItemID.MAGICTRAINING_BOOKOFMAGIC);
		item(Gear.TOME_OF_FIRE, ItemID.TOME_OF_FIRE);
		item(Gear.ARCANE_SPIRIT_SHIELD, ItemID.ARCANE);
		item(Gear.ELIDINIS_WARD, ItemID.ELIDINIS_WARD);

		item(Gear.RED_CAPE, ItemID.RED_CAPE);
		item(Gear.OBSIDIAN_CAPE, ItemID.TZHAAR_CAPE_OBSIDIAN);
		item(Gear.FIRE_CAPE, ItemID.TZHAAR_CAPE_FIRE);
		item(Gear.INFERNAL_CAPE, ItemID.INFERNAL_CAPE);
		item(Gear.AMULET_OF_POWER, ItemID.AMULET_OF_POWER);
		item(Gear.AMULET_OF_GLORY, ItemID.AMULET_OF_GLORY);
		item(Gear.AMULET_OF_FURY, ItemID.ENCHANTED_ONYX_AMULET);
		item(Gear.AMULET_OF_TORTURE, ItemID.ZENYTE_AMULET_ENCHANTED);
		item(Gear.LEATHER_GLOVES, ItemID.LEATHER_GLOVES);
		item(Gear.COMBAT_BRACELET, ItemID.JEWL_BRACELET_OF_COMBAT);
		item(Gear.BARROWS_GLOVES, ItemID.HUNDRED_GAUNTLETS_LEVEL_10);
		item(Gear.FEROCIOUS_GLOVES, ItemID.FEROCIOUS_GLOVES);
		item(Gear.LEATHER_BOOTS, ItemID.LEATHER_BOOTS);
		item(Gear.CLIMBING_BOOTS, ItemID.DEATH_CLIMBINGBOOTS);
		item(Gear.DRAGON_BOOTS, ItemID.DRAGON_BOOTS);
		item(Gear.PRIMORDIAL_BOOTS, ItemID.PRIMORDIAL_BOOTS);
		item(Gear.RING_OF_RECOIL, ItemID.RING_OF_RECOIL);
		item(Gear.RING_OF_WEALTH, ItemID.RING_OF_WEALTH);
		item(Gear.BERSERKER_RING, ItemID.BERZERKER_RING);
		item(Gear.ULTOR_RING, ItemID.ULTOR_RING);

		weapon(Gear.GRANITE_MAUL, ItemID.GRANITE_MAUL, AnimationID.SLAYER_GRANITE_MAUL_ATTACK);
		weapon(Gear.DRAGON_WARHAMMER, ItemID.DRAGON_WARHAMMER, AnimationID.HUMAN_BLUNT_POUND);
		weapon(Gear.DHAROKS_GREATAXE, ItemID.BARROWS_DHAROK_WEAPON, AnimationID.BARROW_DHAROK_SLASH);
		weapon(Gear.TORAGS_HAMMERS, ItemID.BARROWS_TORAG_WEAPON, AnimationID.BARROW_TORAG_CRUSH);
		weapon(Gear.SARADOMIN_SWORD, ItemID.SARADOMIN_SWORD, AnimationID.HUMAN_SWORD_SLASH);
		weapon(Gear.VERACS_FLAIL, ItemID.BARROWS_VERAC_WEAPON, AnimationID.HUMAN_BLUNT_POUND);
		weapon(Gear.GUTHANS_WARSPEAR, ItemID.BARROWS_GUTHAN_WEAPON, AnimationID.BARROW_GUTHAN_CRUSH);
		weapon(Gear.BLADE_OF_SAELDOR, ItemID.BLADE_OF_SAELDOR, AnimationID.HUMAN_SWORD_SLASH);
		weapon(Gear.OSMUMTENS_FANG, ItemID.OSMUMTENS_FANG, AnimationID.HUMAN_SWORD_STAB);
		weapon(Gear.INQUISITORS_MACE, ItemID.INQUISITORS_MACE, AnimationID.HUMAN_BLUNT_POUND);
		weapon(Gear.ARMADYL_GODSWORD, ItemID.AGS, AnimationID.HUMAN_SWORD_SLASH);
		weapon(Gear.SOULREAPER_AXE, ItemID.SOULREAPER, AnimationID.HUMAN_AXE_HACK);
		weapon(Gear.ELDER_MAUL, ItemID.ELDER_MAUL, AnimationID.SLAYER_GRANITE_MAUL_ATTACK);
		weapon(Gear.DRAGON_CLAWS, ItemID.DRAGON_CLAWS, AnimationID.DCLAWS_HUMAN_ATTACK);
		weapon(Gear.KARILS_CROSSBOW, ItemID.BARROWS_KARIL_WEAPON, AnimationID.BARROWS_REPEATING_CROSSBOW_FIRE);
		weapon(Gear.DARK_BOW, ItemID.DARKBOW, AnimationID.HUMAN_BOW);
		weapon(Gear.DRAGON_HUNTER_CROSSBOW, ItemID.DRAGONHUNTER_XBOW, AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD);
		weapon(Gear.BLACK_CHINCHOMPA, ItemID.CHINCHOMPA_BLACK, AnimationID.HUMAN_CHINCHOMPA_ATTACK);
		weapon(Gear.BOW_OF_FAERDHINEN, ItemID.BOW_OF_FAERDHINEN, AnimationID.HUMAN_BOW);
		weapon(Gear.ZARYTE_CROSSBOW, ItemID.ZARYTE_XBOW, AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD);
		staff(Gear.IBANS_STAFF, ItemID.IBANSTAFF, AnimationID.HUMAN_CASTIBANBLAST, SpriteID.Magicon.FIRE_BLAST);
		staff(Gear.MASTER_WAND, ItemID.MAGICTRAINING_WAND_MASTER, AnimationID.HUMAN_CASTWAVE, SpriteID.Magicon.EARTH_BLAST);
		staff(Gear.AHRIMS_STAFF, ItemID.BARROWS_AHRIM_WEAPON, AnimationID.HUMAN_CASTWAVE_STAFF, SpriteID.Magicon.WIND_BLAST);
		staff(Gear.STAFF_OF_THE_DEAD, ItemID.SOTD, AnimationID.HUMAN_CASTWAVE_STAFF, SpriteID.Magicon.WATER_WAVE);
		staff(Gear.TOXIC_STAFF_OF_THE_DEAD, ItemID.TOXIC_SOTD_CHARGED, AnimationID.HUMAN_CASTWAVE_STAFF, SpriteID.Magicon.EARTH_WAVE);
		staff(Gear.KODAI_WAND, ItemID.KODAI_WAND, AnimationID.HUMAN_CASTWAVE, SpriteID.Magicon2.ICE_BARRAGE);
		staff(Gear.HARMONISED_NIGHTMARE_STAFF, ItemID.NIGHTMARE_STAFF_HARMONISED, AnimationID.HUMAN_CASTWAVE_STAFF, SpriteID.Magicon.FIRE_WAVE);
		staff(Gear.VOLATILE_NIGHTMARE_STAFF, ItemID.NIGHTMARE_STAFF_VOLATILE, AnimationID.HUMAN_CASTWAVE_STAFF, SpriteID.Magicon.FIRE_WAVE);
		staff(Gear.EYE_OF_AYAK, ItemID.EYE_OF_AYAK, AnimationID.HUMAN_CASTWAVE, SpriteID.Magicon.WIND_WAVE);
		staff(Gear.ACCURSED_SCEPTRE, ItemID.WILD_CAVE_ACCURSED_CHARGED, AnimationID.HUMAN_CASTWAVE, SpriteID.Magicon.EARTH_WAVE);

		item(Gear.DHAROKS_HELM, ItemID.BARROWS_DHAROK_HEAD);
		item(Gear.GUTHANS_HELM, ItemID.BARROWS_GUTHAN_HEAD);
		item(Gear.VERACS_HELM, ItemID.BARROWS_VERAC_HEAD);
		item(Gear.TORAGS_HELM, ItemID.BARROWS_TORAG_HEAD);
		item(Gear.VOID_MELEE_HELM, ItemID.GAME_PEST_MELEE_HELM);
		item(Gear.INQUISITORS_GREAT_HELM, ItemID.INQUISITORS_HELM);
		item(Gear.DHAROKS_PLATEBODY, ItemID.BARROWS_DHAROK_BODY);
		item(Gear.GUTHANS_PLATEBODY, ItemID.BARROWS_GUTHAN_BODY);
		item(Gear.VERACS_BRASSARD, ItemID.BARROWS_VERAC_BODY);
		item(Gear.TORAGS_PLATEBODY, ItemID.BARROWS_TORAG_BODY);
		item(Gear.INQUISITORS_HAUBERK, ItemID.INQUISITORS_BODY);
		item(Gear.DHAROKS_PLATELEGS, ItemID.BARROWS_DHAROK_LEGS);
		item(Gear.GUTHANS_CHAINSKIRT, ItemID.BARROWS_GUTHAN_LEGS);
		item(Gear.VERACS_PLATESKIRT, ItemID.BARROWS_VERAC_LEGS);
		item(Gear.TORAGS_PLATELEGS, ItemID.BARROWS_TORAG_LEGS);
		item(Gear.INQUISITORS_PLATESKIRT, ItemID.INQUISITORS_SKIRT);
		item(Gear.KARILS_COIF, ItemID.BARROWS_KARIL_HEAD);
		item(Gear.VOID_RANGER_HELM, ItemID.GAME_PEST_ARCHER_HELM);
		item(Gear.KARILS_LEATHERTOP, ItemID.BARROWS_KARIL_BODY);
		item(Gear.KARILS_LEATHERSKIRT, ItemID.BARROWS_KARIL_LEGS);
		item(Gear.VOID_MAGE_HELM, ItemID.GAME_PEST_MAGE_HELM);
		item(Gear.VOID_KNIGHT_TOP, ItemID.PEST_VOID_KNIGHT_TOP);
		item(Gear.VOID_KNIGHT_ROBE, ItemID.PEST_VOID_KNIGHT_ROBES);
		item(Gear.VOID_KNIGHT_GLOVES, ItemID.PEST_VOID_KNIGHT_GLOVES);
		item(Gear.DINHS_BULWARK, ItemID.DINHS_BULWARK);
		item(Gear.UNHOLY_BOOK, ItemID.ZAMORAKBOOK_COMPLETE);
		item(Gear.BOOK_OF_LAW, ItemID.ARMADYLBOOK_COMPLETE);
		item(Gear.ODIUM_WARD, ItemID.ODIUM_WARD);
		item(Gear.DRAGONFIRE_WARD, ItemID.DRAGONFIRE_WARD);
		item(Gear.TWISTED_BUCKLER, ItemID.TWISTED_BUCKLER);
		item(Gear.MALEDICTION_WARD, ItemID.MALEDICTION_WARD);
		item(Gear.IMBUED_SARADOMIN_CAPE, ItemID.MA2_SARADOMIN_CAPE);
		item(Gear.AMULET_OF_BLOOD_FURY, ItemID.BLOOD_AMULET);
		item(Gear.NECKLACE_OF_ANGUISH, ItemID.ZENYTE_NECKLACE_ENCHANTED);
		item(Gear.OCCULT_NECKLACE, ItemID.OCCULT_NECKLACE);
		item(Gear.ZARYTE_VAMBRACES, ItemID.ZARYTE_VAMBRACES);
		item(Gear.TORMENTED_BRACELET, ItemID.ZENYTE_BRACELET_ENCHANTED);
		item(Gear.PEGASIAN_BOOTS, ItemID.PEGASIAN_BOOTS);
		item(Gear.ETERNAL_BOOTS, ItemID.ETERNAL_BOOTS);
		item(Gear.ARCHERS_RING, ItemID.RANGER_RING);
		item(Gear.SEERS_RING, ItemID.SEER_RING);
		item(Gear.RING_OF_SUFFERING, ItemID.ZENYTE_RING_ENCHANTED);
		item(Gear.VENATOR_RING, ItemID.VENATOR_RING);
		item(Gear.MAGUS_RING, ItemID.MAGUS_RING);
		item(Gear.HOLY_BLESSING, ItemID.BLESSING_SARADOMIN);
		item(Gear.RADAS_BLESSING_1, ItemID.ZEAH_BLESSING_EASY);
		item(Gear.DRAGON_ARROWS, ItemID.DRAGON_ARROW);
		item(Gear.RADAS_BLESSING_4, ItemID.ZEAH_BLESSING_ELITE);

		// sets
		weapon(Gear.TZHAAR_KET_OM, ItemID.TZHAAR_MAUL, AnimationID.SLAYER_GRANITE_MAUL_ATTACK);
		item(Gear.OBSIDIAN_HELMET, ItemID.OBSIDIAN_HELMET);
		item(Gear.OBSIDIAN_PLATEBODY, ItemID.OBSIDIAN_PLATEBODY);
		item(Gear.OBSIDIAN_PLATELEGS, ItemID.OBSIDIAN_PLATELEGS);
		item(Gear.CRYSTAL_HELM, ItemID.CRYSTAL_HELMET);
		item(Gear.CRYSTAL_BODY, ItemID.CRYSTAL_CHESTPLATE);
		item(Gear.CRYSTAL_LEGS, ItemID.CRYSTAL_PLATELEGS);
		item(Gear.VIRTUS_MASK, ItemID.VIRTUS_MASK);
		item(Gear.VIRTUS_ROBE_TOP, ItemID.VIRTUS_TOP);
		item(Gear.VIRTUS_ROBE_BOTTOM, ItemID.VIRTUS_LEGS);

		// combinations
		weapon(Gear.ABYSSAL_TENTACLE, ItemID.ABYSSAL_TENTACLE, AnimationID.SLAYER_ABYSSAL_WHIP_ATTACK);
		weapon(Gear.ARMADYL_GODSWORD_OR, ItemID.AGSG, AnimationID.HUMAN_SWORD_SLASH);
		weapon(Gear.SANGUINE_SCYTHE_OF_VITUR, ItemID.SCYTHE_OF_VITUR_BL, AnimationID.SCYTHE_OF_VITUR_ATTACK);
		weapon(Gear.MAGIC_SHORTBOW_I, ItemID.MAGIC_SHORTBOW_I, AnimationID.HUMAN_BOW);
		weapon(Gear.DRAGON_HUNTER_CROSSBOW_T, ItemID.DRAGONHUNTER_XBOW_VORKATH, AnimationID.XBOWS_HUMAN_FIRE_AND_RELOAD);
		staff(Gear.ICE_ANCIENT_SCEPTRE, ItemID.ANCIENT_SCEPTRE_ICE, AnimationID.ZAROS_VERTICAL_CASTING, SpriteID.Magicon2.ICE_BARRAGE);
		staff(Gear.TRIDENT_OF_THE_SWAMP_E, ItemID.TOXIC_TOTS_I_CHARGED, AnimationID.HUMAN_CASTWAVE_STAFF, SpriteID.Magicon.EARTH_WAVE);
		staff(Gear.HOLY_SANGUINESTI_STAFF, ItemID.SANGUINESTI_STAFF_OR, AnimationID.HUMAN_CASTWAVE_STAFF, SpriteID.Magicon.FIRE_WAVE);

		// boss drops
		item(Gear.INFERNAL_MAX_CAPE, ItemID.SKILLCAPE_MAX_INFERNALCAPE);
		item(Gear.AMULET_OF_RANCOUR, ItemID.AMULET_OF_RANCOUR);
		item(Gear.CONFLICTION_GAUNTLETS, ItemID.CONFLICTION_GAUNTLETS);
		item(Gear.AVERNIC_TREADS, ItemID.AVERNIC_TREADS);
		item(Gear.RING_OF_THE_GODS, ItemID.NZONE_ROTG);
		item(Gear.DIZANAS_QUIVER, ItemID.DIZANAS_QUIVER_CHARGED);
		item(Gear.ELYSIAN_SPIRIT_SHIELD, ItemID.ELYSIAN);
		item(Gear.RONDACHE, ItemID.RONDACHE);
		item(Gear.IMBUED_ANCIENT_CAPE, ItemID.MA2_ZAROS_CAPE_DARKER);

		// the Fractured Archive's weapons, with their own animations (the obligator is a megarare)
		weapon(Gear.OBLIGATOR, ItemID.OBLIGATOR, AnimationID.TFA_HUMAN_OBLIGATOR_ATTACK);
		weapon(Gear.ASCENSION_CROSSBOWS, ItemID.ASCENSION_CROSSBOWS, AnimationID.TFA_HUMAN_ASCENSION_CROSSBOW_FIRE_RIGHT);
		staff(Gear.ZORYAS_TOME, ItemID.ZORYAS_TOME, AnimationID.HUMAN_CASTWAVE, SpriteID.Magicon.FIRE_WAVE);
	}

	private static final Map<Pet, Integer> PETS = new EnumMap<>(Pet.class);

	static
	{
		PETS.put(Pet.PET_CHAOS_ELEMENTAL, ItemID.CHAOSELEPET);
		PETS.put(Pet.BABY_MOLE, ItemID.MOLEPET);
		PETS.put(Pet.PRINCE_BLACK_DRAGON, ItemID.KBDPET);
		PETS.put(Pet.PET_DARK_CORE, ItemID.COREPET);
		PETS.put(Pet.TZREK_JAD, ItemID.JAD_PET);
		PETS.put(Pet.LIL_ZIK, ItemID.VERZIKPET);
		PETS.put(Pet.OLMLET, ItemID.OLMPET);
		PETS.put(Pet.VORKI, ItemID.VORKATHPET);
		PETS.put(Pet.TEKTINY, ItemID.TEKTONPET);
		PETS.put(Pet.VANGUARD, ItemID.VANGUARDPET);
		PETS.put(Pet.VASA_MINIRIO, ItemID.VASAPET);
		PETS.put(Pet.VESPINA, ItemID.VESPULAPET);
		PETS.put(Pet.TUMEKENS_GUARDIAN, ItemID.WARDENPET_TUMEKEN);
		PETS.put(Pet.AKKHITO, ItemID.WARDENPET_AKKHA);
		PETS.put(Pet.BABI, ItemID.WARDENPET_BABA);
		PETS.put(Pet.KEPHRITI, ItemID.WARDENPET_KEPHRI);
		PETS.put(Pet.ORION, ItemID.OCELLUSPET);
		PETS.put(Pet.XARI, ItemID.XARTHPET);
		PETS.put(Pet.CHUBBLES, ItemID.JELLYPET);
		PETS.put(Pet.MIMI, ItemID.MOIAPET);
		PETS.put(Pet.DORK, ItemID.BORKPET);
	}

	/** The potion drawn for a filler boost with {@code doses} left (1 to 4). */
	static int boostItem(Boost boost, int doses)
	{
		switch (boost)
		{
			case DIVINE_SUPER_COMBAT:
				return dose(doses, ItemID._1DOSEDIVINECOMBAT, ItemID._2DOSEDIVINECOMBAT, ItemID._3DOSEDIVINECOMBAT, ItemID._4DOSEDIVINECOMBAT);
			case DIVINE_RANGING:
				return dose(doses, ItemID._1DOSEDIVINERANGE, ItemID._2DOSEDIVINERANGE, ItemID._3DOSEDIVINERANGE, ItemID._4DOSEDIVINERANGE);
			case DIVINE_MAGIC:
				return dose(doses, ItemID._1DOSEDIVINEMAGIC, ItemID._2DOSEDIVINEMAGIC, ItemID._3DOSEDIVINEMAGIC, ItemID._4DOSEDIVINEMAGIC);
			case SARADOMIN_BREW:
				return dose(doses, ItemID._1DOSEPOTIONOFSARADOMIN, ItemID._2DOSEPOTIONOFSARADOMIN, ItemID._3DOSEPOTIONOFSARADOMIN,
					ItemID._4DOSEPOTIONOFSARADOMIN);
			case STAMINA:
				return dose(doses, ItemID._1DOSESTAMINA, ItemID._2DOSESTAMINA, ItemID._3DOSESTAMINA, ItemID._4DOSESTAMINA);
			case SUPER_RESTORE:
				return dose(doses, ItemID._1DOSE2RESTORE, ItemID._2DOSE2RESTORE, ItemID._3DOSE2RESTORE, ItemID._4DOSE2RESTORE);
			case ANTIFIRE:
				return dose(doses, ItemID._1DOSE3ANTIDRAGON, ItemID._2DOSE3ANTIDRAGON, ItemID._3DOSE3ANTIDRAGON, ItemID._4DOSE3ANTIDRAGON);
			// the Tombs of Amascut's supplies come in two doses at most
			case SMELLING_SALTS:
				return dose(doses, ItemID.TOA_SUPPLY_STATS_1, ItemID.TOA_SUPPLY_STATS_2, ItemID.TOA_SUPPLY_STATS_2, ItemID.TOA_SUPPLY_STATS_2);
			case LIQUID_ADRENALINE:
				return dose(doses, ItemID.TOA_SUPPLY_ENERGY_1, ItemID.TOA_SUPPLY_ENERGY_2, ItemID.TOA_SUPPLY_ENERGY_2, ItemID.TOA_SUPPLY_ENERGY_2);
			default:
				return dose(doses, ItemID._1DOSE2DEFENSE, ItemID._2DOSE2DEFENSE, ItemID._3DOSE2DEFENSE, ItemID._4DOSE2DEFENSE);
		}
	}

	private static int dose(int doses, int... items)
	{
		return items[Math.max(1, Math.min(items.length, doses)) - 1];
	}

	/** The item a piece of food is drawn as. */
	static int foodItem(Food food)
	{
		switch (food)
		{
			case SHARK:
				return ItemID.SHARK;
			case MANTA_RAY:
				return ItemID.MANTARAY;
			case ANGLERFISH:
				return ItemID.ANGLERFISH;
			default:
				return ItemID.TBWT_COOKED_KARAMBWAN;
		}
	}

	/** The item a pet is drawn as. */
	/** Pickup gem for an XP orb worth {@code xpValue}. */
	static int gemItem(int xpValue)
	{
		if (xpValue >= 25)
		{
			return ItemID.UNCUT_ONYX;
		}
		if (xpValue >= 15)
		{
			return ItemID.UNCUT_DRAGONSTONE;
		}
		if (xpValue >= 8)
		{
			return ItemID.UNCUT_DIAMOND;
		}
		if (xpValue >= 4)
		{
			return ItemID.UNCUT_RUBY;
		}
		if (xpValue >= 2)
		{
			return ItemID.UNCUT_EMERALD;
		}
		return ItemID.UNCUT_SAPPHIRE;
	}

	static int petItem(Pet pet)
	{
		return PETS.getOrDefault(pet, -1);
	}

	/** Item drawn for a symbol on the boss drop's reels. */
	static int prizeItem(BossDrop.Prize prize)
	{
		switch (prize.getKind())
		{
			case COINS:
				return ItemID.COINS_10000;
			case LAMP:
				return ItemID.MACRO_GENILAMP;
			case GEAR:
				return itemId(prize.getGear());
			default:
				return petItem(prize.getPet());
		}
	}

	/** Sprite for a permanent upgrade, or -1 if it is drawn as an item. */
	static int perkSprite(Perk perk)
	{
		switch (perk)
		{
			case MIGHT:
				return SpriteID.Staticons.STRENGTH;
			case ARMOUR:
				return SpriteID.Staticons.DEFENCE;
			case VITALITY:
				return SpriteID.Staticons.HITPOINTS;
			case RECOVERY:
				return SpriteID.Prayeron.RAPID_HEAL;
			case HASTE:
				return SpriteID.Staticons.ATTACK;
			case SPEED:
				return SpriteID.Staticons.AGILITY;
			case MAGNET:
				return SpriteID.Magicon.TELEKINETIC_GRAB;
			case REVIVAL:
				return SpriteID.Prayeron.REDEMPTION;
			default:
				return -1;
		}
	}

	/** Item icon for upgrades drawn as items, or -1. */
	static int perkItem(Perk perk)
	{
		switch (perk)
		{
			case WISDOM:
				return ItemID.MACRO_GENILAMP;
			case GREED:
				return ItemID.COINS_10000;
			case REROLL:
				return ItemID.CASKET;
			default:
				return -1;
		}
	}

	/** The item for a piece of gear, or -1 if none is mapped. */
	static int itemId(Gear gear)
	{
		return ITEMS.getOrDefault(gear, -1);
	}

	/** The attack animation for a weapon, or the punch for fists. */
	static int attackAnimation(Gear weapon)
	{
		return weapon == null ? PUNCH : ATTACKS.getOrDefault(weapon, PUNCH);
	}

	/** Spell icon sprite for a magic weapon, or -1. */
	static int spellSprite(Gear weapon)
	{
		return weapon == null ? -1 : SPELLS.getOrDefault(weapon, -1);
	}

	/** Sprite for a passive at a given rank (1-based), or -1 if it uses an item icon instead. */
	static int passiveSprite(Passive passive, int rank)
	{
		switch (passive)
		{
			case MELEE_PRAYER:
				return pick(rank, SpriteID.Prayeron.BURST_OF_STRENGTH, SpriteID.Prayeron.SUPERHUMAN_STRENGTH,
					SpriteID.Prayeron.ULTIMATE_STRENGTH, SpriteID.Prayeron.CHIVALRY, SpriteID.Prayeron.PIETY);
			case RANGED_PRAYER:
				return pick(rank, SpriteID.Prayeron.SHARP_EYE, SpriteID.Prayeron.HAWK_EYE, SpriteID.Prayeron.EAGLE_EYE,
					SpriteID.Prayeron.RIGOUR);
			case MAGIC_PRAYER:
				return pick(rank, SpriteID.Prayeron.MYSTIC_WILL, SpriteID.Prayeron.MYSTIC_LORE, SpriteID.Prayeron.MYSTIC_MIGHT,
					SpriteID.Prayeron.AUGURY);
			case PROTECTION:
				return pick(rank, SpriteID.Prayeron.THICK_SKIN, SpriteID.Prayeron.ROCK_SKIN, SpriteID.Prayeron.STEEL_SKIN);
			case RAPID_HEAL:
				return SpriteID.Prayeron.RAPID_HEAL;
			case REDEMPTION:
				return SpriteID.Prayeron.REDEMPTION;
			case HITPOINTS:
				return SpriteID.Staticons.HITPOINTS;
			case AGILITY:
				return SpriteID.Staticons.AGILITY;
			case SWIFTNESS:
				return SpriteID.Staticons.ATTACK;
			case LONGRANGE:
				return SpriteID.Staticons.RANGED;
			case MULTICOMBAT:
				return SpriteID.Magicon.WIND_WAVE;
			case TELEKINETIC_GRAB:
				return SpriteID.Magicon.TELEKINETIC_GRAB;
			case VENGEANCE:
				return SpriteID.LunarMagicOn.VENGEANCE;
			case PROTECT_MELEE:
				return SpriteID.Prayeron.PROTECT_FROM_MELEE;
			case PROTECT_MISSILES:
				return SpriteID.Prayeron.PROTECT_FROM_MISSILES;
			case ICE_BARRAGE:
				return pick(rank, SpriteID.Magicon2.ICE_RUSH, SpriteID.Magicon2.ICE_BARRAGE);
			case THRALLS:
				return SpriteID.Staticons.PRAYER;
			case THIEVING:
				return SpriteID.Staticons.THIEVING;
			case RETRIBUTION:
				return SpriteID.Prayeron.RETRIBUTION;
			case BLOOD_BARRAGE:
				return pick(rank, SpriteID.Magicon2.BLOOD_RUSH, SpriteID.Magicon2.BLOOD_BURST, SpriteID.Magicon2.BLOOD_BLITZ,
					SpriteID.Magicon2.BLOOD_BARRAGE);
			case GOD_SPELLS:
				return pick(rank, SpriteID.Magicon.SARADOMIN_STRIKE, SpriteID.Magicon.CLAWS_OF_GUTHIX, SpriteID.Magicon.FLAMES_OF_ZAMORAK);
			default:
				return -1;
		}
	}

	/** Item icon for passives drawn as items, or -1. */
	static int passiveItem(Passive passive)
	{
		switch (passive)
		{
			case LAMP:
				return ItemID.MACRO_GENILAMP;
			case SPECIAL_ATTACK:
				return ItemID.DRAGON_DAGGER;
			case SUPER_COMBAT:
				return ItemID._4DOSE2COMBAT;
			case RANGING_POTION:
				return ItemID._4DOSERANGERSPOTION;
			case IMBUED_HEART:
				return ItemID.IMBUED_HEART;
			case BONECRUSHER:
				return ItemID.BONECRUSHER;
			case CANNON:
				return ItemID.MCANNONBALL;
			case CHINCHOMPAS:
				return ItemID.CHINCHOMPA_BLACK;
			case OVERLOAD:
				return ItemID.NZONE4DOSEOVERLOADPOTION;
			case SLAYER:
				return ItemID.SLAYER_HELM;
			case DRAGONFIRE:
				return ItemID.DRAGONFIRE_SHIELD;
			default:
				return -1;
		}
	}

	private static int pick(int rank, int... sprites)
	{
		return sprites[Math.max(0, Math.min(sprites.length, rank) - 1)];
	}
}
