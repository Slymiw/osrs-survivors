package com.osrssurvivors.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.osrssurvivors.game.GameSession;
import com.osrssurvivors.game.Inventory;
import com.osrssurvivors.game.MetaProgress;
import com.osrssurvivors.game.Phase;
import com.osrssurvivors.game.Scenarios;
import com.osrssurvivors.game.TitleScreen;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;

/** The screen laid out for fingers, on phones and tablets held either way. */
public class TouchLayoutTest
{
	/** Phones upright (with and without the browser's bars), on their side (likewise), a small phone, a tablet. */
	private static final Rectangle[] PHONES = {
		new Rectangle(0, 0, 390, 844),
		new Rectangle(0, 0, 390, 660),
		new Rectangle(0, 0, 844, 390),
		new Rectangle(0, 0, 844, 340),
		new Rectangle(0, 0, 667, 320),
		new Rectangle(0, 0, 1024, 768),
	};
	/** Smallest side of anything a finger presses. */
	private static final int FINGER = 34;

	private static List<GameSession> screens()
	{
		List<GameSession> screens = new ArrayList<>();
		for (TitleScreen t : TitleScreen.values())
		{
			GameSession title = new GameSession(new Random(3));
			title.setMeta(new MetaProgress());
			title.showTitle("Lumbridge, level 4, 2:10");
			title.showTitleScreen(t);
			screens.add(title);
		}
		GameSession options = new GameSession(new Random(3));
		options.showTitle(null);
		options.setFullscreen(true, false);
		options.openOptions();
		screens.add(options);
		screens.add(Sessions.levelledUp());
		for (boolean bag : new boolean[]{true, false})
		{
			GameSession paused = new GameSession(new Random(3));
			paused.advance(1);
			paused.togglePause();
			paused.setBagShown(bag);
			screens.add(paused);
		}
		screens.add(Sessions.dead());
		screens.add(Scenarios.bossDrop(new Random(3)));
		GameSession running = new GameSession(new Random(3));
		running.advance(1);
		screens.add(running);
		for (GameSession s : screens)
		{
			s.setTouch(true);
		}
		return screens;
	}

	private static String describe(GameSession s, Rectangle view)
	{
		return s.getPhase() + (s.getPhase() == Phase.TITLE ? "/" + s.getTitleScreen() : "") + (s.isOptionsOpen() ? "/options" : "")
			+ " at " + view.width + "x" + view.height;
	}

	@Test
	public void everyButtonFitsTheScreenIsBigEnoughAndStandsAlone()
	{
		for (GameSession s : screens())
		{
			for (Rectangle view : PHONES)
			{
				String where = describe(s, view);
				Rectangle panel = UiLayout.panel(view, s);
				if (panel != null)
				{
					assertTrue("panel on screen: " + where, view.contains(panel));
				}
				List<UiLayout.Button> buttons = UiLayout.buttons(view, s);
				Rectangle side = UiLayout.sidePanel(view, s);
				for (int i = 0; i < buttons.size(); i++)
				{
					UiLayout.Button b = buttons.get(i);
					assertTrue(b.action + " on screen: " + where, view.contains(b.bounds));
					// pets' cards are only looked at
					assertTrue(b.action + " big enough for a finger: " + where + " " + b.bounds,
						b.action == UiLayout.Action.PET_CARD || Math.min(b.bounds.width, b.bounds.height) >= FINGER);
					if (side != null && !(s.isBagShown() && UiLayout.bagCrowdsPause(view, s)))
					{
						// the bag only covers the pause screen when brought out over it on purpose
						assertFalse(b.action + " clear of the bag: " + where, side.intersects(b.bounds));
					}
					for (int j = 0; j < i; j++)
					{
						assertFalse(b.action + " and " + buttons.get(j).action + " apart: " + where,
							b.bounds.intersects(buttons.get(j).bounds));
					}
				}
			}
		}
	}

	@Test
	public void playingHasAPauseButtonClearOfTheBag()
	{
		GameSession s = new GameSession(new Random(3));
		s.advance(1);
		s.setTouch(true);
		for (Rectangle view : PHONES)
		{
			for (boolean bag : new boolean[]{true, false})
			{
				s.setBagShown(bag);
				UiLayout.Button pause = UiLayout.buttonAt(view, s, centre(UiLayout.pauseButton(view, s)));
				assertNotNull(pause);
				assertEquals(UiLayout.Action.PAUSE, pause.action);
				Rectangle side = UiLayout.sidePanel(view, s);
				assertTrue("a bag on every phone", side != null);
				assertFalse("pause button clear of the bag", side.intersects(pause.bounds));
			}
		}
		s.setTouch(false);
		assertNull("no pause button for a mouse", UiLayout.pauseButton(PHONES[2], s));
	}

