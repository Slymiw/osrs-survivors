package com.osrssurvivors.game;

/**
 * Potions: offered once the upgrades run out, they go in the bag with four doses, and each dose
 * drunk gives its effect for {@link #DOSE_SECONDS}. Drinking another dose while one works starts
 * the time again instead of stacking. Append new ones at the end (saves store names).
 */
public enum Boost
{
	DIVINE_SUPER_COMBAT("Divine super combat potion", Style.MELEE, "+15% melee damage"),
	DIVINE_RANGING("Divine ranging potion", Style.RANGED, "+15% ranged damage"),
	DIVINE_MAGIC("Divine magic potion", Style.MAGIC, "+15% magic damage"),
	SARADOMIN_BREW("Saradomin brew", null, "+20% max hitpoints, and heal 20%"),
	STAMINA("Stamina potion", null, "+20% movement speed"),
	SUPER_RESTORE("Super restore", null, "Regenerate 2 hitpoints/s"),
	ANTIFIRE("Super antifire potion", null, "Take 15% less damage"),
	SMELLING_SALTS("Smelling salts", null, "+10% damage with any style"),
	LIQUID_ADRENALINE("Liquid adrenaline", null, "Attack 10% more often"),
	SUPER_DEFENCE("Super defence potion", null, "+40 defence");

	/** Seconds one dose lasts. */
	public static final double DOSE_SECONDS = 60;
	/** Doses in a potion from an offer. */
	public static final int DOSES = 4;
	/** Damage share of the style's potion. */
	static final double DAMAGE = 0.15;
	/** Damage share of smelling salts, for any style. */
	static final double ANY_DAMAGE = 0.10;
	/** Share of max hitpoints a brew adds while it works, and heals each dose. */
	static final double HITPOINTS = 0.2;
	static final double SPEED = 0.2;
	static final double REGEN = 2;
	static final double REDUCTION = 0.15;
	/** Attack time multiplier of liquid adrenaline. */
	static final double HASTE = 0.9;
	static final double DEFENCE = 40;

	private final String title;
	private final Style style;
	private final String effect;

	Boost(String title, Style style, String effect)
	{
		this.title = title;
		this.style = style;
		this.effect = effect;
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
}
