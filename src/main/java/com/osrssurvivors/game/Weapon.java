package com.osrssurvivors.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * The player's automatic attack, set up from the {@link Build}: fists, or whatever weapon is
 * held. Every cooldown it hits the nearest enemies in range for a roll between 60% and 100% of
 * the max hit, plus what the style does around each target: melee swings catch the enemies
 * beside it, ranged missiles pierce the ones behind it, magic splashes the ones around it.
 * Holds its fire (staying ready) while nothing is in range.
 */
public final class Weapon
{
	static final double MIN_ROLL = 0.6;
	/** Damage share for enemies caught beside, behind or around the main target. */
	static final double CLEAVE_SHARE = 0.6;
	static final double PIERCE_SHARE = 0.7;
	static final double SPLASH_SHARE = 0.5;
	/** Half-angle of a melee swing, radians. */
	static final double SWING = Math.toRadians(70);
	/** Extra width of a missile's path, beyond the enemy's own radius. */
	static final double PIERCE_WIDTH = 8;

	Gear gear;
	double damage = Build.FIST_DAMAGE;
	/** Seconds between attacks. */
	double cooldown = Build.FIST_COOLDOWN;
	double range = Build.FIST_RANGE;
	int targets = 1;
	int cleave;
	int pierce;
	double splash;
	int volley = 1;
	double doubleChance;
	boolean berserk;
	double multiplier = 1;
	/** Seconds each hit freezes its target. */
	double freeze;
	double championMultiplier = 1;
	/** Seconds between special attacks (double damage), or 0 for none. */
	double specialInterval;
	/** Total damage of the last attack, for lifesteal. */
	double lastDamage;
	private double timer;
	private double specialTimer;

	void configure(Build build)
	{
		gear = build.get(Slot.WEAPON);
		damage = build.weaponDamage();
		cooldown = build.attackCooldown();
		range = build.attackRange();
		targets = build.targets();
		cleave = build.cleave();
		pierce = build.pierce();
		splash = build.splash();
		volley = build.volley();
		doubleChance = build.doubleAttackChance();
		berserk = build.berserk();
		multiplier = build.damageMultiplier();
		freeze = build.freeze();
		championMultiplier = build.championMultiplier();
		if (specialInterval == 0)
		{
			specialTimer = build.specialInterval();
		}
		specialInterval = build.specialInterval();
	}

	/** The held weapon, or null for fists. */
	public Gear getGear()
	{
		return gear;
	}

	/** Highest hit, after bonuses. */
	public double getMaxHit()
	{
		return damage * multiplier;
	}

	public double getCooldown()
	{
		return cooldown;
	}

	public double getRange()
	{
		return range;
	}

	public int getTargets()
	{
		return targets;
	}

	public int getCleave()
	{
		return cleave;
	}

	public int getPierce()
	{
		return pierce;
	}

	public double getSplash()
	{
		return splash;
	}

	/** Max hit per second against one enemy, for comparing loadouts. */
	public double getDps()
	{
		return getMaxHit() * volley * (1 + doubleChance) * (1 + MIN_ROLL) / 2 / cooldown;
	}

