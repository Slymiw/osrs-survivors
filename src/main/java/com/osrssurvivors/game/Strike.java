package com.osrssurvivors.game;

/** A short-lived visual record of one hit, drawn as a projectile or a swing from the attacker to the enemy. */
public final class Strike
{
	public static final double DURATION = 0.25;

	/** Who made the attack. */
	public enum Source
	{
		PLAYER, CANNON, THRALL, CHINCHOMPA
	}

	public final double fromX;
	public final double fromY;
	public final double toX;
	public final double toY;
	/** The weapon used, or null for fists and abilities. */
	public final Gear weapon;
	public final Source source;
	double remaining = DURATION;

	Strike(double fromX, double fromY, double toX, double toY, Gear weapon)
	{
		this(fromX, fromY, toX, toY, weapon, Source.PLAYER);
	}

	Strike(double fromX, double fromY, double toX, double toY, Gear weapon, Source source)
	{
		this.fromX = fromX;
		this.fromY = fromY;
		this.toX = toX;
		this.toY = toY;
		this.weapon = weapon;
		this.source = source;
	}

	/** 1 when fresh, falling to 0 as it expires. */
	public double getStrength()
	{
		return Math.max(0, remaining / DURATION);
	}
}
