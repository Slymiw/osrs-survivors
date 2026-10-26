package com.osrssurvivors.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import com.osrssurvivors.game.Changelog;
import com.osrssurvivors.game.EnemyKind;
import com.osrssurvivors.game.GameSession;
import com.osrssurvivors.game.MetaProgress;
import com.osrssurvivors.game.Perk;
import com.osrssurvivors.game.Pet;
import com.osrssurvivors.game.Phase;
import com.osrssurvivors.game.TitleScreen;
import com.osrssurvivors.game.Scenarios;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.Offer;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

/** Off-screen rendering only: draws into images, never a window. */
public class GameRendererTest
{
	private static final Rectangle[] VIEWS = {
		new Rectangle(4, 4, 512, 334), // fixed mode
		new Rectangle(0, 0, 1920, 1000), // large resizable
		new Rectangle(0, 0, 120, 80), // tiny
	};

	/** Stand-in artwork: solid images for everything, so every sprite path is exercised. */
	private static final class FakeAssets implements GameAssets
	{
		final Map<UiSprite, BufferedImage> sprites = new EnumMap<>(UiSprite.class);
		int actorRequests;

		FakeAssets()
		{
			for (UiSprite s : UiSprite.values())
			{
				sprites.put(s, solid(8, 8, Color.GRAY));
			}
		}

		@Override
		public ActorSprite enemy(EnemyKind kind, double facing, boolean moving, double animTime, double pixelsPerUnit)
		{
			actorRequests++;
			return new ActorSprite(solid(16, 24, Color.RED), 8, 23);
		}

		@Override
		public ActorSprite player(PlayerPose pose, double pixelsPerUnit)
		{
			actorRequests++;
			return new ActorSprite(solid(16, 30, Color.BLUE), 8, 29);
		}

		@Override
		public BufferedImage gearIcon(Gear gear)
		{
			return solid(32, 28, Color.ORANGE);
		}

		@Override
		public BufferedImage projectile(Gear weapon)
		{
			return solid(12, 12, Color.MAGENTA);
		}

		@Override
		public Terrain terrain()
		{
			return new Terrain(solid(200, 150, Color.GREEN.darker()), 4);
		}

		@Override
		public List<Prop> props(double minX, double minY, double maxX, double maxY, double pixelsPerUnit)
		{
			List<Prop> props = new ArrayList<>();
			for (int i = 0; i < 10; i++)
			{
				props.add(new Prop(minX + (maxX - minX) * i / 10, (minY + maxY) / 2, new ActorSprite(solid(20, 40, Color.DARK_GRAY), 10, 39)));
			}
			return props;
		}

		@Override
		public BufferedImage sprite(UiSprite sprite)
		{
			return sprites.get(sprite);
		}

		@Override
		public BufferedImage offerIcon(Offer offer)
		{
			return solid(24, 24, Color.YELLOW);
		}

		@Override
		public BufferedImage gem(int xpValue)
		{
			return solid(36, 32, Color.CYAN);
		}

		@Override
		public Font font(TextStyle style)
		{
			return new Font(Font.MONOSPACED, Font.PLAIN, 12);
		}

		@Override
		public boolean pixelFonts()
		{
			return true;
		}
	}

	@Test
	public void rendersEveryPhaseAtEverySize()
	{
		for (GameAssets assets : new GameAssets[]{GameAssets.NONE, new FakeAssets()})
		{
			for (Phase phase : Phase.values())
			{
				GameSession s = sessionIn(phase);
				assertEquals(phase, s.getPhase());
				for (Rectangle view : VIEWS)
				{
					BufferedImage img = draw(s, view, assets, new Point(view.x + view.width / 2, view.y + view.height / 2));
					// something was drawn inside the view, nothing outside it
					assertNotEquals(0, img.getRGB(view.x + view.width / 2, view.y + view.height / 2));
					if (view.x > 0)
					{
						assertEquals(0, img.getRGB(0, 0));
					}
				}
			}
		}
	}

