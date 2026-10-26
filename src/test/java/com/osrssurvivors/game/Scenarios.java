package com.osrssurvivors.game;

import java.util.Random;

/** Sessions set up in states that are slow to reach by playing, for tests outside this package. */
public final class Scenarios
{
	private Scenarios()
	{
	}

	/** A run with a champion's casket just opened: its drop on the reels. */
	public static GameSession bossDrop(Random random)
	{
		GameSession s = new GameSession(random);
		s.disableSpawning();
		Player p = s.getPlayer();
		s.dropCasketAt(p.x, p.y);
		s.advance(GameSession.STEP);
		if (s.getPhase() != Phase.BOSS_DROP)
		{
			throw new IllegalStateException("casket didn't open");
		}
		return s;
	}

	/** Lets a boss drop's reels run for {@code seconds}. */
	public static void spin(GameSession s, double seconds)
	{
		for (double t = 0; t < seconds; t += 1 / 30.0)
		{
			s.advance(1 / 30.0);
		}
	}
}
