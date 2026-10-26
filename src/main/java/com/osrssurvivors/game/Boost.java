package com.osrssurvivors.game;

/**
 * Potions offered once the upgrades run out, so the last levels of a long run still give a real
 * choice. Each dose lasts the whole run and they stack, some only up to a limit. Append new ones
 * at the end (saves store names).
 */
public enum Boost
{
	DIVINE_SUPER_COMBAT("Divine super combat potion", Style.MELEE, "+4% melee damage", 0),
	DIVINE_RANGING("Divine ranging potion", Style.RANGED, "+4% ranged damage", 0),
	DIVINE_MAGIC("Divine magic potion", Style.MAGIC, "+4% magic damage", 0),
	SARADOMIN_BREW("Saradomin brew", null, "+15 max hitpoints, and heal 15", 0),
	STAMINA("Stamina potion", null, "+3% movement speed", 5),
	SUPER_RESTORE("Super restore", null, "Regenerate 0.3 hitpoints/s", 0),
	ANTIFIRE("Antifire potion", null, "Take 3% less damage", 5);

	/** Damage share per dose of the style's potion. */
	static final double DAMAGE = 0.04;
	static final double HITPOINTS = 15;
	static final double SPEED = 0.03;
	static final double REGEN = 0.3;
	static final double REDUCTION = 0.03;

	private final String title;
	private final Style style;
	private final String effect;
	/** Most doses in a run, or 0 for no limit. */
	private final int maxDoses;

	Boost(String title, Style style, String effect, int maxDoses)
	{
		this.title = title;
		this.style = style;
		this.effect = effect;
		this.maxDoses = maxDoses;
	}

	public String getTitle()
	{
		return title;
	}

	/** The only style it helps, or null for any. */
	public Style getStyle()
	{
		return style;
	}

	public String getEffect()
	{
		return effect;
	}

	/** Whether another dose can be offered, having had {@code doses}, fighting in {@code style}. */
	boolean offerable(int doses, Style fighting)
	{
		return (style == null || style == fighting) && (maxDoses == 0 || doses < maxDoses);
	}
}
