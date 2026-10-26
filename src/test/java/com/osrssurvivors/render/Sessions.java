package com.osrssurvivors.render;

import com.osrssurvivors.game.GameSession;
import com.osrssurvivors.game.Phase;
import java.util.Random;

/** Real sessions played to a given phase, for render and layout tests. */
final class Sessions
{
	private Sessions()
	{
	}

	static GameSession levelledUp()
	{
		return playUntil(Phase.LEVEL_UP);
	}

	static GameSession dead()
	{
		return playUntil(Phase.GAME_OVER);
	}

	/** Plays a seeded run without moving until it reaches {@code phase} (or 10 minutes pass). */
	static GameSession playUntil(Phase phase)
	{
		GameSession s = new GameSession(new Random(3));
		for (int i = 0; i < 60 * 60 * 10 && s.getPhase() != phase; i++)
		{
			if (s.getPhase() == Phase.LEVEL_UP)
			{
				s.chooseUpgrade(0);
			}
			s.advance(GameSession.STEP);
		}
		return s;
	}
}
