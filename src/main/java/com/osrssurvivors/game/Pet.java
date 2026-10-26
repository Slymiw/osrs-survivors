package com.osrssurvivors.game;

/**
 * Boss pets: the jackpot of a champion's casket. Once won, a pet is kept for good and follows
 * you in every run with its small bonus, like the game's collection of boss pets.
 */
public enum Pet
{
	PET_CHAOS_ELEMENTAL("Pet chaos elemental", "+5% damage"),
	BABY_MOLE("Baby mole", "+6% movement speed"),
	PRINCE_BLACK_DRAGON("Prince black dragon", "+12 defence"),
	PET_DARK_CORE("Pet dark core", "+0.3 hitpoints per second"),
	TZREK_JAD("Tzrek-jad", "Attack 4% faster"),
	LIL_ZIK("Lil' zik", "+8% XP"),
	OLMLET("Olmlet", "+10% coins from runs"),
	VORKI("Vorki", "+40 pickup radius"),
	// raid pets: Chambers of Xeric, Tombs of Amascut and the Fractured Archive
	TEKTINY("Tektiny", "+3% damage"),
	VANGUARD("Vanguard", "+8 defence"),
	VASA_MINIRIO("Vasa Minirio", "+0.2 hitpoints per second"),
	VESPINA("Vespina", "+4% movement speed"),
	TUMEKENS_GUARDIAN("Tumeken's guardian", "+5% XP"),
	AKKHITO("Akkhito", "Attack 2% faster"),
	BABI("Babi", "+10 max hitpoints"),
	KEPHRITI("Kephriti", "+20 pickup radius"),
	ORION("Orion", "+10% damage to champions"),
	XARI("Xari", "+5% coins from runs"),
	CHUBBLES("Chubbles", "+1 hitpoint per kill"),
	MIMI("Mimi", "+3% damage"),
	DORK("Dork", "+6 defence");

	private final String title;
	private final String effect;

	Pet(String title, String effect)
	{
		this.title = title;
		this.effect = effect;
	}

	public String getTitle()
	{
		return title;
	}

	public String getEffect()
	{
		return effect;
	}
}
