package com.osrssurvivors.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Equipment that can drop as a reward. Items are ranked in tiers per slot (and per combat style
 * for weapons, armour, shields and the best accessories); an upgrade offer is an item one tier
 * above what you wear, and some tiers have several items to choose between (Barrows sets,
 * godswords, crossbows...). Weapon damage follows one curve per tier for every style, so the
 * styles stay level with each other at every stage of a run. Weapons attack at the game's own
 * speeds (in ticks) and get faster as the run goes on (see {@link Build#attackCooldown}). Unique
 * items sit outside the chains: combination weapons and boss drops.
 */
public enum Gear
{
	// melee weapons; speeds are the game's attack speeds in ticks
	BRONZE_SCIMITAR(weapon("Bronze scimitar", Style.MELEE, 1, 4, 46)),
	IRON_SCIMITAR(weapon("Iron scimitar", Style.MELEE, 2, 4, 46)),
	STEEL_SCIMITAR(weapon("Steel scimitar", Style.MELEE, 3, 4, 46)),
	MITHRIL_SCIMITAR(weapon("Mithril scimitar", Style.MELEE, 4, 4, 46)),
	ADAMANT_SCIMITAR(weapon("Adamant scimitar", Style.MELEE, 5, 4, 46)),
	TZHAAR_KET_OM(weapon("Tzhaar-ket-om", Style.MELEE, 5, 7, 50).twoHanded().cleave(2).power(1.05).set(GearSet.OBSIDIAN)),
	RUNE_SCIMITAR(weapon("Rune scimitar", Style.MELEE, 6, 4, 48)),
	GRANITE_MAUL(weapon("Granite maul", Style.MELEE, 6, 7, 50).twoHanded().cleave(2)),
	DRAGON_SCIMITAR(weapon("Dragon scimitar", Style.MELEE, 7, 4, 48)),
	DRAGON_WARHAMMER(weapon("Dragon warhammer", Style.MELEE, 7, 6, 48).power(1.05)),
	DHAROKS_GREATAXE(weapon("Dharok's greataxe", Style.MELEE, 7, 7, 52).twoHanded().cleave(1).set(GearSet.DHAROK)),
	TORAGS_HAMMERS(weapon("Torag's hammers", Style.MELEE, 7, 5, 46).set(GearSet.TORAG)),
	ABYSSAL_WHIP(weapon("Abyssal whip", Style.MELEE, 8, 4, 62)),
	SARADOMIN_SWORD(weapon("Saradomin sword", Style.MELEE, 8, 4, 54).twoHanded().cleave(1)),
	VERACS_FLAIL(weapon("Verac's flail", Style.MELEE, 8, 5, 50).set(GearSet.VERAC)),
	GUTHANS_WARSPEAR(weapon("Guthan's warspear", Style.MELEE, 8, 5, 60).twoHanded().set(GearSet.GUTHAN)),
	GHRAZI_RAPIER(weapon("Ghrazi rapier", Style.MELEE, 9, 4, 50)),
	BLADE_OF_SAELDOR(weapon("Blade of saeldor", Style.MELEE, 9, 4, 54)),
	OSMUMTENS_FANG(weapon("Osmumten's fang", Style.MELEE, 9, 5, 50).power(1.03)),
	INQUISITORS_MACE(weapon("Inquisitor's mace", Style.MELEE, 9, 4, 48).set(GearSet.INQUISITOR)),
	ARMADYL_GODSWORD(weapon("Armadyl godsword", Style.MELEE, 9, 6, 58).twoHanded().cleave(2)),
	SCYTHE_OF_VITUR(weapon("Scythe of vitur", Style.MELEE, 10, 5, 58).targets(3).twoHanded()),
	SOULREAPER_AXE(weapon("Soulreaper axe", Style.MELEE, 10, 5, 54).twoHanded().cleave(1).power(1.05)),
	ELDER_MAUL(weapon("Elder maul", Style.MELEE, 10, 6, 54).twoHanded().cleave(3)),

	// ranged weapons, on rapid
	SHORTBOW(weapon("Shortbow", Style.RANGED, 1, 3, 210).twoHanded().missile(Missile.ARROW)),
	OAK_SHORTBOW(weapon("Oak shortbow", Style.RANGED, 2, 3, 210).twoHanded().missile(Missile.ARROW)),
	WILLOW_SHORTBOW(weapon("Willow shortbow", Style.RANGED, 3, 3, 215).twoHanded().missile(Missile.ARROW)),
	MAPLE_SHORTBOW(weapon("Maple shortbow", Style.RANGED, 4, 3, 220).twoHanded().missile(Missile.ARROW)),
	YEW_SHORTBOW(weapon("Yew shortbow", Style.RANGED, 5, 3, 225).twoHanded().missile(Missile.ARROW)),
	MAGIC_SHORTBOW(weapon("Magic shortbow", Style.RANGED, 6, 3, 230).twoHanded().missile(Missile.ARROW)),
	KARILS_CROSSBOW(weapon("Karil's crossbow", Style.RANGED, 6, 3, 220).twoHanded().missile(Missile.BOLT).set(GearSet.KARIL)),
	RUNE_CROSSBOW(weapon("Rune crossbow", Style.RANGED, 7, 5, 240).missile(Missile.BOLT)),
	DARK_BOW(weapon("Dark bow", Style.RANGED, 7, 8, 230).twoHanded().volley(2).missile(Missile.ARROW)),
	ARMADYL_CROSSBOW(weapon("Armadyl crossbow", Style.RANGED, 8, 5, 250).missile(Missile.BOLT)),
	DRAGON_HUNTER_CROSSBOW(weapon("Dragon hunter crossbow", Style.RANGED, 8, 5, 245).missile(Missile.BOLT).power(1.03)),
	BLACK_CHINCHOMPA(weapon("Black chinchompa", Style.RANGED, 8, 3, 200).splash(40).missile(Missile.CHINCHOMPA)),
	TOXIC_BLOWPIPE(weapon("Toxic blowpipe", Style.RANGED, 9, 2, 200).twoHanded().missile(Missile.DART)),
	BOW_OF_FAERDHINEN(weapon("Bow of faerdhinen", Style.RANGED, 9, 4, 260).twoHanded().missile(Missile.ARROW)),
	ZARYTE_CROSSBOW(weapon("Zaryte crossbow", Style.RANGED, 9, 5, 260).missile(Missile.BOLT)),
	TWISTED_BOW(weapon("Twisted bow", Style.RANGED, 10, 5, 300).twoHanded().missile(Missile.ARROW)),

	// magic weapons
	STAFF_OF_AIR(weapon("Staff of air", Style.MAGIC, 1, 5, 190).twoHanded().missile(Missile.SPELL)),
	STAFF_OF_FIRE(weapon("Staff of fire", Style.MAGIC, 2, 5, 195).twoHanded().missile(Missile.SPELL)),
	MYSTIC_FIRE_STAFF(weapon("Mystic fire staff", Style.MAGIC, 3, 5, 200).twoHanded().missile(Missile.SPELL)),
	IBANS_STAFF(weapon("Iban's staff", Style.MAGIC, 3, 5, 200).twoHanded().splash(12).missile(Missile.SPELL)),
	ANCIENT_STAFF(weapon("Ancient staff", Style.MAGIC, 4, 5, 200).twoHanded().splash(16).missile(Missile.SPELL)),
	MASTER_WAND(weapon("Master wand", Style.MAGIC, 4, 5, 200).missile(Missile.SPELL)),
	AHRIMS_STAFF(weapon("Ahrim's staff", Style.MAGIC, 4, 5, 205).twoHanded().missile(Missile.SPELL).set(GearSet.AHRIM)),
	TRIDENT_OF_THE_SEAS(weapon("Trident of the seas", Style.MAGIC, 5, 4, 220).missile(Missile.SPELL)),
	STAFF_OF_THE_DEAD(weapon("Staff of the dead", Style.MAGIC, 5, 5, 205).missile(Missile.SPELL)),
	TRIDENT_OF_THE_SWAMP(weapon("Trident of the swamp", Style.MAGIC, 6, 4, 220).missile(Missile.SPELL)),
	TOXIC_STAFF_OF_THE_DEAD(weapon("Toxic staff of the dead", Style.MAGIC, 6, 5, 210).missile(Missile.SPELL)),
	SANGUINESTI_STAFF(weapon("Sanguinesti staff", Style.MAGIC, 7, 4, 225).lifesteal(0.06).missile(Missile.SPELL)),
	KODAI_WAND(weapon("Kodai wand", Style.MAGIC, 7, 5, 220).splash(8).missile(Missile.SPELL)),
	HARMONISED_NIGHTMARE_STAFF(weapon("Harmonised nightmare staff", Style.MAGIC, 8, 4, 225).missile(Missile.SPELL)),
	VOLATILE_NIGHTMARE_STAFF(weapon("Volatile nightmare staff", Style.MAGIC, 8, 5, 230).splash(24).missile(Missile.SPELL)),
	EYE_OF_AYAK(weapon("Eye of ayak", Style.MAGIC, 9, 3, 230).missile(Missile.SPELL)),
	ACCURSED_SCEPTRE(weapon("Accursed sceptre", Style.MAGIC, 9, 4, 235).splash(10).missile(Missile.SPELL)),
	TUMEKENS_SHADOW(weapon("Tumeken's shadow", Style.MAGIC, 10, 5, 240).twoHanded().missile(Missile.SPELL)),

	// combination weapons: never in a chain, only offered when a weapon meets its upgrade (see Combination)
	ABYSSAL_TENTACLE(weapon("Abyssal tentacle", Style.MELEE, 8, 4, 70).power(1.3).unique()),
	ARMADYL_GODSWORD_OR(weapon("Armadyl godsword (or)", Style.MELEE, 9, 6, 62).twoHanded().cleave(3).power(1.25).unique()),
	SANGUINE_SCYTHE_OF_VITUR(weapon("Sanguine scythe of vitur", Style.MELEE, 10, 5, 62).targets(3).twoHanded().lifesteal(0.05)
		.power(1.2).unique()),
	MAGIC_SHORTBOW_I(weapon("Magic shortbow (i)", Style.RANGED, 6, 3, 270).twoHanded().missile(Missile.ARROW).power(1.3).unique()),
	DRAGON_HUNTER_CROSSBOW_T(weapon("Dragon hunter crossbow (t)", Style.RANGED, 8, 5, 260).missile(Missile.BOLT).power(1.3).unique()),
	ICE_ANCIENT_SCEPTRE(weapon("Ice ancient sceptre", Style.MAGIC, 4, 5, 210).splash(28).freeze(1.2).missile(Missile.SPELL)
		.power(1.3).unique()),
	TRIDENT_OF_THE_SWAMP_E(weapon("Trident of the swamp (e)", Style.MAGIC, 6, 4, 230).targets(2).missile(Missile.SPELL).power(1.2)
		.unique()),
	HOLY_SANGUINESTI_STAFF(weapon("Holy sanguinesti staff", Style.MAGIC, 7, 4, 235).lifesteal(0.12).missile(Missile.SPELL)
		.power(1.2).unique()),

	// melee armour
	BRONZE_FULL_HELM(armour("Bronze full helm", Style.MELEE, Slot.HEAD, 1).hidesHairAndJaw()),
	IRON_FULL_HELM(armour("Iron full helm", Style.MELEE, Slot.HEAD, 2).hidesHairAndJaw()),
	STEEL_FULL_HELM(armour("Steel full helm", Style.MELEE, Slot.HEAD, 3).hidesHairAndJaw()),
	MITHRIL_FULL_HELM(armour("Mithril full helm", Style.MELEE, Slot.HEAD, 4).hidesHairAndJaw()),
	ADAMANT_FULL_HELM(armour("Adamant full helm", Style.MELEE, Slot.HEAD, 5).hidesHairAndJaw()),
	RUNE_FULL_HELM(armour("Rune full helm", Style.MELEE, Slot.HEAD, 6).hidesHairAndJaw()),
	DHAROKS_HELM(armour("Dharok's helm", Style.MELEE, Slot.HEAD, 6).set(GearSet.DHAROK).hidesHairAndJaw()),
	GUTHANS_HELM(armour("Guthan's helm", Style.MELEE, Slot.HEAD, 6).set(GearSet.GUTHAN).hidesHairAndJaw()),
	VERACS_HELM(armour("Verac's helm", Style.MELEE, Slot.HEAD, 6).set(GearSet.VERAC).hidesHairAndJaw()),
	TORAGS_HELM(armour("Torag's helm", Style.MELEE, Slot.HEAD, 6).set(GearSet.TORAG).hidesHairAndJaw()),
	VOID_MELEE_HELM(armour("Void melee helm", Style.MELEE, Slot.HEAD, 6).set(GearSet.VOID).hidesHair()),
	DRAGON_FULL_HELM(armour("Dragon full helm", Style.MELEE, Slot.HEAD, 7).hidesHairAndJaw()),
	NEITIZNOT_FACEGUARD(armour("Neitiznot faceguard", Style.MELEE, Slot.HEAD, 8).hidesHairAndJaw()),
	INQUISITORS_GREAT_HELM(armour("Inquisitor's great helm", Style.MELEE, Slot.HEAD, 8).set(GearSet.INQUISITOR).hidesHairAndJaw()),
	TORVA_FULL_HELM(armour("Torva full helm", Style.MELEE, Slot.HEAD, 9).hidesHairAndJaw()),
	OBSIDIAN_HELMET(armour("Obsidian helmet", Style.MELEE, Slot.HEAD, 5).set(GearSet.OBSIDIAN).hidesHairAndJaw()),
	BRONZE_PLATEBODY(armour("Bronze platebody", Style.MELEE, Slot.BODY, 1)),
	IRON_PLATEBODY(armour("Iron platebody", Style.MELEE, Slot.BODY, 2)),
	STEEL_PLATEBODY(armour("Steel platebody", Style.MELEE, Slot.BODY, 3)),
	MITHRIL_PLATEBODY(armour("Mithril platebody", Style.MELEE, Slot.BODY, 4)),
	ADAMANT_PLATEBODY(armour("Adamant platebody", Style.MELEE, Slot.BODY, 5)),
	RUNE_PLATEBODY(armour("Rune platebody", Style.MELEE, Slot.BODY, 6)),
	DHAROKS_PLATEBODY(armour("Dharok's platebody", Style.MELEE, Slot.BODY, 6).set(GearSet.DHAROK)),
	GUTHANS_PLATEBODY(armour("Guthan's platebody", Style.MELEE, Slot.BODY, 6).set(GearSet.GUTHAN)),
	VERACS_BRASSARD(armour("Verac's brassard", Style.MELEE, Slot.BODY, 6).set(GearSet.VERAC)),
	TORAGS_PLATEBODY(armour("Torag's platebody", Style.MELEE, Slot.BODY, 6).set(GearSet.TORAG)),
	DRAGON_PLATEBODY(armour("Dragon platebody", Style.MELEE, Slot.BODY, 7)),
	BANDOS_CHESTPLATE(armour("Bandos chestplate", Style.MELEE, Slot.BODY, 8)),
	INQUISITORS_HAUBERK(armour("Inquisitor's hauberk", Style.MELEE, Slot.BODY, 8).set(GearSet.INQUISITOR)),
	TORVA_PLATEBODY(armour("Torva platebody", Style.MELEE, Slot.BODY, 9)),
	OBSIDIAN_PLATEBODY(armour("Obsidian platebody", Style.MELEE, Slot.BODY, 5).set(GearSet.OBSIDIAN)),
	BRONZE_PLATELEGS(armour("Bronze platelegs", Style.MELEE, Slot.LEGS, 1)),
	IRON_PLATELEGS(armour("Iron platelegs", Style.MELEE, Slot.LEGS, 2)),
	STEEL_PLATELEGS(armour("Steel platelegs", Style.MELEE, Slot.LEGS, 3)),
	MITHRIL_PLATELEGS(armour("Mithril platelegs", Style.MELEE, Slot.LEGS, 4)),
	ADAMANT_PLATELEGS(armour("Adamant platelegs", Style.MELEE, Slot.LEGS, 5)),
	RUNE_PLATELEGS(armour("Rune platelegs", Style.MELEE, Slot.LEGS, 6)),
	DHAROKS_PLATELEGS(armour("Dharok's platelegs", Style.MELEE, Slot.LEGS, 6).set(GearSet.DHAROK)),
	GUTHANS_CHAINSKIRT(armour("Guthan's chainskirt", Style.MELEE, Slot.LEGS, 6).set(GearSet.GUTHAN)),
	VERACS_PLATESKIRT(armour("Verac's plateskirt", Style.MELEE, Slot.LEGS, 6).set(GearSet.VERAC)),
	TORAGS_PLATELEGS(armour("Torag's platelegs", Style.MELEE, Slot.LEGS, 6).set(GearSet.TORAG)),
	DRAGON_PLATELEGS(armour("Dragon platelegs", Style.MELEE, Slot.LEGS, 7)),
	BANDOS_TASSETS(armour("Bandos tassets", Style.MELEE, Slot.LEGS, 8)),
	INQUISITORS_PLATESKIRT(armour("Inquisitor's plateskirt", Style.MELEE, Slot.LEGS, 8).set(GearSet.INQUISITOR)),
	TORVA_PLATELEGS(armour("Torva platelegs", Style.MELEE, Slot.LEGS, 9)),
	OBSIDIAN_PLATELEGS(armour("Obsidian platelegs", Style.MELEE, Slot.LEGS, 5).set(GearSet.OBSIDIAN)),

	// ranged armour
	LEATHER_COWL(armour("Leather cowl", Style.RANGED, Slot.HEAD, 1).hidesHair()),
	COIF(armour("Coif", Style.RANGED, Slot.HEAD, 2).hidesHair()),
	ROBIN_HOOD_HAT(armour("Robin hood hat", Style.RANGED, Slot.HEAD, 3)),
	KARILS_COIF(armour("Karil's coif", Style.RANGED, Slot.HEAD, 4).set(GearSet.KARIL).hidesHair()),
	VOID_RANGER_HELM(armour("Void ranger helm", Style.RANGED, Slot.HEAD, 4).set(GearSet.VOID).hidesHair()),
	ARMADYL_HELMET(armour("Armadyl helmet", Style.RANGED, Slot.HEAD, 4).hidesHairAndJaw()),
	MASORI_MASK(armour("Masori mask", Style.RANGED, Slot.HEAD, 5).hidesHairAndJaw()),
	CRYSTAL_HELM(armour("Crystal helm", Style.RANGED, Slot.HEAD, 4).set(GearSet.CRYSTAL).hidesHair()),
	LEATHER_BODY(armour("Leather body", Style.RANGED, Slot.BODY, 1)),
	STUDDED_BODY(armour("Studded body", Style.RANGED, Slot.BODY, 2)),
	GREEN_DHIDE_BODY(armour("Green d'hide body", Style.RANGED, Slot.BODY, 3)),
	BLUE_DHIDE_BODY(armour("Blue d'hide body", Style.RANGED, Slot.BODY, 4)),
	RED_DHIDE_BODY(armour("Red d'hide body", Style.RANGED, Slot.BODY, 5)),
	BLACK_DHIDE_BODY(armour("Black d'hide body", Style.RANGED, Slot.BODY, 6)),
	KARILS_LEATHERTOP(armour("Karil's leathertop", Style.RANGED, Slot.BODY, 6).set(GearSet.KARIL)),
	ARMADYL_CHESTPLATE(armour("Armadyl chestplate", Style.RANGED, Slot.BODY, 7)),
	MASORI_BODY(armour("Masori body", Style.RANGED, Slot.BODY, 8)),
	CRYSTAL_BODY(armour("Crystal body", Style.RANGED, Slot.BODY, 7).set(GearSet.CRYSTAL)),
	LEATHER_CHAPS(armour("Leather chaps", Style.RANGED, Slot.LEGS, 1)),
	STUDDED_CHAPS(armour("Studded chaps", Style.RANGED, Slot.LEGS, 2)),
	GREEN_DHIDE_CHAPS(armour("Green d'hide chaps", Style.RANGED, Slot.LEGS, 3)),
	BLUE_DHIDE_CHAPS(armour("Blue d'hide chaps", Style.RANGED, Slot.LEGS, 4)),
	RED_DHIDE_CHAPS(armour("Red d'hide chaps", Style.RANGED, Slot.LEGS, 5)),
	BLACK_DHIDE_CHAPS(armour("Black d'hide chaps", Style.RANGED, Slot.LEGS, 6)),
	KARILS_LEATHERSKIRT(armour("Karil's leatherskirt", Style.RANGED, Slot.LEGS, 6).set(GearSet.KARIL)),
	ARMADYL_CHAINSKIRT(armour("Armadyl chainskirt", Style.RANGED, Slot.LEGS, 7)),
	MASORI_CHAPS(armour("Masori chaps", Style.RANGED, Slot.LEGS, 8)),
	CRYSTAL_LEGS(armour("Crystal legs", Style.RANGED, Slot.LEGS, 7).set(GearSet.CRYSTAL)),

	// magic armour
	WIZARD_HAT(armour("Wizard hat", Style.MAGIC, Slot.HEAD, 1)),
	MYSTIC_HAT(armour("Mystic hat", Style.MAGIC, Slot.HEAD, 2).hidesHair()),
	INFINITY_HAT(armour("Infinity hat", Style.MAGIC, Slot.HEAD, 3).hidesHair()),
	AHRIMS_HOOD(armour("Ahrim's hood", Style.MAGIC, Slot.HEAD, 4).set(GearSet.AHRIM).hidesHair()),
	VOID_MAGE_HELM(armour("Void mage helm", Style.MAGIC, Slot.HEAD, 4).set(GearSet.VOID).hidesHair()),
	ANCESTRAL_HAT(armour("Ancestral hat", Style.MAGIC, Slot.HEAD, 5).hidesHairAndJaw()),
	VIRTUS_MASK(armour("Virtus mask", Style.MAGIC, Slot.HEAD, 5).set(GearSet.VIRTUS).hidesHairAndJaw()),
	WIZARD_ROBE(armour("Wizard robe", Style.MAGIC, Slot.BODY, 1)),
	MYSTIC_ROBE_TOP(armour("Mystic robe top", Style.MAGIC, Slot.BODY, 2)),
	INFINITY_TOP(armour("Infinity top", Style.MAGIC, Slot.BODY, 3)),
	AHRIMS_ROBETOP(armour("Ahrim's robetop", Style.MAGIC, Slot.BODY, 4).set(GearSet.AHRIM)),
	ANCESTRAL_ROBE_TOP(armour("Ancestral robe top", Style.MAGIC, Slot.BODY, 5)),
	VIRTUS_ROBE_TOP(armour("Virtus robe top", Style.MAGIC, Slot.BODY, 5).set(GearSet.VIRTUS)),
	WIZARD_SKIRT(armour("Blue skirt", Style.MAGIC, Slot.LEGS, 1)),
	MYSTIC_ROBE_BOTTOM(armour("Mystic robe bottom", Style.MAGIC, Slot.LEGS, 2)),
	INFINITY_BOTTOMS(armour("Infinity bottoms", Style.MAGIC, Slot.LEGS, 3)),
	AHRIMS_ROBESKIRT(armour("Ahrim's robeskirt", Style.MAGIC, Slot.LEGS, 4).set(GearSet.AHRIM)),
	ANCESTRAL_ROBE_BOTTOM(armour("Ancestral robe bottom", Style.MAGIC, Slot.LEGS, 5)),
	VIRTUS_ROBE_BOTTOM(armour("Virtus robe bottom", Style.MAGIC, Slot.LEGS, 5).set(GearSet.VIRTUS)),

	// Void knight robes suit every style; the helm decides which (tier 0: offered beside a chain, never in it)
	VOID_KNIGHT_TOP(armour("Void knight top", null, Slot.BODY, 0).power(0.6).set(GearSet.VOID)),
	VOID_KNIGHT_ROBE(armour("Void knight robe", null, Slot.LEGS, 0).power(0.6).set(GearSet.VOID)),

	// shields (one-handed weapons only)
	BRONZE_DEFENDER(armour("Bronze defender", Style.MELEE, Slot.SHIELD, 1)),
	IRON_DEFENDER(armour("Iron defender", Style.MELEE, Slot.SHIELD, 2)),
	STEEL_DEFENDER(armour("Steel defender", Style.MELEE, Slot.SHIELD, 3)),
	MITHRIL_DEFENDER(armour("Mithril defender", Style.MELEE, Slot.SHIELD, 4)),
	ADAMANT_DEFENDER(armour("Adamant defender", Style.MELEE, Slot.SHIELD, 5)),
	RUNE_DEFENDER(armour("Rune defender", Style.MELEE, Slot.SHIELD, 6)),
	DRAGON_DEFENDER(armour("Dragon defender", Style.MELEE, Slot.SHIELD, 7)),
	AVERNIC_DEFENDER(armour("Avernic defender", Style.MELEE, Slot.SHIELD, 8)),
	DINHS_BULWARK(armour("Dinh's bulwark", Style.MELEE, Slot.SHIELD, 8).defence(30)),
	UNHOLY_BOOK(armour("Unholy book", Style.RANGED, Slot.SHIELD, 1)),
	BOOK_OF_LAW(armour("Book of law", Style.RANGED, Slot.SHIELD, 2)),
	ODIUM_WARD(armour("Odium ward", Style.RANGED, Slot.SHIELD, 3)),
	DRAGONFIRE_WARD(armour("Dragonfire ward", Style.RANGED, Slot.SHIELD, 4)),
	TWISTED_BUCKLER(armour("Twisted buckler", Style.RANGED, Slot.SHIELD, 5)),
	MAGES_BOOK(armour("Mage's book", Style.MAGIC, Slot.SHIELD, 1)),
	TOME_OF_FIRE(armour("Tome of fire", Style.MAGIC, Slot.SHIELD, 2)),
	MALEDICTION_WARD(armour("Malediction ward", Style.MAGIC, Slot.SHIELD, 3)),
	ARCANE_SPIRIT_SHIELD(armour("Arcane spirit shield", Style.MAGIC, Slot.SHIELD, 4)),
	ELIDINIS_WARD(armour("Elidinis' ward", Style.MAGIC, Slot.SHIELD, 5)),

	// accessories; the best of each are made for one style
	RED_CAPE(accessory("Red cape", Slot.CAPE, 1).damage(0.02)),
	OBSIDIAN_CAPE(accessory("Obsidian cape", Slot.CAPE, 2).damage(0.04)),
	FIRE_CAPE(accessory("Fire cape", Slot.CAPE, 3).damage(0.07)),
	IMBUED_SARADOMIN_CAPE(accessory("Imbued saradomin cape", Slot.CAPE, 3).damage(0.05).magnet(40)),
	INFERNAL_CAPE(accessory("Infernal cape", Slot.CAPE, 4).damage(0.10)),
	AMULET_OF_POWER(accessory("Amulet of power", Slot.NECK, 1).damage(0.03)),
	AMULET_OF_GLORY(accessory("Amulet of glory", Slot.NECK, 2).damage(0.05)),
	AMULET_OF_FURY(accessory("Amulet of fury", Slot.NECK, 3).damage(0.07).defence(10)),
	AMULET_OF_BLOOD_FURY(accessory("Amulet of blood fury", Slot.NECK, 3).damage(0.05).lifesteal(0.03)),
	AMULET_OF_TORTURE(accessory("Amulet of torture", Slot.NECK, 4).style(Style.MELEE).damage(0.12)),
	NECKLACE_OF_ANGUISH(accessory("Necklace of anguish", Slot.NECK, 4).style(Style.RANGED).damage(0.12)),
	OCCULT_NECKLACE(accessory("Occult necklace", Slot.NECK, 4).style(Style.MAGIC).damage(0.12)),
	LEATHER_GLOVES(accessory("Leather gloves", Slot.HANDS, 1).damage(0.02)),
	COMBAT_BRACELET(accessory("Combat bracelet", Slot.HANDS, 2).damage(0.04)),
	VOID_KNIGHT_GLOVES(accessory("Void knight gloves", Slot.HANDS, 2).damage(0.03).set(GearSet.VOID)),
	BARROWS_GLOVES(accessory("Barrows gloves", Slot.HANDS, 3).damage(0.07)),
	FEROCIOUS_GLOVES(accessory("Ferocious gloves", Slot.HANDS, 4).style(Style.MELEE).damage(0.10)),
	ZARYTE_VAMBRACES(accessory("Zaryte vambraces", Slot.HANDS, 4).style(Style.RANGED).damage(0.10)),
	TORMENTED_BRACELET(accessory("Tormented bracelet", Slot.HANDS, 4).style(Style.MAGIC).damage(0.10)),
	LEATHER_BOOTS(accessory("Leather boots", Slot.FEET, 1).speed(0.03)),
	CLIMBING_BOOTS(accessory("Climbing boots", Slot.FEET, 2).speed(0.05)),
	DRAGON_BOOTS(accessory("Dragon boots", Slot.FEET, 3).speed(0.06).damage(0.03)),
	PRIMORDIAL_BOOTS(accessory("Primordial boots", Slot.FEET, 4).style(Style.MELEE).speed(0.08).damage(0.05)),
	PEGASIAN_BOOTS(accessory("Pegasian boots", Slot.FEET, 4).style(Style.RANGED).speed(0.08).damage(0.05)),
	ETERNAL_BOOTS(accessory("Eternal boots", Slot.FEET, 4).style(Style.MAGIC).speed(0.08).damage(0.05)),
	RING_OF_RECOIL(accessory("Ring of recoil", Slot.RING, 1).reflect(8)),
	RING_OF_WEALTH(accessory("Ring of wealth", Slot.RING, 2).magnet(60)),
	BERSERKER_RING(accessory("Berserker ring", Slot.RING, 3).style(Style.MELEE).damage(0.08)),
	ARCHERS_RING(accessory("Archers ring", Slot.RING, 3).style(Style.RANGED).damage(0.08)),
	SEERS_RING(accessory("Seers ring", Slot.RING, 3).style(Style.MAGIC).damage(0.08)),
	RING_OF_SUFFERING(accessory("Ring of suffering", Slot.RING, 3).defence(15).reflect(15)),
	ULTOR_RING(accessory("Ultor ring", Slot.RING, 4).style(Style.MELEE).damage(0.12)),
	VENATOR_RING(accessory("Venator ring", Slot.RING, 4).style(Style.RANGED).damage(0.12)),
	MAGUS_RING(accessory("Magus ring", Slot.RING, 4).style(Style.MAGIC).damage(0.12)),
	HOLY_BLESSING(accessory("Holy blessing", Slot.AMMO, 1).damage(0.02)),
	RADAS_BLESSING_1(accessory("Rada's blessing 1", Slot.AMMO, 2).damage(0.03)),
	DRAGON_ARROWS(accessory("Dragon arrows", Slot.AMMO, 3).style(Style.RANGED).damage(0.06)),
	RADAS_BLESSING_4(accessory("Rada's blessing 4", Slot.AMMO, 3).damage(0.05).speed(0.02)),

	// boss drops: only from a champion's casket (see BossDrop), better than anything in their slot's chain
	INFERNAL_MAX_CAPE(accessory("Infernal max cape", Slot.CAPE, 5).damage(0.13).magnet(30).unique()),
	AMULET_OF_RANCOUR(accessory("Amulet of rancour", Slot.NECK, 5).damage(0.14).defence(10).unique()),
	CONFLICTION_GAUNTLETS(accessory("Confliction gauntlets", Slot.HANDS, 5).damage(0.11).defence(10).unique()),
	AVERNIC_TREADS(accessory("Avernic treads", Slot.FEET, 5).speed(0.10).damage(0.07).unique()),
	RING_OF_THE_GODS(accessory("Ring of the gods (i)", Slot.RING, 5).damage(0.12).magnet(60).unique()),
	DIZANAS_QUIVER(accessory("Dizana's quiver", Slot.AMMO, 4).damage(0.08).speed(0.03).unique()),
	ELYSIAN_SPIRIT_SHIELD(accessory("Elysian spirit shield", Slot.SHIELD, 9).defence(70).damage(0.04).unique());

	/** Seconds per game tick. */
	public static final double TICK = 0.6;

	/** How a weapon's attacks travel to the target. */
	public enum Missile
	{
		NONE, ARROW, BOLT, DART, SPELL, CHINCHOMPA
	}

	/**
	 * Weapon damage per second at each tier, the same curve for every style, and each style's
	 * share of it. Melee hits hardest but has to stand in the crowd; ranged pierces lines of
	 * monsters from afar; magic splashes clumps. The shares make up for those differences.
	 */
	static final class Balance
	{
		static final double BASE_DPS = 17;
		static final double GROWTH = 1.22;
		static final double MELEE = 1.1;
		static final double RANGED = 1.0;
		static final double MAGIC = 0.97;

		private Balance()
		{
		}

		static double dps(Style style, int tier)
		{
			double share = style == Style.MELEE ? MELEE : style == Style.RANGED ? RANGED : MAGIC;
			return BASE_DPS * Math.pow(GROWTH, tier - 1) * share;
		}
	}

	/** Builder for the many optional stats; keeps the table above readable. */
	static final class Spec
	{
		final String name;
		final Slot slot;
		Style style;
		final int tier;
		int ticks;
		double cooldown;
		double range;
		double powerScale = 1;
		int targets = 1;
		int cleave;
		int volley = 1;
		double splash;
		double lifesteal;
		boolean twoHanded;
		Missile missile = Missile.NONE;
		double fixedPower = -1;
		double extraDefence;
		double damageBonus;
		double speedBonus;
		double magnet;
		double reflectDps;
		GearSet set;
		boolean hidesHair;
		boolean hidesJaw;
		/** True for head/body/legs/shield pieces whose numbers come from their place in the chain. */
		boolean scaled;
		/** Combination weapons and boss drops: never part of a chain, never offered as a plain upgrade. */
		boolean unique;
		/** Seconds each hit freezes its target. */
		double freeze;

		Spec(String name, Slot slot, Style style, int tier)
		{
			this.name = name;
			this.slot = slot;
			this.style = style;
			this.tier = tier;
		}

		Spec targets(int n)
		{
			targets = n;
			return this;
		}

		Spec cleave(int n)
		{
			cleave = n;
			return this;
		}

		Spec volley(int n)
		{
			volley = n;
			return this;
		}

		Spec splash(double radius)
		{
			splash = radius;
			return this;
		}

		Spec lifesteal(double fraction)
		{
			lifesteal = fraction;
			return this;
		}

		Spec twoHanded()
		{
			twoHanded = true;
			return this;
		}

		Spec missile(Missile m)
		{
			missile = m;
			return this;
		}

		/** Weapons: damage relative to the tier's curve. Armour: a fixed share of the best of its kind. */
		Spec power(double p)
		{
			if (slot == Slot.WEAPON)
			{
				powerScale = p;
			}
			else
			{
				fixedPower = p;
			}
			return this;
		}

		Spec style(Style s)
		{
			style = s;
			return this;
		}

		Spec damage(double fraction)
		{
			damageBonus = fraction;
			return this;
		}

		Spec defence(double d)
		{
			extraDefence = d;
			return this;
		}

		Spec speed(double fraction)
		{
			speedBonus = fraction;
			return this;
		}

		Spec magnet(double units)
		{
			magnet = units;
			return this;
		}

		Spec reflect(double dps)
		{
			reflectDps = dps;
			return this;
		}

		Spec set(GearSet s)
		{
			set = s;
			return this;
		}

		Spec unique()
		{
			unique = true;
			return this;
		}

		Spec freeze(double seconds)
		{
			freeze = seconds;
			return this;
		}

		Spec hidesHair()
		{
			hidesHair = true;
			return this;
		}

		Spec hidesHairAndJaw()
		{
			hidesHair = true;
			hidesJaw = true;
			return this;
		}
	}

	/** @param ticks attack speed in game ticks, like the game's own weapons */
	static Spec weapon(String name, Style style, int tier, int ticks, double range)
	{
		Spec s = new Spec(name, Slot.WEAPON, style, tier);
		s.ticks = ticks;
		s.cooldown = ticks * TICK;
		s.range = range;
		return s;
	}

	/** Armour and shields: defence and damage grow with the piece's place in its chain. */
	static Spec armour(String name, Style style, Slot slot, int tier)
	{
		Spec s = new Spec(name, slot, style, tier);
		s.scaled = true;
		return s;
	}

	static Spec accessory(String name, Slot slot, int tier)
	{
		return new Spec(name, slot, null, tier);
	}

	/** Defence at the top of a chain, per piece. Melee armour protects most, magic least. */
	private static double topDefence(Style style, Slot slot)
	{
		double weight = slot == Slot.BODY ? 2 : slot == Slot.LEGS ? 1.5 : slot == Slot.SHIELD ? 1.2 : 1;
		double factor = style == Style.MELEE ? 27 : style == Style.RANGED ? 19 : style == Style.MAGIC ? 15 : 20;
		return weight * factor;
	}

	/** Damage bonus at the top of a chain, per piece. Magic and ranged armour add the most damage. */
	private static double topDamage(Style style, Slot slot)
	{
		double weight = slot == Slot.BODY ? 1.3 : slot == Slot.LEGS ? 1.1 : 1;
		double base = style == Style.MELEE ? 0.035 : style == Style.RANGED ? 0.07 : style == Style.MAGIC ? 0.09 : 0.05;
		return weight * base;
	}

	/** Highest tier in each (slot, style) chain, for scaling armour. */
	private static final class Chains
	{
		static final Map<Slot, Map<Style, Integer>> TOP = new EnumMap<>(Slot.class);

		static
		{
			for (Gear g : values())
			{
				if (g.spec.style != null && !g.spec.unique)
				{
					TOP.computeIfAbsent(g.spec.slot, k -> new EnumMap<>(Style.class)).merge(g.spec.style, g.spec.tier, Math::max);
				}
			}
		}

		private Chains()
		{
		}

		static int top(Slot slot, Style style)
		{
			Map<Style, Integer> m = TOP.get(slot);
			return m == null ? 1 : m.getOrDefault(style, 1);
		}
	}

	private final Spec spec;

	Gear(Spec spec)
	{
		this.spec = spec;
	}

	public String getTitle()
	{
		return spec.name;
	}

	public Slot getSlot()
	{
		return spec.slot;
	}

	/** Combat style for weapons, style armour, shields and style accessories; null for the rest. */
	public Style getStyle()
	{
		return spec.style;
	}

	public int getTier()
	{
		return spec.tier;
	}

	/** How far up its chain this piece is, from just above 0 to 1, comparable across styles and slots. */
	public double getPower()
	{
		if (spec.fixedPower >= 0)
		{
			return spec.fixedPower;
		}
		if (spec.slot == Slot.WEAPON)
		{
			return spec.tier / 10.0;
		}
		if (spec.style == null)
		{
			return spec.tier / 4.0;
		}
		return spec.tier / (double) Chains.top(spec.slot, spec.style);
	}

	/** Max hit per missile (per attack for most weapons), before bonuses. */
	public double getDamage()
	{
		if (spec.slot != Slot.WEAPON)
		{
			return 0;
		}
		// a volley shares one attack's damage between its missiles
		return Math.round(Balance.dps(spec.style, spec.tier) * spec.cooldown * spec.powerScale / spec.volley * 10) / 10.0;
	}

	/** Seconds between attacks at the start of a run, before any speed-ups. */
	public double getCooldown()
	{
		return spec.cooldown;
	}

	/** Attack speed in game ticks. */
	public int getTicks()
	{
		return spec.ticks;
	}

	/** Combination weapons and boss drops. */
	public boolean isUnique()
	{
		return spec.unique;
	}

	/** Seconds each hit freezes its target, 0 for none. */
	public double getFreeze()
	{
		return spec.freeze;
	}

	public double getRange()
	{
		return spec.range;
	}

	public int getTargets()
	{
		return spec.targets;
	}

	/** Extra enemies a melee swing hits beside its target. */
	public int getCleave()
	{
		return spec.cleave;
	}

	/** Missiles per attack. */
	public int getVolley()
	{
		return spec.volley;
	}

	/** Extra splash radius for magic (and chinchompas). */
	public double getSplash()
	{
		return spec.splash;
	}

	/** Share of damage dealt that heals the wearer. */
	public double getLifesteal()
	{
		return spec.lifesteal;
	}

	public boolean isTwoHanded()
	{
		return spec.twoHanded;
	}

	public Missile getMissile()
	{
		return spec.missile;
	}

	public double getDefence()
	{
		double d = spec.extraDefence;
		if (spec.scaled)
		{
			d += getPower() * topDefence(spec.style, spec.slot);
		}
		return d;
	}

	public double getDamageBonus()
	{
		double b = spec.damageBonus;
		if (spec.scaled)
		{
			b += getPower() * topDamage(spec.style, spec.slot) * (spec.slot == Slot.SHIELD ? 0.8 : 1);
		}
		return b;
	}

	public double getSpeedBonus()
	{
		return spec.speedBonus;
	}

	public double getMagnet()
	{
		return spec.magnet;
	}

	public double getReflectDps()
	{
		return spec.reflectDps;
	}

	/** The armour set this piece belongs to, or null. */
	public GearSet getSet()
	{
		return spec.set;
	}

	public boolean hidesHair()
	{
		return spec.hidesHair;
	}

	public boolean hidesJaw()
	{
		return spec.hidesJaw;
	}

	/** One line describing what the item does, for offer cards. */
	public String describe()
	{
		List<String> parts = new ArrayList<>();
		if (spec.slot == Slot.WEAPON)
		{
			parts.add(capitalised(spec.style.getLabel()) + ", max hit " + (int) Math.round(getDamage()));
			parts.add(spec.ticks + "-tick");
			if (spec.targets > 1)
			{
				parts.add("hits " + spec.targets);
			}
			if (spec.cleave > 0)
			{
				parts.add("cleaves +" + spec.cleave);
			}
			if (spec.volley > 1)
			{
				parts.add("fires " + spec.volley);
			}
			if (spec.splash > 0)
			{
				parts.add("splash +" + (int) spec.splash);
			}
			if (spec.lifesteal > 0)
			{
				parts.add("heals " + Math.round(spec.lifesteal * 100) + "% of damage");
			}
			if (spec.freeze > 0)
			{
				parts.add("freezes");
			}
			if (spec.twoHanded)
			{
				parts.add("2-handed");
			}
		}
		else
		{
			double defence = getDefence();
			double damage = getDamageBonus();
			if (defence > 0)
			{
				parts.add("+" + (int) Math.round(defence) + " defence");
			}
			if (damage > 0.001)
			{
				parts.add("+" + Math.round(damage * 100) + "% " + (spec.style == null ? "" : spec.style.getLabel() + " ") + "damage");
			}
			if (spec.speedBonus > 0)
			{
				parts.add("+" + Math.round(spec.speedBonus * 100) + "% speed");
			}
			if (spec.magnet > 0)
			{
				parts.add("+" + (int) spec.magnet + " pickup radius");
			}
			if (spec.reflectDps > 0)
			{
				parts.add("touching enemies take " + (int) spec.reflectDps + " damage/s");
			}
			if (spec.lifesteal > 0)
			{
				parts.add("heals " + Math.round(spec.lifesteal * 100) + "% of damage");
			}
		}
		if (spec.set != null)
		{
			parts.add(spec.set.getTitle() + " set");
		}
		return String.join(", ", parts);
	}

	private static String capitalised(String s)
	{
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	/** Highest tier of a style's chain for a slot (armour, shields); 1 if there is none. */
	public static int topTier(Slot slot, Style style)
	{
		return Chains.top(slot, style);
	}

	/** Items for a slot and tier wearable with a style: that style's items plus any-style items. */
	public static List<Gear> at(Slot slot, Style style, int tier)
	{
		List<Gear> found = new ArrayList<>();
		for (Gear g : values())
		{
			if (!g.spec.unique && g.spec.slot == slot && g.spec.tier == tier && (g.spec.style == null || g.spec.style == style))
			{
				found.add(g);
			}
		}
		return found;
	}

	/** The plain upgrade path of a chain: the first item of each tier, lowest first. */
	public static List<Gear> chain(Slot slot, Style style)
	{
		List<Gear> chain = new ArrayList<>();
		for (int tier = 1; ; tier++)
		{
			List<Gear> at = at(slot, style, tier);
			if (at.isEmpty())
			{
				break;
			}
			chain.add(plain(at));
		}
		return Collections.unmodifiableList(chain);
	}

	/** First item of the chain for a slot and style. */
	public static Gear first(Slot slot, Style style)
	{
		List<Gear> at = at(slot, style, 1);
		return at.isEmpty() ? null : plain(at);
	}

	/** The plain next item in this item's chain, or null at the top. */
	public Gear next()
	{
		List<Gear> at = at(spec.slot, spec.style, spec.tier + 1);
		return at.isEmpty() ? null : plain(at);
	}

	/** The first item that isn't part of a set, else the first. */
	private static Gear plain(List<Gear> items)
	{
		for (Gear g : items)
		{
			if (g.spec.set == null)
			{
				return g;
			}
		}
		return items.get(0);
	}
}