	@Test
	public void usesProvidedArtwork()
	{
		FakeAssets assets = new FakeAssets();
		GameSession s = sessionIn(Phase.RUNNING);
		assertTrue(s.getEnemies().size() > 0);
		draw(s, VIEWS[1], assets, null);
		assertTrue("asked for player and enemy sprites", assets.actorRequests > 1);
	}

	@Test
	public void hoveredCardIsHighlighted()
	{
		GameSession s = Sessions.levelledUp();
		Rectangle view = VIEWS[1];
		UiLayout.Button card = UiLayout.buttons(view, s).get(0);
		Point inside = new Point(card.bounds.x + card.bounds.width - 3, card.bounds.y + card.bounds.height / 2);
		int idle = draw(s, view, GameAssets.NONE, null).getRGB(inside.x, inside.y);
		int hovered = draw(s, view, GameAssets.NONE, inside).getRGB(inside.x, inside.y);
		assertNotEquals(idle, hovered);
	}

	@Test
	public void rendersTheSlotMachineThroughItsSpin()
	{
		for (GameAssets assets : new GameAssets[]{GameAssets.NONE, new FakeAssets()})
		{
			GameSession s = Scenarios.bossDrop(new Random(8));
			BufferedImage before = null;
			for (double t : new double[]{0, 0.6, 1.5, 2.85, 3.6, 3.9, 4.4, 6})
			{
				Scenarios.spin(s, t - s.getBossDrop().getTime());
				for (Rectangle view : VIEWS)
				{
					BufferedImage img = draw(s, view, assets, null);
					assertNotEquals(0, img.getRGB(view.x + view.width / 2, view.y + view.height / 2));
				}
				BufferedImage now = draw(s, VIEWS[1], assets, null);
				if (before != null)
				{
					assertTrue("the reels move", differs(before, now));
				}
				before = now;
			}
		}
	}

	@Test
	public void rendersTheShopAndLockedPlaces()
	{
		MetaProgress meta = new MetaProgress();
		meta.coins = 420;
		meta.perks.put(Perk.MIGHT.name(), Perk.MIGHT.getMaxRank());
		meta.addPet(Pet.OLMLET);
		for (TitleScreen screen : TitleScreen.values())
		{
			GameSession s = new GameSession(new Random(3));
			s.setMeta(meta);
			s.showTitle(null);
			s.showTitleScreen(screen);
			for (Rectangle view : VIEWS)
			{
				for (UiLayout.Button b : UiLayout.buttons(view, s))
				{
					// hovering every card draws its description line too
					draw(s, view, new FakeAssets(), new Point(b.bounds.x + 2, b.bounds.y + 2));
				}
			}
		}
		GameSession main = new GameSession(new Random(3));
		main.setMeta(meta);
		main.showTitle(null);
		boolean shop = false;
		for (UiLayout.Button b : UiLayout.buttons(VIEWS[1], main))
		{
			shop |= b.action == UiLayout.Action.UPGRADES && b.label.contains("420");
		}
		assertTrue("the main menu leads to the shop, showing the coins", shop);
	}

	private static boolean differs(BufferedImage a, BufferedImage b)
	{
		for (int y = 0; y < a.getHeight(); y += 3)
		{
			for (int x = 0; x < a.getWidth(); x += 3)
			{
				if (a.getRGB(x, y) != b.getRGB(x, y))
				{
					return true;
				}
			}
		}
		return false;
	}

	@Test
	public void emptyViewIsIgnored()
	{
		draw(new GameSession(new Random(1)), new Rectangle(0, 0, 0, 0), GameAssets.NONE, null);
	}

	@Test
	public void visibleAreaKeepsFixedHeight()
	{
		Rectangle view = new Rectangle(0, 0, 1600, 900);
		assertEquals(GameRenderer.VIEW_HEIGHT * 16 / 9, GameRenderer.visibleWidth(view), 1e-9);
	}

