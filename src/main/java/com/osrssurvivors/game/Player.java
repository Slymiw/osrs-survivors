package com.osrssurvivors.game;

public final class Player
{
	public static final double RADIUS = 12;
	/** Half-size of the box used against walls; a little smaller than the body so doorways fit. */
	public static final double COLLISION_RADIUS = 9;
	/** How long an attack animation plays, in seconds. */
	public static final double ATTACK_ANIMATION = 0.6;
	public static final double BASE_SPEED = 160;
	public static final double BASE_MAX_HEALTH = 100;
	public static final double BASE_MAGNET_RADIUS = 70;
	/** Hitpoints restored per second without any bonuses. */
	public static final double BASE_REGEN = 0.25;

	double x;
	double y;
	double health = BASE_MAX_HEALTH;
	double maxHealth = BASE_MAX_HEALTH;
	/** World units per second. */
	double speed = BASE_SPEED;
	/** XP orbs inside this radius fly towards the player. */
	double magnetRadius = BASE_MAGNET_RADIUS;
	/** Last direction moved in radians (0 = east, PI/2 = south); starts facing the camera. */
	double facing = Math.PI / 2;
	boolean moving;
	/** Seconds since the last attack started. */
	double attackAge = Double.MAX_VALUE;

	Player(double x, double y)
	{
		this.x = x;
		this.y = y;
	}

	public double getX()
	{
		return x;
	}

	public double getY()
	{
		return y;
	}

	public double getHealth()
	{
		return health;
	}

	public double getMaxHealth()
	{
		return maxHealth;
	}

	public double getSpeed()
	{
		return speed;
	}

	public double getMagnetRadius()
	{
		return magnetRadius;
	}

	public double getFacing()
	{
		return facing;
	}

	public boolean isMoving()
	{
		return moving;
	}

	/** Seconds since the last attack started; large when not attacking. */
	public double getAttackAge()
	{
		return attackAge;
	}

	public boolean isDead()
	{
		return health <= 0;
	}

	void damage(double amount)
	{
		health = Math.max(0, health - amount);
	}

	void heal(double amount)
	{
		health = Math.min(maxHealth, health + amount);
	}
}
