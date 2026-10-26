package com.osrssurvivors.game;

import static com.osrssurvivors.game.TestSessions.EPS;
import static com.osrssurvivors.game.TestSessions.quiet;
import static com.osrssurvivors.game.TestSessions.run;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MovementTest
{
	private static final double ONE_STEP = Player.BASE_SPEED * GameSession.STEP;

	@Test
	public void movesAtSpeedPerSecond()
	{
		GameSession s = quiet();
		double startX = s.getPlayer().getX();
		double startY = s.getPlayer().getY();
		s.setMoveDirection(1, 0);
		run(s, 1.0);
		assertEquals(startX + Player.BASE_SPEED, s.getPlayer().getX(), EPS);
		assertEquals(startY, s.getPlayer().getY(), EPS);
	}

	@Test
	public void movementIsFrameRateIndependent()
	{
		double[] frameRates = {30, 60, 144, 240};
		double expected = Double.NaN;
		for (double fps : frameRates)
		{
			GameSession s = quiet();
			s.setMoveDirection(0, -1);
			run(s, 2.0, 1 / fps);
			double travelled = s.getArena().getStartY() - s.getPlayer().getY();
			assertEquals("at " + fps + " fps", 2 * Player.BASE_SPEED, travelled, ONE_STEP + EPS);
			if (Double.isNaN(expected))
			{
				expected = travelled;
			}
			assertEquals("same result at " + fps + " fps", expected, travelled, ONE_STEP + EPS);
		}
	}

	@Test
	public void diagonalIsNotFaster()
	{
		GameSession s = quiet();
		double startX = s.getPlayer().getX();
		double startY = s.getPlayer().getY();
		s.setMoveDirection(1, 1);
		run(s, 1.0);
		double travelled = Math.hypot(s.getPlayer().getX() - startX, s.getPlayer().getY() - startY);
		assertEquals(Player.BASE_SPEED, travelled, EPS);
	}

	@Test
	public void stopsWhenInputReleased()
	{
		GameSession s = quiet();
		s.setMoveDirection(-1, 0);
		run(s, 0.5);
		s.setMoveDirection(0, 0);
		double x = s.getPlayer().getX();
		run(s, 1.0);
		assertEquals(x, s.getPlayer().getX(), EPS);
	}

	@Test
	public void staysInsideArena()
	{
		GameSession s = quiet();
		s.setMoveDirection(-1, -1);
		run(s, 30);
		assertEquals(Player.COLLISION_RADIUS, s.getPlayer().getX(), EPS);
		assertEquals(Player.COLLISION_RADIUS, s.getPlayer().getY(), EPS);

		s.setMoveDirection(1, 1);
		run(s, 30);
		assertEquals(GameSession.OPEN_TILES_X * Arena.TILE - Player.COLLISION_RADIUS, s.getPlayer().getX(), EPS);
		assertEquals(GameSession.OPEN_TILES_Y * Arena.TILE - Player.COLLISION_RADIUS, s.getPlayer().getY(), EPS);
	}

	@Test
	public void longFrameIsClamped()
	{
		GameSession s = quiet();
		double startX = s.getPlayer().getX();
		s.setMoveDirection(1, 0);
		s.advance(5.0);
		assertEquals(startX + Player.BASE_SPEED * GameSession.MAX_FRAME, s.getPlayer().getX(), ONE_STEP + EPS);
		assertEquals(GameSession.MAX_FRAME, s.getElapsed(), GameSession.STEP + EPS);
	}

	@Test
	public void ignoresBadFrameTimes()
	{
		GameSession s = quiet();
		s.setMoveDirection(1, 0);
		double x = s.getPlayer().getX();
		s.advance(0);
		s.advance(-1);
		s.advance(Double.NaN);
		assertEquals(x, s.getPlayer().getX(), 0);
		assertEquals(0, s.getElapsed(), 0);
	}
}
