package com.osrssurvivors.game;

/**
 * A short-lived visual record of one hit, drawn as a projectile or a swing from the attacker to the
 * enemy. A missile takes {@link #flight} seconds to arrive and then flashes on impact; the hit's
 * hitsplat and the enemy's flash wait for it (the damage itself is dealt straight away).
 */
public final class Strike
{
	/** Seconds a melee swing shows. */
	public static final double DURATION = 0.25;
	/** Seconds the impact flash shows after a missile arrives. */
	public static final double IMPACT = 0.15;
	/** World units per second for arrows, bolts and spells; lobbed things are slower. */
	static final double MISSILE_SPEED = 900;
	static final double LOB_SPEED = 520;
	static final double MIN_FLIGHT = 0.06;
	static final double MAX_FLIGHT = 0.3;

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
	/** Seconds the missile flies; 0 for a swing. */
	public final double flight;
	double age;
	double remaining;

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
		this.flight = flightFor(weapon, source, Math.hypot(toX - fromX, toY - fromY));
		this.remaining = flight > 0 ? flight + IMPACT : DURATION;
	}

	/** Seconds a missile from this weapon or ability takes to fly {@code distance}; 0 for melee. */
	static double flightFor(Gear weapon, Source source, double distance)
	{
		boolean lobbed = source == Source.CANNON || source == Source.CHINCHOMPA
			|| weapon != null && weapon.getMissile() == Gear.Missile.CHINCHOMPA;
		boolean missile = lobbed || source == Source.THRALL
			|| weapon != null && weapon.getMissile() != Gear.Missile.NONE;
		if (!missile)
		{
			return 0;
		}
		double t = distance / (lobbed ? LOB_SPEED : MISSILE_SPEED);
		return Math.max(MIN_FLIGHT, Math.min(MAX_FLIGHT, t));
	}

	/** Counts down; returns true once it's over. */
	boolean tick(double dt)
	{
		age += dt;
		return (remaining -= dt) <= 0;
	}

	/** 1 when fresh, falling to 0 as it expires. */
	public double getStrength()
	{
		double total = flight > 0 ? flight + IMPACT : DURATION;
		return Math.max(0, remaining / total);
	}

	/** How far the missile has come (0..1), or how far through the swing. */
	public double getProgress()
	{
		return Math.min(1, flight > 0 ? age / flight : age / DURATION);
	}

	/** For a missile that has landed, 0..1 through the impact flash; -1 before it lands or for a swing. */
	public double getImpactAge()
	{
		if (flight <= 0 || age < flight)
		{
			return -1;
		}
		return Math.min(1, (age - flight) / IMPACT);
	}
}
