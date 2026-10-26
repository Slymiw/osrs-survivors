package com.osrssurvivors.game;

import static com.osrssurvivors.game.TestSessions.EPS;
import static com.osrssurvivors.game.TestSessions.quiet;
import static com.osrssurvivors.game.TestSessions.run;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

/** Hitsplats, corpses, XP drops, facing and enemy kinds. */
public class EffectsTest
{
	@Test
	public void hitsShowDamageHitsplats()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		Enemy e = s.spawnEnemyAt(p.x + 30, p.y, 1e9);
		s.advance(GameSession.STEP);
		assertEquals(1, s.getHitsplats().size());
		Effects.Hitsplat h = s.getHitsplats().get(0);
		assertSame(e, h.getTarget());
		assertTrue(h.getDamage() >= Math.floor(Build.FIST_DAMAGE * Weapon.MIN_ROLL));
		assertTrue(h.getDamage() <= Build.FIST_DAMAGE);

		run(s, Effects.Hitsplat.DURATION / 2);
		assertTrue(h.getAge() > 0.4 && h.getAge() < 0.6);
	}

	@Test
	public void effectsExpire()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		s.spawnEnemyAt(p.x + 30, p.y, 1);
		run(s, 0.5);
		assertEquals(1, s.getKills());
		assertEquals("gem collected", 1, s.getProgression().getXp());
		assertFalse(s.getXpDrops().isEmpty());

		run(s, 2);
		assertTrue(s.getHitsplats().isEmpty());
		assertTrue(s.getCorpses().isEmpty());
		assertTrue(s.getXpDrops().isEmpty());
		assertTrue(s.getStrikes().isEmpty());
	}

	@Test
	public void killsLeaveAFadingCorpse()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		Enemy e = s.spawnEnemyAt(p.x + 30, p.y, 1);
		s.advance(GameSession.STEP);
		assertEquals(1, s.getCorpses().size());
		Effects.Corpse c = s.getCorpses().get(0);
		assertEquals(e.x, c.getX(), EPS);
		assertEquals(e.getKind(), c.getKind());
		double before = c.getStrength();
		run(s, 0.2);
		assertTrue(c.getStrength() < before);
	}

	@Test
	public void restartClearsEffects()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		s.spawnEnemyAt(p.x + 30, p.y, 1);
		run(s, 0.3);
		s.restart();
		assertTrue(s.getHitsplats().isEmpty());
		assertTrue(s.getCorpses().isEmpty());
		assertTrue(s.getXpDrops().isEmpty());
	}

	@Test
	public void charactersFaceWhereTheyGo()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		assertEquals("starts facing the camera", Math.PI / 2, p.getFacing(), EPS);
		s.setMoveDirection(-1, 0);
		s.advance(GameSession.STEP);
		assertEquals(Math.PI, p.getFacing(), EPS);
		assertTrue(p.isMoving());
		s.setMoveDirection(0, 0);
		s.advance(GameSession.STEP);
		assertFalse(p.isMoving());
		assertEquals("keeps last facing", Math.PI, p.getFacing(), EPS);

		Enemy e = s.spawnEnemyAt(p.x, p.y - 400, 1e9);
		s.advance(GameSession.STEP);
		assertEquals("walks south towards the player", Math.PI / 2, e.getFacing(), EPS);
		assertTrue(e.isMoving());
	}

	@Test
	public void tougherKindsJoinOverTime()
	{
		Random r = new Random(9);
		Set<EnemyKind> early = EnumSet.noneOf(EnemyKind.class);
		for (int i = 0; i < 500; i++)
		{
			early.add(Location.LUMBRIDGE.choose(10, r));
		}
		assertEquals(EnumSet.of(EnemyKind.CHICKEN, EnemyKind.RAT), early);
		Set<EnemyKind> late = EnumSet.noneOf(EnemyKind.class);
		for (int i = 0; i < 4000; i++)
		{
			late.add(Location.LUMBRIDGE.choose(900, r));
		}
		assertEquals(EnumSet.copyOf(Location.LUMBRIDGE.getKinds()), late);
	}

	@Test
	public void kindsChangeStats()
	{
		Enemy rat = Spawner.createEnemy(EnemyKind.RAT, 0, 0, 0, 0, Location.LUMBRIDGE, false);
		Enemy giant = Spawner.createEnemy(EnemyKind.MOSS_GIANT, 0, 0, 0, 0, Location.LUMBRIDGE, false);
		assertTrue(giant.maxHealth > rat.maxHealth * 5);
		assertTrue(rat.speed > giant.speed);
		assertTrue(giant.radius > rat.radius);
		assertTrue(giant.xpValue > rat.xpValue);
	}

	@Test
	public void bigEnemiesStopFurtherAway()
	{
		Player player = new Player(0, 0);
		Enemy giant = Spawner.createEnemy(EnemyKind.MOSS_GIANT, 200, 0, 0, 0, Location.LUMBRIDGE, false);
		for (int i = 0; i < 600; i++)
		{
			giant.pursue(player.x, player.y, giant.radius + Player.RADIUS, GameSession.STEP);
		}
		assertEquals(EnemyKind.MOSS_GIANT.radius + Player.RADIUS, giant.x, EPS);
		assertTrue(giant.touches(player));
	}

	@Test
	public void crowdsSpreadOutInsteadOfStacking()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		for (int i = 0; i < 20; i++)
		{
			s.spawnEnemyAt(p.x + 300, p.y, 1e9);
		}
		run(s, 6);
		List<Enemy> enemies = s.getEnemies();
		int touching = 0;
		for (int i = 0; i < enemies.size(); i++)
		{
			touching += enemies.get(i).touches(p) ? 1 : 0;
			for (int j = i + 1; j < enemies.size(); j++)
			{
				double d = Math.hypot(enemies.get(i).x - enemies.get(j).x, enemies.get(i).y - enemies.get(j).y);
				assertTrue("enemies never stack: " + d, d > Enemy.RADIUS * 0.6);
			}
		}
		assertTrue("only a ring of them reach the player: " + touching, touching > 0 && touching < 20);
	}
}