	/**
	 * Advances the cooldown and attacks if ready. Each hit is recorded in {@code strikes} and
	 * {@code hitsplats}, splashes in {@code bursts}.
	 *
	 * @return the main targets hit, nearest first (empty if no attack)
	 */
	List<Enemy> update(double dt, Player player, List<Enemy> enemies, List<Strike> strikes, List<Effects.Hitsplat> hitsplats,
		List<Effects.Burst> bursts, Random random)
	{
		lastDamage = 0;
		if (specialInterval > 0)
		{
			specialTimer = Math.max(0, specialTimer - dt);
		}
		timer -= dt;
		if (timer > 0)
		{
			return new ArrayList<>();
		}
		timer = 0;

		double rangeSq = range * range;
		List<Enemy> inRange = new ArrayList<>();
		for (Enemy e : enemies)
		{
			if (!e.isDead() && distSq(player.x, player.y, e) <= rangeSq)
			{
				inRange.add(e);
			}
		}
		if (inRange.isEmpty())
		{
			return inRange;
		}

		inRange.sort(Comparator.comparingDouble(e -> distSq(player.x, player.y, e)));
		List<Enemy> primaries = new ArrayList<>(inRange.subList(0, Math.min(targets, inRange.size())));
		boolean special = specialInterval > 0 && specialTimer <= 0;
		double maxHit = getMaxHit() * (special ? 2 : 1);
		if (berserk)
		{
			// Dharok's: up to 60% more damage as hitpoints run out
			maxHit *= 1 + 0.6 * (1 - player.health / player.maxHealth);
		}
		int shots = volley + (random != null && doubleChance > 0 && random.nextDouble() < doubleChance ? 1 : 0);
		for (int shot = 0; shot < shots; shot++)
		{
			for (Enemy p : primaries)
			{
				hit(p, maxHit, special, hitsplats, random);
				strikes.add(new Strike(player.x, player.y, p.x, p.y, gear));
				if (cleave > 0)
				{
					for (Enemy e : beside(player, p, inRange, primaries))
					{
						hit(e, maxHit * CLEAVE_SHARE, special, hitsplats, random);
					}
				}
				if (pierce > 0)
				{
					for (Enemy e : behind(player, p, enemies, primaries))
					{
						hit(e, maxHit * PIERCE_SHARE, special, hitsplats, random);
					}
				}
				if (splash > 0)
				{
					double r2 = splash * splash;
					for (Enemy e : enemies)
					{
						if (e != p && !e.isDead() && distSq(p.x, p.y, e) <= r2)
						{
							hit(e, maxHit * SPLASH_SHARE, special, hitsplats, random);
						}
					}
					bursts.add(new Effects.Burst(gear != null && gear.getMissile() == Gear.Missile.CHINCHOMPA
						? Effects.Burst.Type.EXPLOSION : Effects.Burst.Type.SPLASH, p.x, p.y, splash));
				}
			}
		}
		if (special)
		{
			specialTimer = specialInterval;
		}
		timer = cooldown;
		return primaries;
	}

	private void hit(Enemy e, double maxHit, boolean special, List<Effects.Hitsplat> hitsplats, Random random)
	{
		double roll = random == null ? 1 : MIN_ROLL + (1 - MIN_ROLL) * random.nextDouble();
		double amount = maxHit * roll * (e.champion ? championMultiplier : 1);
		e.damage(amount);
		if (freeze > 0)
		{
			e.frozen = Math.max(e.frozen, freeze * (e.champion ? 0.3 : 1));
		}
		lastDamage += amount;
		hitsplats.add(new Effects.Hitsplat(e, (int) Math.round(amount), special));
	}

	/** Enemies in range within the swing's arc around the target, nearest first. */
	private List<Enemy> beside(Player player, Enemy target, List<Enemy> inRange, List<Enemy> primaries)
	{
		double aim = Math.atan2(target.y - player.y, target.x - player.x);
		List<Enemy> caught = new ArrayList<>();
		for (Enemy e : inRange)
		{
			if (caught.size() >= cleave)
			{
				break;
			}
			if (primaries.contains(e))
			{
				continue;
			}
			double a = Math.atan2(e.y - player.y, e.x - player.x) - aim;
			a = Math.atan2(Math.sin(a), Math.cos(a));
			if (Math.abs(a) <= SWING)
			{
				caught.add(e);
			}
		}
		return caught;
	}

	/** Enemies on the missile's line beyond the target (out to the weapon's range), nearest first. */
	private List<Enemy> behind(Player player, Enemy target, List<Enemy> enemies, List<Enemy> primaries)
	{
		double dx = target.x - player.x;
		double dy = target.y - player.y;
		double len = Math.hypot(dx, dy);
		if (len < 1e-6)
		{
			return new ArrayList<>();
		}
		double ux = dx / len;
		double uy = dy / len;
		List<Enemy> line = new ArrayList<>();
		List<Double> along = new ArrayList<>();
		for (Enemy e : enemies)
		{
			if (e.isDead() || primaries.contains(e))
			{
				continue;
			}
			double ex = e.x - player.x;
			double ey = e.y - player.y;
			double t = ex * ux + ey * uy;
			double off = Math.abs(ex * uy - ey * ux);
			if (t > len && t <= range && off <= e.radius + PIERCE_WIDTH)
			{
				int i = 0;
				while (i < along.size() && along.get(i) < t)
				{
					i++;
				}
				line.add(i, e);
				along.add(i, t);
			}
		}
		return line.size() > pierce ? new ArrayList<>(line.subList(0, pierce)) : line;
	}

	private static double distSq(double x, double y, Enemy e)
	{
		double dx = e.x - x;
		double dy = e.y - y;
		return dx * dx + dy * dy;
	}
}