	@Test
	public void bagSlotsSitInsideTheSidePanelOnShortScreensToo()
	{
		GameSession s = new GameSession(new Random(3));
		s.advance(1);
		s.setTouch(true);
		for (Rectangle view : PHONES)
		{
			Rectangle bag = UiLayout.bag(view, s);
			assertNotNull(bag);
			for (int i = 0; i < Inventory.SIZE; i++)
			{
				Rectangle slot = UiLayout.bagSlot(view, s, bag, i);
				assertTrue("slot " + i + " in the bag at " + view, bag.contains(slot));
				assertEquals(i, UiLayout.bagSlotAt(view, s, centre(slot)));
			}
			Rectangle tab = UiLayout.inventoryTab(view, s);
			assertTrue("the backpack tab is on the panel", bag.contains(tab));
			assertEquals(-1, UiLayout.bagSlotAt(view, s, centre(tab)));
		}
		assertTrue(UiLayout.shortSide(new Rectangle(0, 0, 667, 320), s));
		assertFalse(UiLayout.shortSide(new Rectangle(0, 0, 844, 390), s));
	}

	@Test
	public void phonesHeldUprightStackTheLevelUpCards()
	{
		GameSession s = Sessions.levelledUp();
		s.setTouch(true);
		assertTrue(UiLayout.stackCards(PHONES[0], s));
		assertFalse(UiLayout.stackCards(PHONES[2], s));
		List<UiLayout.Button> cards = UiLayout.buttons(PHONES[0], s);
		assertTrue(cards.get(1).bounds.y > cards.get(0).bounds.y);
		assertEquals(cards.get(0).bounds.x, cards.get(1).bounds.x);
	}

	@Test
	public void shortScreensPutThePauseStatsBesideTheEquipment()
	{
		GameSession s = new GameSession(new Random(3));
		s.advance(1);
		s.togglePause();
		s.setTouch(true);
		assertTrue(UiLayout.statsBeside(PHONES[2], s));
		assertFalse(UiLayout.statsBeside(PHONES[0], s));
		for (Rectangle view : PHONES)
		{
			Rectangle panel = UiLayout.panel(view, s);
			for (UiLayout.Tile t : UiLayout.loadoutTiles(panel, s, UiLayout.statsBeside(view, s)))
			{
				assertTrue("tile in the panel at " + view, panel.contains(t.bounds));
			}
		}
	}

	@Test
	public void phonesShowMoreWorldThanASliverAndTheMouseKeepsItsView()
	{
		Rectangle upright = PHONES[0];
		assertTrue(GameRenderer.visibleWidth(upright, true) >= GameRenderer.MIN_VIEW_WIDTH - 1e-9);
		Rectangle wide = new Rectangle(0, 0, 1920, 1000);
		assertEquals(GameRenderer.VIEW_HEIGHT, GameRenderer.visibleHeight(wide, false), 1e-9);
		// a phone on its side is zoomed in a little, for its small pixels
		assertTrue(GameRenderer.visibleHeight(PHONES[3], true) <= GameRenderer.TOUCH_SHORT_SIDE + 1e-9);
	}

	@Test
	public void theStickSteersPastItsDeadZoneAndFollowsAFarFinger()
	{
		TouchControls t = new TouchControls();
		assertEquals(0, t.direction()[0], 0);
		t.stickOrigin = new Point(100, 100);
		t.drag(new Point(102, 101));
		assertEquals("still inside the dead zone", 0, t.direction()[0], 0);
		assertFalse(t.steered);
		t.drag(new Point(200, 100));
		assertTrue(t.steered);
		assertTrue(t.direction()[0] > 0);
		assertEquals("the centre follows the finger", 200 - TouchControls.STICK_RADIUS, t.stickOrigin.x);
		t.drag(new Point(150, 100));
		assertTrue("turning back steers back at once", t.direction()[0] < 0);
		t.release();
		assertEquals(0, t.direction()[0], 0);
	}

	private static Point centre(Rectangle r)
	{
		return new Point((int) r.getCenterX(), (int) r.getCenterY());
	}
}
