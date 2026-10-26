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
	VORKI("Vorki", "+40 pickup radius");

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
