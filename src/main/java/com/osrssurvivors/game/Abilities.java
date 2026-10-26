package com.osrssurvivors.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Attacks that happen on their own once the matching passive is picked: a dwarf multicannon set
 * up where you stand, thralls fighting beside you, Ice and Blood Barrage on the thickest clumps
 * of monsters, thrown chinchompas, god spells on scattered monsters and a dragonfire shield's
 * breath ahead of you. Their damage follows the player's max hit, so they stay useful with any
 * style and at any stage of a run.
 */
public final class Abilities
{
	static final double CANNON_INTERVAL = 20;
	static final double CANNON_RANGE = 230;
	static final double CANNON_FIRE_RATE = 0.3;
	static final double THRALL_RANGE = 120;
	static final double THRALL_COOLDOWN = 1.2;
	static final double THRALL_ORBIT = 34;
	static final double BARRAGE_REACH = 260;
	static final double CHINCHOMPA_RANGE = 240;
	static final double CHINCHOMPA_RADIUS = 44;
	static final double GOD_SPELL_RANGE = 280;
	static final double GOD_SPELL_RADIUS = 20;
	static final double DRAGONFIRE_RANGE = 170;
	/** Half-angle of the dragonfire cone, radians. */
	static final double DRAGONFIRE_CONE = Math.toRadians(40);
	/** Share of Blood Barrage damage that heals. */
	static final double BLOOD_HEAL = 0.15;

	/** A cannon set up on the ground for a while. */
	public static final class Cannon
	{
		final double x;
		final double y;
		double remaining;
		double fireTimer;
		/** Barrel direction, radians. */
		double aim;

		Cannon(double x, double y, double lifetime)
		{
			this.x = x;
			this.y = y;
			this.remaining = lifetime;
		}

		public double getX()
		{
			return x;
		}

		public double getY()
		{
			return y;
		}

		public double getAim()
		{
			return aim;
		}
	}

	/** A summoned thrall circling the player. */
	public static final class Thrall
	{
		double x;
		double y;
		double cooldown;
		double facing = Math.PI / 2;
		boolean attacking;

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
	}

	private final List<Cannon> cannons = new ArrayList<>();
	private final List<Thrall> thralls = new ArrayList<>();
	private double cannonTimer = 2;
	private double barrageTimer = 2;
	private double chinchompaTimer = 1;
	private double bloodTimer = 3;
	private double godTimer = 1.5;
	private double dragonfireTimer = 2;
	private double orbit;
	private double championMultiplier = 1;

	public List<Cannon> getCannons()
	{
		return Collections.unmodifiableList(cannons);
	}

	public List<Thrall> getThralls()
	{
		return Collections.unmodifiableList(thralls);
	}

	void clear()
	{
		cannons.clear();
		thralls.clear();
		cannonTimer = 2;
		barrageTimer = 2;
		chinchompaTimer = 1;
		bloodTimer = 3;
		godTimer = 1.5;
		dragonfireTimer = 2;
	}

	/**
	 * @param power the player's current max hit
	 * @return total damage dealt this step
	 */
	double update(double dt, Build build, Player player, double power, List<Enemy> enemies, List<Strike> strikes,
		List<Effects.Hitsplat> hitsplats, List<Effects.Burst> bursts, Random random)
	{
		championMultiplier = build.championMultiplier();
		double dealt = 0;
		dealt += cannon(dt, build.rank(Passive.CANNON), player, power, enemies, strikes, hitsplats);
		dealt += thralls(dt, build.rank(Passive.THRALLS), player, power, enemies, strikes, hitsplats);
		dealt += barrage(dt, build.rank(Passive.ICE_BARRAGE), player, power, enemies, hitsplats, bursts);
		dealt += chinchompas(dt, build.rank(Passive.CHINCHOMPAS), player, power, enemies, strikes, hitsplats, bursts, random);
		dealt += bloodBarrage(dt, build.rank(Passive.BLOOD_BARRAGE), player, power, enemies, hitsplats, bursts);
		dealt += godSpells(dt, build.rank(Passive.GOD_SPELLS), player, power, enemies, hitsplats, bursts, random);
		dealt += dragonfire(dt, build.rank(Passive.DRAGONFIRE), player, power, enemies, hitsplats, bursts);
		return dealt;
	}

	private double cannon(double dt, int rank, Player player, double power, List<Enemy> enemies, List<Strike> strikes,
		List<Effects.Hitsplat> hitsplats)
	{
		double dealt = 0;
		if (rank > 0)
		{
			cannonTimer -= dt;
			if (cannonTimer <= 0)
			{
				cannonTimer = CANNON_INTERVAL - 2 * (rank - 1);
				cannons.add(new Cannon(player.x, player.y, 8 + 2 * rank));
			}
		}
		for (Cannon c : cannons)
		{
			c.remaining -= dt;
			c.fireTimer -= dt;
			if (c.fireTimer > 0)
			{
				continue;
			}
			Enemy target = nearest(enemies, c.x, c.y, CANNON_RANGE);
			if (target == null)
			{
				continue;
			}
			c.fireTimer = CANNON_FIRE_RATE;
			c.aim = Math.atan2(target.y - c.y, target.x - c.x);
			dealt += hit(target, power * 0.45 * (1 + 0.15 * (rank - 1)), hitsplats);
			strikes.add(new Strike(c.x, c.y, target.x, target.y, null, Strike.Source.CANNON));
		}
		cannons.removeIf(c -> c.remaining <= 0);
		return dealt;
	}

