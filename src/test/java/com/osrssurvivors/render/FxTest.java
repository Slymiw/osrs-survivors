package com.osrssurvivors.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.osrssurvivors.game.Cue;
import com.osrssurvivors.game.GameSession;
import com.osrssurvivors.game.Option;
import com.osrssurvivors.game.Phase;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;

/** Screen effects stay within their limits however busy the run gets. */
public class FxTest
{
	/** Plays a real run for a while, gathering its cues. */
	private static List<Cue> play(GameSession s, Fx fx, int steps)
	{
		List<Cue> all = new ArrayList<>();
		for (int i = 0; i < steps && s.getPhase() != Phase.GAME_OVER; i++)
		{
			if (s.getPhase() == Phase.LEVEL_UP)
			{
				s.chooseUpgrade(0);
			}
			s.advance(GameSession.STEP);
			List<Cue> cues = s.takeCues();
			all.addAll(cues);
			fx.handle(cues, s);
			fx.update(GameSession.STEP, s);
			assertTrue(fx.particleCount() <= Fx.MAX_PARTICLES);
		}
		return all;
	}

	@Test
	public void aLongRunStaysWithinTheParticleBudget()
	{
		GameSession s = new GameSession(new Random(11));
		Fx fx = new Fx();
		List<Cue> cues = play(s, fx, 60 * 90);
		assertTrue("the run made plenty to show", cues.size() > 100);
		// the same cues a hundred times over in one frame still fit
		List<Cue> flood = new ArrayList<>();
		for (int i = 0; i < 100; i++)
		{
			flood.addAll(cues);
		}
		s.restart();
		fx.handle(flood, s);
		assertTrue(fx.particleCount() <= Fx.MAX_PARTICLES);
		assertTrue("at most two notices", fx.getToasts().size() <= 2);
		BufferedImage img = new BufferedImage(640, 360, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		GameRenderer.render(g, new Rectangle(0, 0, 640, 360), s, GameAssets.NONE, null, "Q", -1, fx, false);
		g.dispose();
		fx.clear();
		assertEquals(0, fx.particleCount());
	}

	@Test
	public void theShakeRespectsTheOption()
	{
		GameSession s = new GameSession(new Random(1));
		Fx fx = new Fx();
		List<Cue> big = new ArrayList<>();
		for (Cue c : play(s, fx, 60 * 20))
		{
			if (c.type == Cue.Type.PLAYER_HURT || c.type == Cue.Type.KILL)
			{
				big.add(c);
			}
		}
		s.getOptions().turnOff(Option.SCREEN_SHAKE);
		s.restart();
		fx.handle(big, s);
		for (int i = 0; i < 10; i++)
		{
			fx.update(GameSession.STEP, s);
			assertEquals(0, fx.getShakeX(), 0);
			assertEquals(0, fx.getShakeY(), 0);
		}
	}

	@Test
	public void noticesFadeAway()
	{
		GameSession s = new GameSession(new Random(1));
		Fx fx = new Fx();
		fx.toast("One", "line", Color.WHITE);
		fx.toast("Two", "line", Color.WHITE);
		fx.toast("Three", "line", Color.WHITE);
		assertEquals(2, fx.getToasts().size());
		assertEquals("Two", fx.getToasts().get(0).title);
		for (int i = 0; i < 400; i++)
		{
			fx.update(GameSession.STEP, s);
		}
		assertTrue(fx.getToasts().isEmpty());
	}
}