	@Test
	public void wrapsLongText()
	{
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		List<String> lines = GameRenderer.wrap("Hit one more enemy per cast", g.getFontMetrics(), 60);
		assertTrue(lines.size() > 1);
		assertEquals("Hit one more enemy per cast", String.join(" ", lines));
		assertEquals(Arrays.asList("short"), GameRenderer.wrap("short", g.getFontMetrics(), 500));
		g.dispose();
	}

	@Test
	public void formatsTime()
	{
		assertEquals("0:00", GameRenderer.formatTime(0));
		assertEquals("1:05", GameRenderer.formatTime(65.9));
		assertEquals("12:00", GameRenderer.formatTime(720));
	}

	@Test
	public void groundShadesAreStable()
	{
		assertEquals(GameRenderer.tileShade(3, 7), GameRenderer.tileShade(3, 7));
		boolean varied = false;
		for (int i = 0; i < 20; i++)
		{
			varied |= GameRenderer.tileShade(i, 0) != GameRenderer.tileShade(0, 0);
		}
		assertTrue(varied);
	}

	private static BufferedImage draw(GameSession s, Rectangle view, GameAssets assets, Point mouse)
	{
		BufferedImage img = new BufferedImage(view.x + Math.max(1, view.width) + 4, view.y + Math.max(1, view.height) + 4,
			BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		assets.beginFrame();
		GameRenderer.render(g, view, s, assets, mouse, "Ctrl+Shift+G");
		g.dispose();
		return img;
	}

	@Test
	public void devButtonsOnlyWithDevTools()
	{
		Rectangle view = VIEWS[1];
		GameSession s = sessionIn(Phase.PAUSED);
		assertTrue(actions(view, s).contains(UiLayout.Action.RESUME));
		assertTrue(!actions(view, s).contains(UiLayout.Action.DEV_END_RUN));
		s.setDevTools(true);
		assertTrue(actions(view, s).contains(UiLayout.Action.DEV_END_RUN));
		draw(s, view, GameAssets.NONE, null);
	}

	@Test
	public void petsPageShowsEveryPet()
	{
		Rectangle view = VIEWS[1];
		GameSession s = new GameSession(new Random(1));
		s.setMeta(new MetaProgress());
		s.showTitle(null);
		assertTrue(actions(view, s).contains(UiLayout.Action.PETS));
		s.showTitleScreen(TitleScreen.PETS);
		int cards = 0;
		for (UiLayout.Button b : UiLayout.buttons(view, s))
		{
			cards += b.action == UiLayout.Action.PET_CARD ? 1 : 0;
		}
		assertEquals(Pet.values().length, cards);
		draw(s, view, GameAssets.NONE, null);
	}

	@Test
	public void pauseScreenTooltipsCoverEverySlotAndPassive()
	{
		Rectangle view = VIEWS[1];
		GameSession s = sessionIn(Phase.PAUSED);
		Rectangle panel = UiLayout.panel(view, s);
		List<UiLayout.Tile> tiles = UiLayout.loadoutTiles(panel, s);
		assertTrue(tiles.size() >= 11 + GameSession.MAX_ABILITIES);
		for (UiLayout.Tile t : tiles)
		{
			assertTrue("tile inside the panel", panel.contains(t.bounds));
			BufferedImage hovered = draw(s, view, GameAssets.NONE, new Point(t.bounds.x + 2, t.bounds.y + 2));
			assertNotEquals(0, hovered.getRGB(t.bounds.x + 2, t.bounds.y + 2));
		}
	}

	private static List<UiLayout.Action> actions(Rectangle view, GameSession s)
	{
		List<UiLayout.Action> actions = new ArrayList<>();
		for (UiLayout.Button b : UiLayout.buttons(view, s))
		{
			actions.add(b.action);
		}
		return actions;
	}

	private static GameSession sessionIn(Phase phase)
	{
		switch (phase)
		{
			case RUNNING:
				GameSession s = new GameSession(new Random(3));
				for (int i = 0; i < 60 * 60 && s.getEnemies().size() < 6; i++)
				{
					if (s.getPhase() == Phase.LEVEL_UP)
					{
						s.chooseUpgrade(0);
					}
					s.advance(GameSession.STEP);
				}
				return s;
			case PAUSED:
				GameSession paused = new GameSession(new Random(3));
				paused.advance(1);
				paused.togglePause();
				return paused;
			case TITLE:
				GameSession title = new GameSession(new Random(3));
				title.showTitle("Level 4, 2:10");
				return title;
			case BOSS_DROP:
				return Scenarios.bossDrop(new Random(3));
			default:
				return Sessions.playUntil(phase);
		}
	}

	private static BufferedImage solid(int w, int h, Color c)
	{
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(c);
		g.fillRect(0, 0, w, h);
		g.dispose();
		return img;
	}

	@Test
	public void changelogShowsEveryReleaseOnceAcrossItsPages()
	{
		for (Rectangle view : new Rectangle[]{VIEWS[0], VIEWS[1], new Rectangle(0, 0, 400, 260)})
		{
			GameSession s = new GameSession(new Random(1));
			s.setMeta(new MetaProgress());
			s.showTitle(null);
			s.showTitleScreen(TitleScreen.CHANGELOG);
			List<Changelog.Release> seen = new ArrayList<>();
			for (int page = 0; page < Changelog.RELEASES.size(); page++)
			{
				s.setChangelogPage(page);
				Rectangle panel = UiLayout.panel(view, s);
				int buttonsTop = Integer.MAX_VALUE;
				for (UiLayout.Button b : UiLayout.buttons(view, s))
				{
					buttonsTop = Math.min(buttonsTop, b.bounds.y);
					assertTrue(panel.contains(b.bounds));
				}
				List<UiLayout.ReleaseCard> cards = UiLayout.releaseCards(view, s);
				assertFalse("every page shows something at " + view, cards.isEmpty());
				for (UiLayout.ReleaseCard c : cards)
				{
					assertTrue("card inside the panel at " + view, panel.contains(c.bounds));
					assertTrue("card above the buttons at " + view, c.bounds.y + c.bounds.height <= buttonsTop);
					if (!seen.contains(c.release))
					{
						seen.add(c.release);
					}
				}
				draw(s, view, new FakeAssets(), null);
				if (!actions(view, s).contains(UiLayout.Action.CHANGELOG_PAGE) || seen.size() == Changelog.RELEASES.size())
				{
					break;
				}
			}
			assertEquals("all releases reachable at " + view, Changelog.RELEASES, seen);
		}
	}

	@Test
	public void mainMenuPointsAtTheChangelogUntilItIsOpened()
	{
		Rectangle view = VIEWS[1];
		GameSession s = new GameSession(new Random(1));
		s.setMeta(new MetaProgress());
		s.showTitle(null);
		assertTrue(actions(view, s).contains(UiLayout.Action.CHANGELOG));
		BufferedImage quiet = draw(s, view, new FakeAssets(), null);
		s.setChangelogNews(true);
		BufferedImage news = draw(s, view, new FakeAssets(), null);
		assertTrue("the highlight and callout are drawn", differs(quiet, news));
		s.showTitleScreen(TitleScreen.CHANGELOG);
		assertFalse("opening the page clears it", s.hasChangelogNews());
	}

	@Test
	public void changelogDatesReadNaturally()
	{
		assertEquals("26 Oct 2026", GameRenderer.formatDate("2026-10-26"));
		assertEquals("3 Jan 2027", GameRenderer.formatDate("2027-01-03"));
		assertTrue(Changelog.hasNews(null));
		assertTrue(Changelog.hasNews("0.0"));
		assertFalse(Changelog.hasNews(Changelog.VERSION));
	}
}
