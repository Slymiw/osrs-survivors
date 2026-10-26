package com.osrssurvivors.game;

/** A spell or arrow fired by a monster, flying in a straight line; it hurts the player on contact. */
public final class EnemyShot
{
	static final double SPEED = 230;

	final EnemyKind.Attack type;
	double x;
	double y;
	final double vx;
	final double vy;
	final double damage;
	double remaining;

	EnemyShot(EnemyKind.Attack type, double x, double y, double targetX, double targetY, double damage, double range)
	{
		this.type = type;
		this.x = x;
		this.y = y;
		double d = Math.max(1, Math.hypot(targetX - x, targetY - y));
		this.vx = (targetX - x) / d * SPEED;
		this.vy = (targetY - y) / d * SPEED;
		this.damage = damage;
		this.remaining = range * 1.25 / SPEED;
	}

	public EnemyKind.Attack getType()
	{
		return type;
	}

	public double getX()
	{
		return x;
	}

	public double getY()
	{
		return y;
	}

	/** Direction of flight in radians. */
	public double getHeading()
	{
		return Math.atan2(vy, vx);
	}
}
