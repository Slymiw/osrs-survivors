package com.osrssurvivors.game;

public final class Enemy
{
	/** Radius of the default (zombie-sized) enemy. */
	public static final double RADIUS = 10;
	/** How long an enemy flashes after being hit, in seconds. */
	static final double HIT_FLASH = 0.12;
	/** Ranged monsters stop at this share of their range to shoot. */
	static final double STAND_OFF = 0.8;
	/** Speed share at which shooters keep closing in. */
	static final double CREEP = 0.35;
	/** Marks a shooter in range, in the movement code. */
	static final double STAND_OFF_MARK = -1;

	final EnemyKind kind;
	final double radius;
	double x;
	double y;
	double health;
	final double maxHealth;
	/** World units per second. */
	final double speed;
	/** Damage per second dealt while touching the player. */
	final double contactDps;
	final int xpValue;
	/** Offsets this enemy's animation so a crowd doesn't walk in lockstep. */
	final double animOffset;
	/** A champion: a big, tough version that drops a casket. */
	final boolean champion;
	/** Damage per missile for ranged monsters, after scaling. */
	double missileDamage;
	double attackTimer;
	/** Seconds left frozen in place (Ice Barrage). */
	double frozen;
	double hitFlash;
	/** Direction of travel in radians (0 = east, PI/2 = south). */
	double facing = Math.PI / 2;
	boolean moving;
	/** Whether the player was in plain sight at the last check, and steps until the next check. */
	boolean inSight = true;
	int sightCheck;

	Enemy(double x, double y, double maxHealth, double speed, double contactDps, int xpValue)
	{
		this(EnemyKind.ZOMBIE, RADIUS, x, y, maxHealth, speed, contactDps, xpValue, 0, false);
	}

	Enemy(EnemyKind kind, double radius, double x, double y, double maxHealth, double speed, double contactDps,
		int xpValue, double animOffset, boolean champion)
	{
		this.kind = kind;
		this.radius = radius;
		this.x = x;
		this.y = y;
		this.health = maxHealth;
		this.maxHealth = maxHealth;
		this.speed = speed;
		this.contactDps = contactDps;
		this.xpValue = xpValue;
		this.animOffset = animOffset;
		this.champion = champion;
	}

	public EnemyKind getKind()
	{
		return kind;
	}

	public double getRadius()
	{
		return radius;
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

	public double getFacing()
	{
		return facing;
	}

	public boolean isMoving()
	{
		return moving;
	}

	public double getAnimOffset()
	{
		return animOffset;
	}

	public boolean isFlashing()
	{
		return hitFlash > 0;
	}

	public boolean isChampion()
	{
		return champion;
	}

	public boolean isFrozen()
	{
		return frozen > 0;
	}

	/** Size relative to a normal monster of its kind, for drawing. */
	public double getScale()
	{
		return radius / kind.radius;
	}

	public String getTitle()
	{
		return champion && !kind.boss ? kind.title + " champion" : kind.title;
	}

	public boolean isDead()
	{
		return health <= 0;
	}

	void damage(double amount)
	{
		health -= amount;
		hitFlash = HIT_FLASH;
	}

	/** Moves straight towards the target, stopping on contact instead of overshooting into it. */
	void pursue(double targetX, double targetY, double contactDistance, double dt)
	{
		pursue(targetX, targetY, contactDistance, dt, null);
	}

	/**
	 * Moves towards the target, stopping {@code contactDistance} short of it. With an arena,
	 * walls and blocked tiles stop the movement (sliding along them). Frozen enemies only turn.
	 */
	void pursue(double targetX, double targetY, double contactDistance, double dt, Arena arena)
	{
		if (hitFlash > 0)
		{
			hitFlash -= dt;
		}
		double dx = targetX - x;
		double dy = targetY - y;
		double dist = Math.sqrt(dx * dx + dy * dy);
		if (dist > 0)
		{
			facing = Math.atan2(dy, dx);
		}
		if (frozen > 0)
		{
			frozen -= dt;
			moving = false;
			return;
		}
		double travel = Math.min(speed * dt, dist - contactDistance);
		moving = travel > 0;
		if (moving)
		{
			double mx = dx / dist * travel;
			double my = dy / dist * travel;
			if (arena == null || arena.isOpen())
			{
				x += mx;
				y += my;
			}
			else
			{
				double r = collisionRadius();
				x = arena.moveX(x, y, mx, r);
				y = arena.moveY(x, y, my, r);
			}
		}
	}

	/** Half-size of the box used against walls. */
	double collisionRadius()
	{
		return Math.min(radius * 0.7, 14);
	}

	boolean touches(Player player)
	{
		double dx = player.x - x;
		double dy = player.y - y;
		// tiny tolerance so an enemy that stopped exactly at contact distance still counts
		double reach = radius + Player.RADIUS + 0.5;
		return dx * dx + dy * dy <= reach * reach;
	}
}
