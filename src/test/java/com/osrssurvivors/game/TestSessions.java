package com.osrssurvivors.game;

import java.util.Random;

final class TestSessions
{
	static final double EPS = 1e-6;

	private TestSessions()
	{
	}

	/** A deterministic session with no natural spawning, so tests control the board. */
	static GameSession quiet()
	{
		GameSession s = new GameSession(new Random(42));
		s.disableSpawning();
		return s;
	}

	/** Advances {@code seconds} of game time in frames of {@code frame} seconds. */
	static void run(GameSession s, double seconds, double frame)
	{
		int frames = (int) Math.round(seconds / frame);
		for (int i = 0; i < frames; i++)
		{
			s.advance(frame);
		}
	}

	static void run(GameSession s, double seconds)
	{
		run(s, seconds, GameSession.STEP);
	}

	static double dist(Player p, Enemy e)
	{
		return Math.hypot(e.x - p.x, e.y - p.y);
	}
}
