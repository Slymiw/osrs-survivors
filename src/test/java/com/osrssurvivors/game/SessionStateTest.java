package com.osrssurvivors.game;

import static com.osrssurvivors.game.TestSessions.EPS;
import static com.osrssurvivors.game.TestSessions.quiet;
import static com.osrssurvivors.game.TestSessions.run;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Random;
import org.junit.Test;

/** Pause, game over and restart. */
public class SessionStateTest
{
	@Test
	public void pauseFreezesEverything()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		Enemy e = s.spawnEnemyAt(p.x + 400, p.y, 1e9);
		s.setMoveDirection(0, 1);
		run(s, 0.5);

		s.togglePause();
		assertEquals(Phase.PAUSED, s.getPhase());
		double elapsed = s.getElapsed();
		double px = p.x;
		double py = p.y;
		double ex = e.x;
		run(s, 3.0);
		assertEquals(elapsed, s.getElapsed(), 0);
		assertEquals(px, p.x, 0);
		assertEquals(py, p.y, 0);
		assertEquals(ex, e.x, 0);

		s.togglePause();
		assertEquals(Phase.RUNNING, s.getPhase());
		run(s, 0.5);
		assertEquals(elapsed + 0.5, s.getElapsed(), EPS);
		assertTrue(p.y > py);
	}

	@Test
	public void pausedTimeIsNotCaughtUpOnResume()
	{
		GameSession s = quiet();
		s.togglePause();
		s.advance(0.2);
		s.togglePause();
		s.advance(GameSession.STEP);
		assertEquals(GameSession.STEP, s.getElapsed(), EPS);
	}

	@Test
	public void autoPauseOnlyWhileRunning()
	{
		GameSession s = quiet();
		s.pause();
		assertEquals(Phase.PAUSED, s.getPhase());
		s.pause();
		assertEquals(Phase.PAUSED, s.getPhase());

		GameSession levelling = quiet();
		Player p = levelling.getPlayer();
		levelling.dropOrbAt(p.x, p.y, 100);
		levelling.advance(GameSession.STEP);
		levelling.pause();
		levelling.togglePause();
		assertEquals(Phase.LEVEL_UP, levelling.getPhase());
	}

	@Test
	public void elapsedTimeCountsRunningTimeOnly()
	{
		GameSession s = quiet();
		run(s, 2.0);
		assertEquals(2.0, s.getElapsed(), EPS);
	}

	@Test
	public void deathEndsTheRun()
	{
		GameSession s = overwhelmed();
		assertEquals(Phase.GAME_OVER, s.getPhase());
		assertTrue(s.getPlayer().isDead());
		assertEquals(0, s.getPlayer().getHealth(), 0);

		double elapsed = s.getElapsed();
		run(s, 2.0);
		assertEquals(elapsed, s.getElapsed(), 0);

		s.togglePause();
		assertEquals(Phase.GAME_OVER, s.getPhase());
		assertFalse(s.chooseUpgrade(0));
	}

	@Test
	public void restartResetsRun()
	{
		GameSession s = overwhelmed();
		s.setVisibleArea(400, 300);
		s.restart();

		assertEquals(Phase.RUNNING, s.getPhase());
		assertEquals(0, s.getElapsed(), 0);
		assertEquals(0, s.getKills());
		assertEquals(1, s.getProgression().getLevel());
		assertEquals(0, s.getProgression().getXp());
		assertEquals(Player.BASE_MAX_HEALTH, s.getPlayer().getHealth(), 0);
		assertTrue(s.getEnemies().isEmpty());
		assertTrue(s.getOrbs().isEmpty());
		assertTrue(s.getStrikes().isEmpty());
		assertEquals(Build.FIST_DAMAGE, s.getWeapon().getMaxHit(), 0);
		assertTrue(s.getBuild().getWorn().isEmpty());
		assertEquals((GameSession.OPEN_TILES_X * Arena.TILE) / 2, s.getPlayer().getX(), 0);
	}

	@Test
	public void unattendedRunEventuallyEnds()
	{
		// a full run with natural spawning: standing still, the player levels up a little and is overrun
		GameSession s = new GameSession(new Random(11));
		for (int i = 0; i < 60 * 60 * 10 && s.getPhase() != Phase.GAME_OVER; i++)
		{
			if (s.getPhase() == Phase.LEVEL_UP)
			{
				assertTrue(s.chooseUpgrade(0));
			}
			s.advance(GameSession.STEP);
		}
		assertEquals(Phase.GAME_OVER, s.getPhase());
		assertTrue(s.getKills() > 0);
		assertTrue(s.getProgression().getLevel() > 1);
	}

	private static GameSession overwhelmed()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		for (int i = 0; i < 12; i++)
		{
			double angle = i * Math.PI / 6;
			s.spawnEnemyAt(p.x + Math.cos(angle) * 30, p.y + Math.sin(angle) * 30, 1e9);
		}
		run(s, 5.0);
		return s;
	}

	@Test
	public void devToolsEndTheRunAndGrantLevels()
	{
		MetaProgress meta = new MetaProgress();
		GameSession s = new GameSession(new Random(1));
		s.setMeta(meta);
		s.disableSpawning();
		run(s, 30);
		s.togglePause();
		int level = s.getProgression().getLevel();
		s.devGrantLevels(5);
		assertEquals(level + 5, s.getProgression().getLevel());
		s.devEndRun();
		assertEquals(Phase.GAME_OVER, s.getPhase());
		assertTrue("the run paid out", meta.coins > 0);

		meta.maxOut();
		for (Perk p : Perk.values())
		{
			assertEquals(p.getMaxRank(), meta.rank(p));
		}
		assertEquals(Pet.values().length, meta.getPets().size());
		meta.reset();
		assertEquals(0, meta.coins);
		assertTrue(meta.getPets().isEmpty());
	}
}
