package com.osrssurvivors.game;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Spawns the location's monsters just outside the visible area around the player, faster and
 * tougher as the run goes on, plus a champion every few minutes: one of the game's bosses, more
 * famous the harder the place and the longer the run. On a real map they only appear on
 * reachable, open ground.
 */
final class Spawner
{
	static final int MAX_ENEMIES = 60;
	static final double START_INTERVAL = 2.4;
	static final double MIN_INTERVAL = 0.8;
	/** Interval reduction per second survived. */
	static final double INTERVAL_DECAY = 0.0018;
	/** Seconds survived per extra monster in each spawn. */
	static final double BATCH_GROWTH = 270;
	/**
	 * Monster health relative to the original curve. There are fewer monsters on screen than
	 * there used to be, so each one is tougher and worth more XP.
	 */
	static final double HEALTH_SCALE = 2.8;
	static final double XP_SCALE = 2.4;
	/** When overtime begins, and how fast monsters toughen after it (e-folding time). */
	static final double OVERTIME_START = 900;
	static final double OVERTIME_GROWTH = 200;
	/** Spawn distance beyond the edge of the visible area. */
	static final double OFFSCREEN_MARGIN = 60;
	/** Spawns never land closer than this, even when the arena edge forces them inward. */
	static final double MIN_SPAWN_DISTANCE = 250;
	/** When the first champion arrives, and how often after that. */
	static final double FIRST_CHAMPION = 180;
	static final double CHAMPION_INTERVAL = 180;
	static final double CHAMPION_HEALTH = 12;
	static final double CHAMPION_SIZE = 1.5;
	/** Overall monster damage, relative to the kinds' listed numbers. */
	static final double CONTACT_SCALE = 0.8;
	private static final int PLACEMENT_ATTEMPTS = 24;
	/** Bosses, weakest first; champions are picked from a window that moves along it. */
	static final EnemyKind[] BOSSES = bosses();
	/** How far along the list each champion of a run, and each difficulty step, moves the window. */
	static final int BOSS_STEP = 2;
	static final int BOSS_WINDOW = 3;

	private final Random random;
	private final Location location;
	private double timer;
	private double nextChampion = FIRST_CHAMPION;

	Spawner(Random random, Location location)
	{
		this.random = random;
		this.location = location;
	}

	static double interval(double elapsed)
	{
		return Math.max(MIN_INTERVAL, START_INTERVAL - elapsed * INTERVAL_DECAY);
	}

	/** One extra enemy per spawn for every three minutes survived. */
	static int batchSize(double elapsed)
	{
		return 1 + (int) (elapsed / BATCH_GROWTH);
	}

	/** Base health before the kind's scale: grows steadily, then faster, to keep up with late-game gear. */
	static double baseHealth(double elapsed)
	{
		return HEALTH_SCALE * (8 + elapsed * 0.08 + elapsed * elapsed * 0.0003) * overtime(elapsed);
	}

	/**
	 * Past {@link #OVERTIME_START} monsters toughen fast, doubling every few minutes, so even a
	 * build that has outgrown everything meets its end (like Vampire Survivors' reaper).
	 */
	static double overtime(double elapsed)
	{
		return elapsed <= OVERTIME_START ? 1 : Math.exp((elapsed - OVERTIME_START) / OVERTIME_GROWTH);
	}

	/** Damage multiplier for monster missiles: they hit harder the longer the run goes. */
	static double damageScale(double elapsed)
	{
		return 1 + elapsed / 900;
	}

	private static EnemyKind[] bosses()
	{
		return Arrays.stream(EnemyKind.values()).filter(EnemyKind::isBoss).toArray(EnemyKind[]::new);
	}

	/** The boss for a run's {@code index}th champion (0-based) at a difficulty. */
	static EnemyKind boss(int difficulty, int index, Random random)
	{
		int start = Math.min(BOSSES.length - BOSS_WINDOW, BOSS_STEP * (Math.max(1, difficulty) - 1 + index));
		return BOSSES[start + random.nextInt(BOSS_WINDOW)];
	}

