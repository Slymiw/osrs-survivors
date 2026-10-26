package com.osrssurvivors.game;

import static com.osrssurvivors.game.TestSessions.EPS;
import static com.osrssurvivors.game.TestSessions.dist;
import static com.osrssurvivors.game.TestSessions.quiet;
import static com.osrssurvivors.game.TestSessions.run;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class EnemyTest
{
	private static final double CONTACT = Enemy.RADIUS + Player.RADIUS;
	private static final double UNKILLABLE = 1e9;

	@Test
	public void pursuesPlayer()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		Enemy e = s.spawnEnemyAt(p.x + 300, p.y - 400, UNKILLABLE);
		double before = dist(p, e);
		run(s, 1.0);
		assertEquals(before - e.speed, dist(p, e), EPS);

		// heading straight at the player: still on the original line
		assertEquals(-4.0 / 3.0, (e.y - p.y) / (e.x - p.x), EPS);
	}

	@Test
	public void followsMovingPlayer()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		Enemy e = s.spawnEnemyAt(p.x - 300, p.y, UNKILLABLE);
		s.setMoveDirection(0, 1);
		run(s, 2.0);
		assertTrue("enemy turned to follow", e.y > (GameSession.OPEN_TILES_Y * Arena.TILE) / 2);
	}

	@Test
	public void stopsAtContactWithoutOvershooting()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		Enemy e = s.spawnEnemyAt(p.x + 100, p.y, UNKILLABLE);
		run(s, 5.0);
		assertEquals(CONTACT, dist(p, e), EPS);
		assertTrue(e.x > p.x);
	}

	@Test
	public void spawnsOffscreenInsideArena()
	{
		GameSession s = new GameSession(new Random(7));
		s.setVisibleArea(800, 480);
		run(s, 30);
		assertFalse(s.getEnemies().isEmpty());
		Arena arena = s.getArena();
		for (Enemy e : s.getEnemies())
		{
			assertTrue(e.x >= Enemy.RADIUS && e.x <= arena.getWidth() - Enemy.RADIUS);
			assertTrue(e.y >= Enemy.RADIUS && e.y <= arena.getHeight() - Enemy.RADIUS);
		}
	}

	@Test
	public void spawnsBeyondVisibleArea()
	{
		Player p = new Player(1200, 900);
		Arena arena = Arena.open(GameSession.OPEN_TILES_X, GameSession.OPEN_TILES_Y);
		Spawner spawner = new Spawner(new Random(3), Location.LUMBRIDGE);
		List<Enemy> enemies = new ArrayList<>();
		double distance = Math.hypot(800, 480) / 2 + Spawner.OFFSCREEN_MARGIN;
		for (int i = 0; i < 50; i++)
		{
			spawner.update(Spawner.START_INTERVAL, 0, p, arena, distance, enemies);
		}
		assertEquals(50, enemies.size());
		for (Enemy e : enemies)
		{
			assertEquals(distance, dist(p, e), EPS);
		}
	}

	@Test
	public void spawnsKeepDistanceInCorner()
	{
		Player p = new Player(Player.RADIUS, Player.RADIUS);
		Arena arena = Arena.open(GameSession.OPEN_TILES_X, GameSession.OPEN_TILES_Y);
		Spawner spawner = new Spawner(new Random(5), Location.LUMBRIDGE);
		List<Enemy> enemies = new ArrayList<>();
		for (int i = 0; i < 50; i++)
		{
			spawner.update(Spawner.START_INTERVAL, 0, p, arena, 500, enemies);
		}
		for (Enemy e : enemies)
		{
			assertTrue(dist(p, e) >= Spawner.MIN_SPAWN_DISTANCE);
		}
	}

	@Test
	public void spawningSpeedsUpAndIsCapped()
	{
		assertTrue(Spawner.interval(60) < Spawner.interval(0));
		assertEquals(Spawner.MIN_INTERVAL, Spawner.interval(10_000), EPS);
		assertTrue(Spawner.createEnemy(EnemyKind.RAT, 0, 0, 120, 0, Location.LUMBRIDGE, false).maxHealth > Spawner.createEnemy(EnemyKind.RAT, 0, 0, 0, 0, Location.LUMBRIDGE, false).maxHealth);

		Player p = new Player(1200, 900);
		Arena arena = Arena.open(GameSession.OPEN_TILES_X, GameSession.OPEN_TILES_Y);
		Spawner spawner = new Spawner(new Random(1), Location.LUMBRIDGE);
		List<Enemy> enemies = new ArrayList<>();
		for (int i = 0; i < 1000; i++)
		{
			spawner.update(1, 600, p, arena, 500, enemies);
		}
		assertEquals(Spawner.MAX_ENEMIES, enemies.size());
	}

	@Test
	public void championsAreBossesThatGrowWithTheRun()
	{
		Random random = new Random(1);
		assertTrue(Spawner.BOSSES.length >= 20);
		int early = Arrays.asList(Spawner.BOSSES).indexOf(Spawner.boss(1, 0, random));
		int late = Arrays.asList(Spawner.BOSSES).indexOf(Spawner.boss(5, 4, random));
		assertTrue(early < Spawner.BOSS_WINDOW);
		assertTrue(late > early);
		for (EnemyKind boss : Spawner.BOSSES)
		{
			assertTrue(boss.isBoss());
		}
		// a boss is as tough as the champion it replaces
		Enemy boss = Spawner.createEnemy(EnemyKind.VORKATH, 0, 0, 400, 0, Location.VARROCK, true);
		Enemy old = Spawner.createEnemy(Location.VARROCK.newest(400), 0, 0, 400, 0, Location.VARROCK, true);
		assertEquals(old.maxHealth, boss.maxHealth, 1e-9);
		assertEquals("Vorkath", boss.getTitle());
	}
}
