package com.osrssurvivors.game;

/**
 * Short-lived presentation records produced by the simulation (hitsplats, corpses, XP drops).
 * They carry no gameplay weight; renderers read them and the session ages them out.
 */
public final class Effects
{
	private Effects()
	{
	}

	/** A damage number shown on an enemy (or the player), following it while it lives. */
	public static final class Hitsplat
	{
		public static final double DURATION = 0.9;

		/** The enemy hit, or null when the player took the damage. */
		final Enemy target;
		final int damage;
		/** Shown in the special-attack colour. */
		final boolean special;
		double remaining = DURATION;

		Hitsplat(Enemy target, int damage, boolean special)
		{
			this.target = target;
			this.damage = damage;
			this.special = special;
		}

		public boolean isSpecial()
		{
			return special;
		}

		/** The enemy hit, or null for the player. */
		public Enemy getTarget()
		{
			return target;
		}

		public int getDamage()
		{
			return damage;
		}

		/** 0 when fresh, rising to 1 as it expires. */
		public double getAge()
		{
			return 1 - remaining / DURATION;
		}
	}

	/** A killed enemy fading out where it fell. */
	public static final class Corpse
	{
		public static final double DURATION = 0.5;

		final EnemyKind kind;
		final double x;
		final double y;
		final double facing;
		final double radius;
		double remaining = DURATION;

		Corpse(Enemy e)
		{
			kind = e.kind;
			x = e.x;
			y = e.y;
			facing = e.facing;
			radius = e.radius;
		}

		public EnemyKind getKind()
		{
			return kind;
		}

		public double getX()
		{
			return x;
		}

		public double getY()
		{
			return y;
		}

		public double getFacing()
		{
			return facing;
		}

		public double getRadius()
		{
			return radius;
		}

		/** 1 when fresh, falling to 0 as it expires. */
		public double getStrength()
		{
			return Math.max(0, remaining / DURATION);
		}
	}

	/** XP gained from one pickup, floated up the screen like the game's XP drops. */
	public static final class XpDrop
	{
		public static final double DURATION = 1.5;

		final int amount;
		double remaining = DURATION;

		XpDrop(int amount)
		{
			this.amount = amount;
		}

		public int getAmount()
		{
			return amount;
		}

		/** 0 when fresh, rising to 1 as it expires. */
		public double getAge()
		{
			return 1 - remaining / DURATION;
		}
	}

	/** An area effect flashing on the ground: a magic splash, a chinchompa blast, a barrage, a god spell. */
	public static final class Burst
	{
		public static final double DURATION = 0.4;

		public enum Type
		{
			SPLASH, EXPLOSION, ICE, BLOOD, GOD
		}

		final Type type;
		final double x;
		final double y;
		final double radius;
		double remaining = DURATION;

		Burst(Type type, double x, double y, double radius)
		{
			this.type = type;
			this.x = x;
			this.y = y;
			this.radius = radius;
		}

		public Type getType()
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

		public double getRadius()
		{
			return radius;
		}

		/** 0 when fresh, rising to 1 as it expires. */
		public double getAge()
		{
			return 1 - remaining / DURATION;
		}
	}
}