	static Enemy createEnemy(EnemyKind kind, double x, double y, double elapsed, double animOffset, Location location,
		boolean champion)
	{
		// a boss is as tough as a champion of the strongest monster around would be
		EnemyKind stats = kind.boss ? location.newest(elapsed) : kind;
		double health = baseHealth(elapsed) * stats.healthScale * location.healthMultiplier(elapsed) * (champion ? CHAMPION_HEALTH : 1);
		double speed = (70 + Math.min(elapsed * 0.3, 50)) * stats.speedScale * (champion ? 0.75 : 1);
		double damage = CONTACT_SCALE * location.damageMultiplier(elapsed) * (champion ? 1.5 : 1) * Math.sqrt(overtime(elapsed));
		Enemy e = new Enemy(kind, kind.radius * (champion ? CHAMPION_SIZE : 1), x, y, health, speed, stats.contactDps * damage,
			(int) Math.round(stats.xp * XP_SCALE * (champion ? 10 : 1)), animOffset, champion);
		// a boss's missiles hit about as hard as a ranged champion of that monster's strength
		e.missileDamage = kind.boss ? kind.attackDamage * stats.contactDps * 0.5 * damage : kind.attackDamage * damage;
		e.attackTimer = kind.attackCooldown * (0.5 + animOffset / 20);
		return e;
	}

	/** Seconds until the next champion. */
	double untilChampion(double elapsed)
	{
		return nextChampion - elapsed;
	}

	void update(double dt, double elapsed, Player player, Arena arena, double spawnDistance, List<Enemy> enemies)
	{
		if (elapsed >= nextChampion)
		{
			int index = (int) Math.round((nextChampion - FIRST_CHAMPION) / CHAMPION_INTERVAL);
			Enemy champion = placeEnemy(boss(location.getDifficulty(), index, random), player, arena, spawnDistance, elapsed, true);
			if (champion != null)
			{
				enemies.add(champion);
				nextChampion += CHAMPION_INTERVAL;
			}
		}

		timer -= dt;
		if (timer > 0)
		{
			return;
		}
		timer += interval(elapsed);

		for (int n = batchSize(elapsed); n > 0 && enemies.size() < MAX_ENEMIES; n--)
		{
			Enemy e = placeEnemy(location.choose(elapsed, random), player, arena, spawnDistance, elapsed, false);
			if (e != null)
			{
				enemies.add(e);
			}
		}
	}

	/** Brings the next champion forward to now (a developer tool). */
	void summonNow(double elapsed)
	{
		nextChampion = Math.min(nextChampion, elapsed);
	}

	/** Continues the champion schedule from a saved run's time. */
	void resume(double elapsed)
	{
		nextChampion = FIRST_CHAMPION;
		while (nextChampion <= elapsed)
		{
			nextChampion += CHAMPION_INTERVAL;
		}
	}

	/** Returns null if no open spot was found this time. */
	private Enemy placeEnemy(EnemyKind kind, Player player, Arena arena, double spawnDistance, double elapsed, boolean champion)
	{
		double radius = kind.radius * (champion ? CHAMPION_SIZE : 1);
		for (int i = 0; i < PLACEMENT_ATTEMPTS; i++)
		{
			double angle = random.nextDouble() * Math.PI * 2;
			// on a real map, also try a little nearer and further out
			double dist = arena.isOpen() ? spawnDistance : spawnDistance * (0.8 + 0.5 * random.nextDouble());
			double x = clamp(player.x + Math.cos(angle) * dist, radius, arena.getWidth() - radius);
			double y = clamp(player.y + Math.sin(angle) * dist, radius, arena.getHeight() - radius);
			double dx = x - player.x;
			double dy = y - player.y;
			boolean farEnough = dx * dx + dy * dy >= MIN_SPAWN_DISTANCE * MIN_SPAWN_DISTANCE;
			if (arena.isOpen())
			{
				if (farEnough || i == PLACEMENT_ATTEMPTS - 1)
				{
					return createEnemy(kind, x, y, elapsed, random.nextDouble() * 10, location, champion);
				}
				continue;
			}
			if (farEnough && arena.isWalkable(Arena.tileOf(x), Arena.tileOf(y)) && !arena.isSolidBox(x, y, Math.min(radius * 0.7, 14)))
			{
				return createEnemy(kind, x, y, elapsed, random.nextDouble() * 10, location, champion);
			}
		}
		return null;
	}

	private static double clamp(double v, double min, double max)
	{
		return v < min ? min : (v > max ? max : v);
	}
}
