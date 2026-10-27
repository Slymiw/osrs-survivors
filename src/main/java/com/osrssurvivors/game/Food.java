package com.osrssurvivors.game;

/**
 * Food kept in the bag and eaten from it. Eating takes the game's three ticks before the next
 * bite, except karambwans, which can be eaten straight after other food (the game's combo eat).
 * Append new ones at the end (saves store names).
 */
public enum Food
{
	SHARK("Shark", 0.3, false),
	MANTA_RAY("Manta ray", 0.35, false),
	ANGLERFISH("Anglerfish", 0.45, false),
	COOKED_KARAMBWAN("Cooked karambwan", 0.2, true);

	/** Seconds between bites: three ticks. */
	public static final double EAT_DELAY = 3 * Gear.TICK;

	private final String title;
	private final double heal;
	private final boolean combo;

	Food(String title, double heal, boolean combo)
	{
		this.title = title;
		this.heal = heal;
		this.combo = combo;
	}

	public String getTitle()
	{
		return title;
	}

	/** Share of max hitpoints one bite heals. */
	public double getHeal()
	{
		return heal;
	}

	/** Whether it has its own eating delay, so it can follow other food at once. */
	public boolean isCombo()
	{
		return combo;
	}

	public String getEffect()
	{
		return "Heals " + Math.round(heal * 100) + "% of your hitpoints" + (combo ? ", eaten straight after other food" : "");
	}
}
