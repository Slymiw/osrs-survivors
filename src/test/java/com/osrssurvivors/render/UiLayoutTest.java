package com.osrssurvivors.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.osrssurvivors.game.GameSession;
import com.osrssurvivors.game.Option;
import com.osrssurvivors.game.Volume;
import com.osrssurvivors.game.Phase;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class UiLayoutTest
{
	private static final Rectangle FIXED = new Rectangle(4, 4, 512, 334);
	private static final Rectangle LARGE = new Rectangle(0, 0, 1920, 1000);

	@Test
	public void noButtonsWhilePlaying()
	{
		GameSession s = new GameSession(new Random(1));
		assertNull(UiLayout.panel(FIXED, s));
		assertTrue(UiLayout.buttons(FIXED, s).isEmpty());
		assertNull(UiLayout.buttonAt(FIXED, s, new Point(200, 200)));
	}

	@Test
	public void levelUpHasOneCardPerChoiceInsideThePanel()
	{
		for (Rectangle view : new Rectangle[]{FIXED, LARGE})
		{
			GameSession s = Sessions.levelledUp();
			List<UiLayout.Button> buttons = UiLayout.buttons(view, s);
			assertEquals(3, buttons.size());
			Rectangle panel = UiLayout.panel(view, s);
			assertTrue(view.contains(panel));
			for (int i = 0; i < buttons.size(); i++)
			{
				UiLayout.Button b = buttons.get(i);
				assertEquals(UiLayout.Action.CHOOSE_UPGRADE, b.action);
				assertEquals(i, b.index);
				assertTrue("card inside panel at " + view, panel.contains(b.bounds));
				if (i > 0)
				{
					assertFalse("cards don't overlap", buttons.get(i - 1).bounds.intersects(b.bounds));
				}
			}
		}
	}

	@Test
	public void clickingACardFindsThatUpgrade()
	{
		GameSession s = Sessions.levelledUp();
		UiLayout.Button second = UiLayout.buttons(FIXED, s).get(1);
		Point centre = new Point((int) second.bounds.getCenterX(), (int) second.bounds.getCenterY());
		UiLayout.Button hit = UiLayout.buttonAt(FIXED, s, centre);
		assertEquals(1, hit.index);

		assertTrue(s.chooseUpgrade(hit.index));
		assertEquals(Phase.RUNNING, s.getPhase());
		assertNull("buttons are gone once resumed", UiLayout.buttonAt(FIXED, s, centre));
	}

	@Test
	public void pauseAndDeathButtons()
	{
		GameSession paused = new GameSession(new Random(1));
		paused.togglePause();
		List<UiLayout.Button> buttons = UiLayout.buttons(FIXED, paused);
		assertEquals(UiLayout.Action.RESUME, buttons.get(0).action);
		assertEquals(UiLayout.Action.OPTIONS, buttons.get(1).action);
		assertEquals(UiLayout.Action.EXIT, buttons.get(2).action);
		for (UiLayout.Button b : buttons)
		{
			assertTrue(b.label, UiLayout.panel(FIXED, paused).contains(b.bounds));
		}

		GameSession dead = Sessions.dead();
		assertEquals(Phase.GAME_OVER, dead.getPhase());
		buttons = UiLayout.buttons(FIXED, dead);
		assertEquals(UiLayout.Action.RESTART, buttons.get(0).action);
		assertEquals(UiLayout.Action.EXIT, buttons.get(1).action);
		assertTrue(UiLayout.panel(FIXED, dead).contains(buttons.get(1).bounds));
	}

	@Test
	public void optionsPageListsEverySettingAndCloses()
	{
		for (boolean title : new boolean[]{true, false})
		{
			GameSession s = new GameSession(new Random(1));
			if (title)
			{
				s.showTitle(null);
				assertTrue("the main menu has no exit any more",
					UiLayout.buttons(FIXED, s).stream().noneMatch(b -> b.action == UiLayout.Action.EXIT));
			}
			else
			{
				s.togglePause();
			}
			s.openOptions();
			assertTrue(s.isOptionsOpen());
			List<UiLayout.Button> buttons = UiLayout.buttons(FIXED, s);
			// each volume has a minus button, its bar and a plus button; then report a bug and back
			assertEquals(Option.values().length + 3 * Volume.values().length + 2, buttons.size());
			assertEquals(UiLayout.Action.REPORT_BUG, buttons.get(buttons.size() - 2).action);
			Rectangle panel = UiLayout.panel(FIXED, s);
			for (UiLayout.Button b : buttons)
			{
				assertTrue(b.label, panel.contains(b.bounds));
			}
			assertEquals(UiLayout.Action.CLOSE_OPTIONS, buttons.get(buttons.size() - 1).action);
			// Esc leaves the options page, not the pause
			s.togglePause();
			assertFalse(s.isOptionsOpen());
			assertEquals(title ? Phase.TITLE : Phase.PAUSED, s.getPhase());
		}
	}

	@Test
	public void missesBetweenButtons()
	{
		GameSession s = Sessions.levelledUp();
		Rectangle panel = UiLayout.panel(FIXED, s);
		// the title strip of the panel is not a button
		assertNull(UiLayout.buttonAt(FIXED, s, new Point(panel.x + panel.width / 2, panel.y + 5)));
		assertNull(UiLayout.buttonAt(FIXED, s, null));
	}
}