	private double thralls(double dt, int rank, Player player, double power, List<Enemy> enemies, List<Strike> strikes,
		List<Effects.Hitsplat> hitsplats)
	{
		while (thralls.size() < rank)
		{
			Thrall t = new Thrall();
			t.x = player.x;
			t.y = player.y;
			thralls.add(t);
		}
		orbit += dt * 1.1;
		double dealt = 0;
		for (int i = 0; i < thralls.size(); i++)
		{
			Thrall t = thralls.get(i);
			double angle = orbit + i * 2 * Math.PI / thralls.size();
			double tx = player.x + Math.cos(angle) * THRALL_ORBIT;
			double ty = player.y + Math.sin(angle) * THRALL_ORBIT;
			// follow quickly, without snapping
			double mx = tx - t.x;
			double my = ty - t.y;
			t.x += mx * Math.min(1, dt * 8);
			t.y += my * Math.min(1, dt * 8);
			if (Math.hypot(mx, my) > 1)
			{
				t.facing = Math.atan2(my, mx);
			}
			t.cooldown -= dt;
			t.attacking = false;
			if (t.cooldown > 0)
			{
				continue;
			}
			Enemy target = nearest(enemies, t.x, t.y, THRALL_RANGE);
			if (target == null)
			{
				continue;
			}
			t.cooldown = THRALL_COOLDOWN;
			t.attacking = true;
			t.facing = Math.atan2(target.y - t.y, target.x - t.x);
			dealt += hit(target, power * 0.7, hitsplats);
			strikes.add(new Strike(t.x, t.y, target.x, target.y, null, Strike.Source.THRALL));
		}
		return dealt;
	}

	private double barrage(double dt, int rank, Player player, double power, List<Enemy> enemies, List<Effects.Hitsplat> hitsplats,
		List<Effects.Burst> bursts)
	{
		if (rank == 0)
		{
			return 0;
		}
		barrageTimer -= dt;
		if (barrageTimer > 0)
		{
			return 0;
		}
		double radius = 56 + 8 * rank;
		Enemy best = thickestClump(enemies, player, radius);
		if (best == null)
		{
			return 0;
		}
		barrageTimer = 7 - 0.75 * (rank - 1);
		double damage = power * 1.5 * (1 + 0.2 * (rank - 1));
		double dealt = 0;
		for (Enemy e : enemies)
		{
			if (!e.isDead() && dist2(e, best.x, best.y) <= radius * radius)
			{
				dealt += hit(e, damage, hitsplats);
				e.frozen = Math.max(e.frozen, 1.5 + 0.25 * rank) * (e.champion ? 0.3 : 1);
			}
		}
		bursts.add(new Effects.Burst(Effects.Burst.Type.ICE, best.x, best.y, radius));
		return dealt;
	}

	private double chinchompas(double dt, int rank, Player player, double power, List<Enemy> enemies, List<Strike> strikes,
		List<Effects.Hitsplat> hitsplats, List<Effects.Burst> bursts, Random random)
	{
		if (rank == 0)
		{
			return 0;
		}
		chinchompaTimer -= dt;
		if (chinchompaTimer > 0)
		{
			return 0;
		}
		List<Enemy> near = new ArrayList<>();
		for (Enemy e : enemies)
		{
			if (!e.isDead() && dist2(e, player.x, player.y) <= CHINCHOMPA_RANGE * CHINCHOMPA_RANGE)
			{
				near.add(e);
			}
		}
		if (near.isEmpty())
		{
			return 0;
		}
		chinchompaTimer = 3.2 - 0.4 * (rank - 1);
		Enemy target = near.get(random == null ? 0 : random.nextInt(near.size()));
		double damage = power * 1.1 * (1 + 0.2 * (rank - 1));
		double dealt = 0;
		for (Enemy e : enemies)
		{
			if (!e.isDead() && dist2(e, target.x, target.y) <= CHINCHOMPA_RADIUS * CHINCHOMPA_RADIUS)
			{
				dealt += hit(e, damage, hitsplats);
			}
		}
		strikes.add(new Strike(player.x, player.y, target.x, target.y, null, Strike.Source.CHINCHOMPA));
		bursts.add(new Effects.Burst(Effects.Burst.Type.EXPLOSION, target.x, target.y, CHINCHOMPA_RADIUS));
		return dealt;
	}

	private double bloodBarrage(double dt, int rank, Player player, double power, List<Enemy> enemies,
		List<Effects.Hitsplat> hitsplats, List<Effects.Burst> bursts)
	{
		if (rank == 0)
		{
			return 0;
		}
		bloodTimer -= dt;
		if (bloodTimer > 0)
		{
			return 0;
		}
		double radius = 50 + 8 * rank;
		Enemy best = thickestClump(enemies, player, radius);
		if (best == null)
		{
			return 0;
		}
		bloodTimer = 8 - 0.75 * (rank - 1);
		double damage = power * 1.3 * (1 + 0.2 * (rank - 1));
		double dealt = 0;
		for (Enemy e : enemies)
		{
			if (!e.isDead() && dist2(e, best.x, best.y) <= radius * radius)
			{
				dealt += hit(e, damage, hitsplats);
			}
		}
		player.heal(dealt * BLOOD_HEAL);
		bursts.add(new Effects.Burst(Effects.Burst.Type.BLOOD, best.x, best.y, radius));
		return dealt;
	}

	/** Strikes scattered monsters in range, two plus one per rank. */
	private double godSpells(double dt, int rank, Player player, double power, List<Enemy> enemies,
		List<Effects.Hitsplat> hitsplats, List<Effects.Burst> bursts, Random random)
	{
		if (rank == 0)
		{
			return 0;
		}
		godTimer -= dt;
		if (godTimer > 0)
		{
			return 0;
		}
		List<Enemy> near = new ArrayList<>();
		for (Enemy e : enemies)
		{
			if (!e.isDead() && dist2(e, player.x, player.y) <= GOD_SPELL_RANGE * GOD_SPELL_RANGE)
			{
				near.add(e);
			}
		}
		if (near.isEmpty())
		{
			return 0;
		}
		godTimer = 3;
		if (random != null)
		{
			Collections.shuffle(near, random);
		}
		double damage = power * 1.2 * (1 + 0.15 * (rank - 1));
		double dealt = 0;
		for (int i = 0; i < Math.min(near.size(), 1 + rank); i++)
		{
			Enemy e = near.get(i);
			dealt += hit(e, damage, hitsplats);
			bursts.add(new Effects.Burst(Effects.Burst.Type.GOD, e.x, e.y, GOD_SPELL_RADIUS));
		}
		return dealt;
	}

	/** A cone of fire towards the nearest monster. */
	private double dragonfire(double dt, int rank, Player player, double power, List<Enemy> enemies,
		List<Effects.Hitsplat> hitsplats, List<Effects.Burst> bursts)
	{
		if (rank == 0)
		{
			return 0;
		}
		dragonfireTimer -= dt;
		if (dragonfireTimer > 0)
		{
			return 0;
		}
		Enemy target = nearest(enemies, player.x, player.y, DRAGONFIRE_RANGE);
		if (target == null)
		{
			return 0;
		}
		dragonfireTimer = 9 - 1.5 * (rank - 1);
		double aim = Math.atan2(target.y - player.y, target.x - player.x);
		double damage = power * 2 * (1 + 0.25 * (rank - 1));
		double dealt = 0;
		for (Enemy e : enemies)
		{
			if (e.isDead() || dist2(e, player.x, player.y) > DRAGONFIRE_RANGE * DRAGONFIRE_RANGE)
			{
				continue;
			}
			double a = Math.atan2(e.y - player.y, e.x - player.x) - aim;
			if (Math.abs(Math.atan2(Math.sin(a), Math.cos(a))) <= DRAGONFIRE_CONE)
			{
				dealt += hit(e, damage, hitsplats);
			}
		}
		// flames along the cone
		for (int i = 1; i <= 3; i++)
		{
			double d = DRAGONFIRE_RANGE * i / 3.5;
			bursts.add(new Effects.Burst(Effects.Burst.Type.EXPLOSION, player.x + Math.cos(aim) * d, player.y + Math.sin(aim) * d,
				14 + 12 * i));
		}
		return dealt;
	}

	/** The monster in reach with the most others around it, or null. */
	private static Enemy thickestClump(List<Enemy> enemies, Player player, double radius)
	{
		Enemy best = null;
		int bestCount = 0;
		double reach = BARRAGE_REACH * BARRAGE_REACH;
		for (Enemy e : enemies)
		{
			if (e.isDead() || dist2(e, player.x, player.y) > reach)
			{
				continue;
			}
			int count = 0;
			for (Enemy o : enemies)
			{
				count += dist2(o, e.x, e.y) <= radius * radius ? 1 : 0;
			}
			if (count > bestCount)
			{
				bestCount = count;
				best = e;
			}
		}
		return best;
	}

	private double hit(Enemy e, double damage, List<Effects.Hitsplat> hitsplats)
	{
		double amount = damage * (e.champion ? championMultiplier : 1);
		e.damage(amount);
		hitsplats.add(new Effects.Hitsplat(e, (int) Math.round(amount), false));
		return amount;
	}

	private static Enemy nearest(List<Enemy> enemies, double x, double y, double range)
	{
		Enemy best = null;
		double bestD = range * range;
		for (Enemy e : enemies)
		{
			double d = dist2(e, x, y);
			if (!e.isDead() && d <= bestD)
			{
				bestD = d;
				best = e;
			}
		}
		return best;
	}

	private static double dist2(Enemy e, double x, double y)
	{
		double dx = e.x - x;
		double dy = e.y - y;
		return dx * dx + dy * dy;
	}
}
