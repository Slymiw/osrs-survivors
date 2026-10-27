package com.osrssurvivors.render;

import com.osrssurvivors.game.Abilities;
import com.osrssurvivors.game.Arena;
import com.osrssurvivors.game.Boost;
import com.osrssurvivors.game.BossDrop;
import com.osrssurvivors.game.Build;
import com.osrssurvivors.game.Casket;
import com.osrssurvivors.game.Changelog;
import com.osrssurvivors.game.Combination;
import com.osrssurvivors.game.Effects;
import com.osrssurvivors.game.Enemy;
import com.osrssurvivors.game.EnemyKind;
import com.osrssurvivors.game.EnemyShot;
import com.osrssurvivors.game.GameSession;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.GearSet;
import com.osrssurvivors.game.Inventory;
import com.osrssurvivors.game.Item;
import com.osrssurvivors.game.ItemRarity;
import com.osrssurvivors.game.Location;
import com.osrssurvivors.game.MetaProgress;
import com.osrssurvivors.game.Offer;
import com.osrssurvivors.game.Option;
import com.osrssurvivors.game.Options;
import com.osrssurvivors.game.Passive;
import com.osrssurvivors.game.Perk;
import com.osrssurvivors.game.Pet;
import com.osrssurvivors.game.Phase;
import com.osrssurvivors.game.Player;
import com.osrssurvivors.game.Progression;
import com.osrssurvivors.game.Slot;
import com.osrssurvivors.game.Strike;
import com.osrssurvivors.game.TitleScreen;
import com.osrssurvivors.game.Volume;
import com.osrssurvivors.game.Weapon;
import com.osrssurvivors.game.XpOrb;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a {@link GameSession} into a screen rectangle in the style of the game's own interfaces.
 * Artwork and fonts come from {@link GameAssets}; anything missing falls back to plain shapes, so
 * the renderer works (and is tested) without a client. The camera follows the player and always
 * shows {@link #VIEW_HEIGHT} world units vertically.
 */
public final class GameRenderer
{
	/** World units visible from the top to the bottom of the view. */
	public static final double VIEW_HEIGHT = 480;

	// colours of the game's interfaces
	static final Color ORANGE = new Color(255, 152, 31);
	static final Color YELLOW = new Color(255, 255, 0);
	static final Color WHITE = Color.WHITE;
	static final Color RED = new Color(255, 0, 0);
	static final Color HINT = new Color(200, 190, 160);
	/** Shown on the main menu. */
	static final String CREDITS = "Free unofficial fan game, not affiliated with Jagex. Old School RuneScape and its artwork and sounds are the property of Jagex Ltd.";
	static final Color PANEL = new Color(62, 53, 41, 240);
	static final Color PANEL_EDGE_DARK = new Color(24, 20, 15);
	static final Color PANEL_EDGE_LIGHT = new Color(110, 98, 78);
	static final Color STONE = new Color(78, 68, 55);
	static final Color STONE_HOVER = new Color(104, 91, 73);
	static final Color STONE_PRESSED = new Color(66, 57, 45);
	static final Color STONE_DISABLED = new Color(58, 52, 44);
	static final Color PANEL_BAND = new Color(48, 41, 31, 200);
	private static final Color ABILITY_STRIP = new Color(110, 140, 255);
	private static final Color PASSIVE_STRIP = new Color(90, 200, 190);
	private static final Color SUPPLY_STRIP = new Color(200, 170, 110);
	static final Color SLOT_BACK = new Color(45, 39, 30);

	private static final Color OUTSIDE = new Color(18, 22, 14);
	private static final Color WALL = new Color(74, 70, 62);
	private static final Color WALL_DARK = new Color(38, 36, 32);
	private static final Color SHADOW = new Color(0, 0, 0, 55);
	private static final Color SHADE = new Color(0, 0, 0, 120);
	private static final Color HP_GREEN = new Color(0, 255, 0);
	private static final Color HP_RED = new Color(255, 0, 0);
	private static final Color XP_FILL = new Color(30, 150, 40);
	private static final Color BAR_BACK = new Color(0, 0, 0, 200);
	private static final Color PLAYER_FALLBACK = new Color(90, 150, 230);
	private static final Color SPELL_FALLBACK = new Color(220, 235, 255);
	private static final Color GEM_FALLBACK = new Color(80, 160, 230);
	private static final Color SHAFT = new Color(120, 85, 50);
	private static final Color ARROWHEAD = new Color(200, 200, 210);
	private static final Color SLASH = new Color(255, 255, 255);
	private static final Color[] GRASS = grass();
	private static final Color ICE = new Color(170, 220, 255);
	private static final Color FIRE = new Color(255, 140, 30);
	private static final Color SPLASH_COLOUR = new Color(200, 160, 255);
	private static final Color ENEMY_ARROW = new Color(90, 70, 50);
	private static final Color CANNON_FALLBACK = new Color(60, 60, 66);
	private static final Color CHAMPION_BAR = new Color(150, 20, 20);
	private static final Color BLOOD = new Color(170, 20, 30);
	static final Color GOD = new Color(255, 215, 90);
	static final Color GOLD = new Color(255, 200, 40);
	static final Color GREEN = new Color(0, 230, 0);
	private static final Color LOCKED = new Color(120, 110, 95);
	private static final Color REEL_BACK = new Color(22, 18, 13);
	private static final Color BULB_OFF = new Color(90, 70, 30);

	private GameRenderer()
	{
	}

	public static double scale(Rectangle view)
	{
		return view.height / VIEW_HEIGHT;
	}

	/** World units visible horizontally, for {@link GameSession#setVisibleArea}. */
	public static double visibleWidth(Rectangle view)
	{
		return view.width / scale(view);
	}

	/**
	 * @param mouse current mouse position for hover highlights, or null
	 * @param exitKey label of the key that leaves game mode, shown in the controls hint
	 */
	public static void render(Graphics2D graphics, Rectangle view, GameSession session, GameAssets assets,
		Point mouse, String exitKey)
	{
		render(graphics, view, session, assets, mouse, exitKey, -1);
	}

	/** @param dragging the bag slot whose item is being dragged by the mouse, or -1 */
	public static void render(Graphics2D graphics, Rectangle view, GameSession session, GameAssets assets,
		Point mouse, String exitKey, int dragging)
	{
		render(graphics, view, session, assets, mouse, exitKey, dragging, null, false);
	}

	/**
	 * @param fx screen effects to draw with the world (null for none)
	 * @param mouseDown whether the mouse button is held, so the button under it looks pressed
	 */
	public static void render(Graphics2D graphics, Rectangle view, GameSession session, GameAssets assets,
		Point mouse, String exitKey, int dragging, Fx fx, boolean mouseDown)
	{
		if (view.width <= 0 || view.height <= 0)
		{
			return;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.clip(view);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, assets.pixelFonts()
				? RenderingHints.VALUE_TEXT_ANTIALIAS_OFF : RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			Ui ui = new Ui(g, assets, session.getOptions(),
				session.getLevelUpTime() >= 0 ? session.getLevelUpTime() : session.getMenuTime(), mouse, mouseDown);
			new World(view, session, assets, fx).draw(g);
			if (session.getPhase() != Phase.TITLE)
			{
				drawVignette(ui, view, session, fx);
				drawHud(ui, view, session, exitKey, fx);
				drawToasts(ui, view, fx);
			}
			if (session.getPhase() != Phase.RUNNING)
			{
				g.setColor(SHADE);
				g.fill(view);
				BossDrop drop = session.getPhase() == Phase.BOSS_DROP ? session.getBossDrop() : null;
				Rectangle panel = drop == null ? null : UiLayout.panel(view, session);
				double levelUp = session.getPhase() == Phase.LEVEL_UP && !session.isCasketOpen() ? session.getLevelUpTime() : -1;
				Rectangle levelPanel = LevelUpShow.isPlaying(levelUp) ? UiLayout.panel(view, session) : null;
				boolean dropShow = ui.on(Option.DROP_SHOW);
				boolean levelShow = ui.on(Option.LEVEL_UP_SHOW);
				if (drop != null && dropShow)
				{
					drawDropBackdrop(ui, view, drop);
				}
				if (levelPanel != null)
				{
					LevelUpShow.drawBehind(g, view, levelPanel, levelUp, ui.on(Option.FLASHES), levelShow);
				}
				AffineTransform plain = g.getTransform();
				if (panel != null && dropShow)
				{
					// the machine pumps with a heartbeat and shakes as the reels land
					g.transform(DropShow.transform(drop, panel));
				}
				if (levelPanel != null && levelShow)
				{
					g.transform(LevelUpShow.transform(levelPanel, levelUp));
				}
				drawMenu(ui, view, session, mouse);
				g.setTransform(plain);
				if (drop != null)
				{
					drawCelebration(ui, view, drop);
				}
				if (levelPanel != null)
				{
					drawLevelUpBanner(ui, view, levelPanel, levelUp);
				}
			}
			drawBag(ui, view, session, mouse, dragging);
		}
		finally
		{
			g.dispose();
		}
	}

	/** Seconds a monster takes to rise into the fight. */
	static final double ENTRANCE = 0.35;
	/** Share of a hitsplat's life spent popping in. */
	static final double SPLAT_POP = 0.12;
	/** Blobs in a spell's tail, and the world units between them. */
	static final int TRAIL = 5;
	static final double TRAIL_STEP = 4;

	/**
	 * A twinkle on loot: a little four-pointed star that flares for a moment every couple of
	 * seconds, at a time set by {@code phase}.
	 */
	static void drawGlint(Graphics2D g, int x, int y, double phase, int size)
	{
		double cycle = phase - Math.floor(phase);
		if (cycle > 0.18)
		{
			return;
		}
		double k = Math.sin(cycle / 0.18 * Math.PI);
		int r = (int) Math.max(1, Math.round(size * k));
		g.setColor(new Color(255, 255, 255, (int) (220 * k)));
		g.fillRect(x - r, y, r * 2 + 1, 1);
		g.fillRect(x, y - r, 1, r * 2 + 1);
		g.setColor(new Color(255, 255, 255, (int) (120 * k)));
		g.fillRect(x - 1, y - 1, 3, 3);
	}

	/** The world: ground, scenery, pickups, characters and combat effects, centred on the player. */
	private static final class World
	{
		final Rectangle view;
		final GameSession session;
		final GameAssets assets;
		final double scale;
		final double originX;
		final double originY;
		/** Where each character's sprite top ended up, for placing health bars and hitsplats. */
		final Map<Enemy, Integer> heads = new IdentityHashMap<>();
		/** Screen effects, or null. */
		final Fx fx;
		int playerTop;

		World(Rectangle view, GameSession session, GameAssets assets, Fx fx)
		{
			this.view = view;
			this.session = session;
			this.assets = assets;
			this.fx = fx;
			this.scale = scale(view);
			Player p = session.getPlayer();
			// the world shakes; the interface over it stays put
			double shakeX = fx == null ? 0 : fx.getShakeX();
			double shakeY = fx == null ? 0 : fx.getShakeY();
			this.originX = view.x + view.width / 2.0 - p.getX() * scale + shakeX;
			this.originY = view.y + view.height / 2.0 - p.getY() * scale + shakeY;
		}

		int sx(double wx)
		{
			return (int) Math.round(originX + wx * scale);
		}

		int sy(double wy)
		{
			return (int) Math.round(originY + wy * scale);
		}

		int len(double w)
		{
			return Math.max(1, (int) Math.round(w * scale));
		}

		double worldX(int screenX)
		{
			return (screenX - originX) / scale;
		}

		double worldY(int screenY)
		{
			return (screenY - originY) / scale;
		}

		boolean visible(double wx, double wy, double margin)
		{
			int m = len(margin);
			int x = sx(wx);
			int y = sy(wy);
			return x + m >= view.x && x - m <= view.x + view.width && y + m >= view.y && y - m <= view.y + view.height;
		}

		void draw(Graphics2D g)
		{
			Terrain terrain = assets.terrain();
			if (terrain != null)
			{
				drawTerrain(g, terrain);
			}
			else
			{
				drawField(g);
			}
			if (fx != null)
			{
				fx.drawGround(g, originX, originY, scale);
			}
			drawBursts(g);
			drawChampionRings(g);
			drawGems(g);
			drawCaskets(g);
			drawSorted(g);
			drawPets(g);
			drawStrikes(g);
			drawShots(g);
			if (fx != null)
			{
				fx.drawAir(g, originX, originY, scale, view);
			}
			drawOverheads(g);
			drawChampionPointers(g);
		}

		/** A pulsing gold ring on the ground under each champion, so it stands out in a crowd. */
		private void drawChampionRings(Graphics2D g)
		{
			Stroke oldStroke = g.getStroke();
			Composite old = g.getComposite();
			double t = session.getElapsed();
			for (Enemy e : session.getEnemies())
			{
				if (!e.isChampion() || !visible(e.getX(), e.getY(), e.getRadius() * 4))
				{
					continue;
				}
				double pulse = 0.5 + 0.5 * Math.sin(t * 6);
				int r = len(e.getRadius() * (1.5 + 0.15 * pulse));
				int x = sx(e.getX());
				int y = sy(e.getY());
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
				g.setColor(GOLD);
				g.fillOval(x - r, y - r / 2, r * 2, r);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.55 + 0.4 * pulse)));
				g.setStroke(new BasicStroke(Math.max(2f, (float) scale * 2)));
				g.drawOval(x - r, y - r / 2, r * 2, r);
			}
			g.setComposite(old);
			g.setStroke(oldStroke);
		}

		/** The run's pets trotting along behind the player, bobbing as they go. */
		private void drawPets(Graphics2D g)
		{
			Player p = session.getPlayer();
			int i = 0;
			int count = session.getBuild().getPets().size();
			for (Pet pet : session.getBuild().getPets())
			{
				BufferedImage icon = assets.petIcon(pet);
				double angle = p.getFacing() + Math.PI + (i - (count - 1) / 2.0) * 0.45;
				double dist = 26 + 6 * (i % 2);
				double t = session.getElapsed() * 5 + i;
				int x = sx(p.getX() + Math.cos(angle) * dist);
				int y = sy(p.getY() + Math.sin(angle) * dist) - (int) Math.round(Math.abs(Math.sin(t)) * 3);
				int size = Math.max(14, len(18));
				if (icon != null)
				{
					int h = size * icon.getHeight() / Math.max(1, icon.getWidth());
					g.drawImage(icon, x - size / 2, y - h, size, h, null);
				}
				else
				{
					g.setColor(GOD);
					g.fillOval(x - 4, y - 8, 8, 8);
				}
				i++;
			}
		}

		/** An arrow at the edge of the view pointing at a champion that's off screen. */
		private void drawChampionPointers(Graphics2D g)
		{
			for (Enemy e : session.getEnemies())
			{
				if (!e.isChampion() || visible(e.getX(), e.getY(), 0))
				{
					continue;
				}
				double cx = view.x + view.width / 2.0;
				double cy = view.y + view.height / 2.0;
				double dx = sx(e.getX()) - cx;
				double dy = sy(e.getY()) - cy;
				double len = Math.hypot(dx, dy);
				if (len < 1)
				{
					continue;
				}
				double ux = dx / len;
				double uy = dy / len;
				double margin = 22;
				double reach = Math.min((view.width / 2.0 - margin) / Math.max(1e-6, Math.abs(ux)),
					(view.height / 2.0 - margin) / Math.max(1e-6, Math.abs(uy)));
				double px = cx + ux * reach;
				double py = cy + uy * reach;
				double pulse = 0.5 + 0.5 * Math.sin(session.getElapsed() * 8);
				int size = (int) Math.round(10 + 3 * pulse);
				int[] xs = {
					(int) Math.round(px + ux * size), (int) Math.round(px - ux * size * 0.6 - uy * size * 0.7),
					(int) Math.round(px - ux * size * 0.6 + uy * size * 0.7)};
				int[] ys = {
					(int) Math.round(py + uy * size), (int) Math.round(py - uy * size * 0.6 + ux * size * 0.7),
					(int) Math.round(py - uy * size * 0.6 - ux * size * 0.7)};
				g.setColor(Color.BLACK);
				g.drawPolygon(xs, ys, 3);
				g.setColor(GOLD);
				g.fillPolygon(xs, ys, 3);
			}
		}

		private void drawTerrain(Graphics2D g, Terrain terrain)
		{
			g.setColor(OUTSIDE);
			g.fill(view);
			Arena arena = session.getArena();
			double upp = terrain.unitsPerPixel;
			int sx1 = Math.max(0, (int) Math.floor(worldX(view.x) / upp));
			int sy1 = Math.max(0, (int) Math.floor(worldY(view.y) / upp));
			int sx2 = Math.min(terrain.image.getWidth(), (int) Math.ceil(worldX(view.x + view.width) / upp));
			int sy2 = Math.min(terrain.image.getHeight(), (int) Math.ceil(worldY(view.y + view.height) / upp));
			sx2 = Math.min(sx2, (int) Math.ceil(arena.getWidth() / upp));
			sy2 = Math.min(sy2, (int) Math.ceil(arena.getHeight() / upp));
			if (sx2 <= sx1 || sy2 <= sy1)
			{
				return;
			}
			g.drawImage(terrain.image, sx(sx1 * upp), sy(sy1 * upp), sx(sx2 * upp), sy(sy2 * upp), sx1, sy1, sx2, sy2, null);
		}

		/** The plain field: patchy grass tiles inside a low stone wall. */
		private void drawField(Graphics2D g)
		{
			Arena arena = session.getArena();
			g.setColor(OUTSIDE);
			g.fill(view);

			double tile = Arena.TILE;
			double left = Math.max(0, worldX(view.x));
			double right = Math.min(arena.getWidth(), worldX(view.x + view.width));
			double top = Math.max(0, worldY(view.y));
			double bottom = Math.min(arena.getHeight(), worldY(view.y + view.height));
			for (int ty = (int) (top / tile); ty < (int) Math.ceil(bottom / tile); ty++)
			{
				int y0 = sy(ty * tile);
				int y1 = sy(Math.min(arena.getHeight(), (ty + 1) * tile));
				for (int tx = (int) (left / tile); tx < (int) Math.ceil(right / tile); tx++)
				{
					int x0 = sx(tx * tile);
					int x1 = sx(Math.min(arena.getWidth(), (tx + 1) * tile));
					g.setColor(GRASS[tileShade(tx, ty)]);
					g.fillRect(x0, y0, x1 - x0, y1 - y0);
				}
			}

			int ax = sx(0);
			int ay = sy(0);
			int aw = sx(arena.getWidth()) - ax;
			int ah = sy(arena.getHeight()) - ay;
			int wall = len(6);
			g.setColor(WALL);
			g.fillRect(ax - wall, ay - wall, aw + 2 * wall, wall);
			g.fillRect(ax - wall, ay + ah, aw + 2 * wall, wall);
			g.fillRect(ax - wall, ay, wall, ah);
			g.fillRect(ax + aw, ay, wall, ah);
			g.setColor(WALL_DARK);
			g.drawRect(ax - wall, ay - wall, aw + 2 * wall, ah + 2 * wall);
			g.drawRect(ax, ay, aw, ah);
		}

		/** Splashes, blasts and Ice Barrage: rings on the ground that fade out. */
		private void drawBursts(Graphics2D g)
		{
			Composite old = g.getComposite();
			for (Effects.Burst b : session.getBursts())
			{
				if (!b.isLanded() || !visible(b.getX(), b.getY(), b.getRadius()))
				{
					continue;
				}
				double age = b.getAge();
				float fade = (float) (1 - age);
				// spreads fast, then settles
				int r = len(b.getRadius() * (0.55 + 0.45 * (1 - (1 - age) * (1 - age))));
				int x = sx(b.getX());
				int y = sy(b.getY());
				Color c = burstColour(b.getType());
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.32f * fade));
				g.setColor(c);
				g.fillOval(x - r, y - r / 2, r * 2, r);
				if (age < 0.35 && (b.getType() == Effects.Burst.Type.EXPLOSION || b.getType() == Effects.Burst.Type.DRAGONFIRE))
				{
					// a hot white core as it goes off
					int core = r / 2;
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.7 * (1 - age / 0.35))));
					g.setColor(new Color(255, 240, 200));
					g.fillOval(x - core, y - core / 2, core * 2, core);
				}
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f * fade));
				g.setColor(c);
				g.drawOval(x - r, y - r / 2, r * 2, r);
				BufferedImage icon = b.getType() == Effects.Burst.Type.ICE ? assets.sprite(UiSprite.ICE_SPELL) : null;
				if (icon != null)
				{
					g.drawImage(icon, x - icon.getWidth() / 2, y - icon.getHeight(), null);
				}
			}
			g.setComposite(old);
		}

		private Color burstColour(Effects.Burst.Type type)
		{
			switch (type)
			{
				case ICE:
					return ICE;
				case EXPLOSION:
				case DRAGONFIRE:
					return FIRE;
				case BLOOD:
					return BLOOD;
				case GOD:
					return GOD;
				default:
					return SPLASH_COLOUR;
			}
		}

		private void drawCaskets(Graphics2D g)
		{
			BufferedImage icon = assets.sprite(UiSprite.CASKET);
			Composite old = g.getComposite();
			for (Casket c : session.getCaskets())
			{
				int x = sx(c.getX());
				int y = sy(c.getY());
				// a slow bob so the loot catches the eye, over a pulsing gold glow
				int bob = (int) Math.round(Math.sin(session.getElapsed() * 4) * 3);
				double pulse = 0.5 + 0.5 * Math.sin(session.getElapsed() * 5);
				int glow = len(16 + 4 * pulse);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.25 + 0.2 * pulse)));
				g.setColor(GOLD);
				g.fillOval(x - glow, y - glow / 2, glow * 2, glow);
				g.setComposite(old);
				drawGlint(g, x + len(6), y - len(14) + bob, session.getElapsed() * 1.3, len(5));
				if (icon != null)
				{
					g.drawImage(icon, x - icon.getWidth() / 2, y - icon.getHeight() + bob, null);
				}
				else
				{
					g.setColor(YELLOW);
					g.fillRect(x - 8, y - 12 + bob, 16, 12);
				}
			}
		}

		/** Monster spells and arrows in flight. */
		private void drawShots(Graphics2D g)
		{
			BufferedImage spell = assets.sprite(UiSprite.ENEMY_SPELL);
			Stroke old = g.getStroke();
			for (EnemyShot s : session.getShots())
			{
				int x = sx(s.getX());
				int y = sy(s.getY());
				if (s.getType() == EnemyKind.Attack.ARROW)
				{
					double h = s.getHeading();
					g.setStroke(new BasicStroke(Math.max(1.5f, (float) scale)));
					g.setColor(ENEMY_ARROW);
					g.drawLine(x - (int) Math.round(Math.cos(h) * len(9)), y - (int) Math.round(Math.sin(h) * len(9)), x, y);
					g.setColor(ARROWHEAD);
					g.fillRect(x - 1, y - 1, 3, 3);
				}
				else if (spell != null)
				{
					int size = Math.max(12, len(10));
					// a dark-red glow round monster spells, so they read as danger
					g.setColor(new Color(200, 40, 90, 70));
					g.fillOval(x - size, y - size, size * 2, size * 2);
					g.drawImage(spell, x - size / 2, y - size / 2, size, size, null);
				}
				else
				{
					g.setColor(SPLASH_COLOUR);
					g.fillOval(x - 5, y - 5, 10, 10);
				}
			}
			g.setStroke(old);
		}

		/**
		 * XP gems: each bobs a little and now and then glints; one being pulled in trails a faint
		 * streak, so pickups read as flying to the player.
		 */
		private void drawGems(Graphics2D g)
		{
			int size = Math.max(10, len(XpOrb.RADIUS * 3.2));
			Player p = session.getPlayer();
			double magnet = p.getMagnetRadius();
			double t = session.getElapsed();
			Stroke oldStroke = g.getStroke();
			for (XpOrb orb : session.getOrbs())
			{
				if (!visible(orb.getX(), orb.getY(), XpOrb.RADIUS * 3))
				{
					continue;
				}
				double seed = noise((int) (orb.getX() * 7) * 31 + (int) (orb.getY() * 13));
				int x = sx(orb.getX());
				int y = sy(orb.getY()) - (int) Math.round(Math.sin(t * 3 + seed * 6.28) * 1.5 * scale);
				double dx = p.getX() - orb.getX();
				double dy = p.getY() - orb.getY();
				double dist = Math.hypot(dx, dy);
				if (dist <= magnet && dist > 1)
				{
					g.setStroke(new BasicStroke(Math.max(1f, (float) scale * 2)));
					g.setColor(new Color(160, 230, 255, 90));
					g.drawLine(x, y, x - (int) Math.round(dx / dist * len(12)), y - (int) Math.round(dy / dist * len(12)));
				}
				BufferedImage gem = assets.gem(orb.getValue());
				if (gem != null)
				{
					// item icons are 36x32; keep the aspect
					int h = size * gem.getHeight() / Math.max(1, gem.getWidth());
					g.drawImage(gem, x - size / 2, y - h / 2, size, h, null);
				}
				else
				{
					int r = len(XpOrb.RADIUS);
					g.setColor(GEM_FALLBACK);
					g.fillOval(x - r, y - r, r * 2, r * 2);
				}
				drawGlint(g, x + size / 4, y - size / 4, t * 0.8 + seed * 5, Math.max(3, size / 3));
			}
			g.setStroke(oldStroke);
		}

		/** Scenery, corpses, enemies and the player, back to front so nearer things overlap further ones. */
		private void drawSorted(Graphics2D g)
		{
			List<Runnable> draws = new ArrayList<>();
			List<Double> depths = new ArrayList<>();
			double animTime = session.getElapsed();

			// scenery can be big, so look a little beyond the view
			double margin = 160;
			for (Prop prop : assets.props(worldX(view.x) - margin, worldY(view.y) - margin,
				worldX(view.x + view.width) + margin, worldY(view.y + view.height) + margin * 2, scale))
			{
				depths.add(prop.y);
				draws.add(() -> g.drawImage(prop.sprite.image, sx(prop.x) - prop.sprite.anchorX, sy(prop.y) - prop.sprite.anchorY, null));
			}
			for (Effects.Corpse c : session.getCorpses())
			{
				if (visible(c.getX(), c.getY(), c.getRadius() * 4))
				{
					depths.add(c.getY());
					draws.add(() ->
					{
						Composite old = g.getComposite();
						double fall = c.getAge();
						g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) c.getStrength()));
						ActorSprite s = assets.enemy(c.getKind(), c.getRadius() > c.getKind().getRadius(), c.getFacing(), false, 0, scale);
						// sinks into the ground as it fades
						drawActor(g, s, c.getX(), c.getY(), c.getRadius(), kindColor(c.getKind()), (int) Math.round(fall * fall * len(8)));
						g.setComposite(old);
					});
				}
			}
			List<Runnable> onTop = new ArrayList<>();
			Player player = session.getPlayer();
			for (Enemy e : session.getEnemies())
			{
				if (visible(e.getX(), e.getY(), e.getRadius() * 4))
				{
					if (!e.isChampion())
					{
						depths.add(e.getY());
					}
					(e.isChampion() ? onTop : draws).add(() ->
					{
						ActorSprite s = assets.enemy(e.getKind(), e.isChampion(), e.getFacing(), e.isMoving() && !e.isFrozen(),
							animTime + e.getAnimOffset(), scale);
						// arriving monsters rise out of the ground and fade in
						double since = animTime - e.getBorn();
						double entrance = since >= 0 && since < ENTRANCE ? since / ENTRANCE : 1;
						Composite before = g.getComposite();
						if (entrance < 1)
						{
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) entrance));
						}
						// a hit knocks the sprite back a touch, away from the player
						double flash = e.getFlash();
						double away = Math.atan2(e.getY() - player.getY(), e.getX() - player.getX());
						int nudgeX = (int) Math.round(Math.cos(away) * flash * len(3));
						int nudgeY = (int) Math.round(Math.sin(away) * flash * len(2)) + (int) Math.round((1 - entrance) * len(10));
						int top = drawActor(g, s, e.getX(), e.getY(), e.getRadius(),
							e.isFlashing() && session.getOptions().isOn(Option.FLASHES) ? WHITE : kindColor(e.getKind()), nudgeY, nudgeX);
						g.setComposite(before);
						heads.put(e, top);
						if (e.isFrozen())
						{
							Composite old = g.getComposite();
							g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
							g.setColor(ICE);
							int r = len(e.getRadius());
							g.fillOval(sx(e.getX()) - r, sy(e.getY()) - r / 2, r * 2, r);
							g.setComposite(old);
						}
					});
				}
			}
			BufferedImage cannon = assets.sprite(UiSprite.CANNON);
			for (Abilities.Cannon c : session.getAbilities().getCannons())
			{
				depths.add(c.getY());
				draws.add(() ->
				{
					int x = sx(c.getX());
					int y = sy(c.getY());
					if (cannon != null)
					{
						int w = Math.max(16, len(30));
						int h = w * cannon.getHeight() / Math.max(1, cannon.getWidth());
						g.drawImage(cannon, x - w / 2, y - h, w, h, null);
					}
					else
					{
						int r = len(10);
						g.setColor(CANNON_FALLBACK);
						g.fillOval(x - r, y - r, r * 2, r * 2);
						g.drawLine(x, y - r / 2, x + (int) (Math.cos(c.getAim()) * r * 1.6), y - r / 2 + (int) (Math.sin(c.getAim()) * r * 1.6));
					}
				});
			}
			for (Abilities.Thrall t : session.getAbilities().getThralls())
			{
				depths.add(t.getY());
				draws.add(() ->
				{
					Composite old = g.getComposite();
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
					ActorSprite s = assets.enemy(EnemyKind.SKELETON, false, t.getFacing(), true, animTime, scale);
					drawActor(g, s, t.getX(), t.getY(), 8, new Color(200, 200, 230), 0);
					g.setComposite(old);
				});
			}

			Player p = session.getPlayer();
			depths.add(p.getY());
			draws.add(() ->
			{
				Build build = session.getBuild();
				double attack = p.getAttackAge() < Player.ATTACK_ANIMATION ? p.getAttackAge() / Player.ATTACK_ANIMATION : -1;
				PlayerPose pose = new PlayerPose(p.getFacing(), p.isMoving(), animTime, build.getWorn(), build.getGearVersion(), attack);
				ActorSprite s = assets.player(pose, scale);
				playerTop = drawActor(g, s, p.getX(), p.getY(), Player.RADIUS, PLAYER_FALLBACK, 0);
			});

			Integer[] order = new Integer[draws.size()];
			for (int i = 0; i < order.length; i++)
			{
				order[i] = i;
			}
			Arrays.sort(order, (a, b) -> Double.compare(depths.get(a), depths.get(b)));
			for (int i : order)
			{
				draws.get(i).run();
			}
			// champions last, so a crowd never hides them
			for (Runnable r : onTop)
			{
				r.run();
			}
		}

		/** Draws a sprite with its feet at the world position, or a fallback disc. Returns the top edge in screen y. */
		private int drawActor(Graphics2D g, ActorSprite sprite, double wx, double wy, double radius, Color fallback, int bob)
		{
			return drawActor(g, sprite, wx, wy, radius, fallback, bob, 0);
		}

		/** As above, the sprite moved {@code nudge} pixels sideways (its shadow stays). */
		private int drawActor(Graphics2D g, ActorSprite sprite, double wx, double wy, double radius, Color fallback, int bob, int nudge)
		{
			int x = sx(wx);
			int y = sy(wy);
			int r = len(radius);
			g.setColor(SHADOW);
			g.fillOval(x - r, y - r / 2, r * 2, r);
			if (sprite != null)
			{
				int top = y - sprite.anchorY + bob;
				g.drawImage(sprite.image, x - sprite.anchorX + nudge, top, null);
				return top;
			}
			g.setColor(fallback);
			g.fillOval(x - r + nudge, y - r * 2 + bob, r * 2, r * 2);
			g.setColor(Color.BLACK);
			g.drawOval(x - r + nudge, y - r * 2 + bob, r * 2, r * 2);
			return y - r * 2 + bob;
		}

		/**
		 * The player's attacks and the abilities' missiles: swings sweep across the target, arrows
		 * streak, spells glow and trail, lobbed things arc; missiles flash where they land.
		 */
		private void drawStrikes(Graphics2D g)
		{
			Stroke oldStroke = g.getStroke();
			Composite oldComposite = g.getComposite();
			for (Strike s : session.getStrikes())
			{
				Gear.Missile missile = s.weapon == null ? Gear.Missile.NONE : s.weapon.getMissile();
				double t = s.getProgress();
				double impact = s.getImpactAge();
				boolean lobbed = s.source == Strike.Source.CANNON || s.source == Strike.Source.CHINCHOMPA || missile == Gear.Missile.CHINCHOMPA;
				if (lobbed)
				{
					if (impact >= 0)
					{
						continue;
					}
					// lobbed: cannonballs and chinchompas
					double wx = s.fromX + (s.toX - s.fromX) * t;
					double wy = s.fromY + (s.toY - s.fromY) * t - 12 - Math.sin(t * Math.PI) * (s.source == Strike.Source.CANNON ? 6 : 30);
					BufferedImage icon = assets.sprite(s.source == Strike.Source.CANNON ? UiSprite.CANNONBALL : UiSprite.CHINCHOMPA);
					int x = sx(wx);
					int y = sy(wy);
					// its shadow on the ground, so the arc reads
					g.setColor(SHADOW);
					int sh = len(4);
					g.fillOval(sx(s.fromX + (s.toX - s.fromX) * t) - sh, sy(s.fromY + (s.toY - s.fromY) * t) - sh / 2, sh * 2, sh);
					if (icon != null)
					{
						int size = Math.max(10, len(s.source == Strike.Source.CANNON ? 8 : 12));
						g.drawImage(icon, x - size / 2, y - size / 2, size, size * icon.getHeight() / Math.max(1, icon.getWidth()), null);
					}
					else
					{
						g.setColor(CANNON_FALLBACK);
						g.fillOval(x - 3, y - 3, 6, 6);
					}
					continue;
				}
				if (missile == Gear.Missile.NONE && s.source == Strike.Source.PLAYER)
				{
					drawSwing(g, s, t);
					continue;
				}
				Color glow = s.source == Strike.Source.THRALL ? new Color(200, 200, 235) : missile == Gear.Missile.SPELL
					? Fx.spellColour(s.weapon) : new Color(255, 245, 220);
				if (impact >= 0)
				{
					// a flash where it lands
					int x = sx(s.toX);
					int y = sy(s.toY - 12);
					int r = len((missile == Gear.Missile.SPELL ? 5 : 3) + 9 * impact);
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.8 * (1 - impact))));
					g.setColor(glow);
					g.fillOval(x - r, y - r, r * 2, r * 2);
					g.setColor(WHITE);
					int core = Math.max(1, r / 3);
					g.fillOval(x - core, y - core, core * 2, core * 2);
					g.setComposite(oldComposite);
					continue;
				}
				// projectiles fly from chest height on the player to the target
				double dx = s.toX - s.fromX;
				double dy = s.toY - s.fromY;
				double d = Math.max(1, Math.hypot(dx, dy));
				double wx = s.fromX + dx * t;
				double wy = s.fromY + dy * t - 12;
				int x = sx(wx);
				int y = sy(wy);
				if (missile == Gear.Missile.SPELL || s.source == Strike.Source.THRALL)
				{
					BufferedImage spell = s.source == Strike.Source.THRALL ? null : assets.projectile(s.weapon);
					int size = Math.max(12, len(10));
					// a glow and a tapering tail behind it, no longer than the way it has come
					double travelled = d * t;
					for (int k = TRAIL; k >= 1; k--)
					{
						double back = Math.min(travelled, k * TRAIL_STEP);
						int tx = sx(wx - dx / d * back);
						int ty = sy(wy - dy / d * back);
						int r = Math.max(1, size * (TRAIL + 2 - k) / (2 * TRAIL + 2));
						g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 110 - k * (90 / TRAIL)));
						g.fillOval(tx - r, ty - r, r * 2, r * 2);
					}
					g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 80));
					g.fillOval(x - size * 3 / 4, y - size * 3 / 4, size * 3 / 2, size * 3 / 2);
					if (spell != null)
					{
						g.drawImage(spell, x - size / 2, y - size / 2, size, size, null);
					}
					else
					{
						g.setColor(s.source == Strike.Source.THRALL ? glow : SPELL_FALLBACK);
						g.fillOval(x - size / 4, y - size / 4, size / 2, size / 2);
					}
					continue;
				}
				double length = missile == Gear.Missile.DART ? 5 : 9;
				int tailX = sx(wx - dx / d * length);
				int tailY = sy(wy - dy / d * length);
				// a motion streak behind the shaft
				int streakX = sx(wx - dx / d * length * 2.6);
				int streakY = sy(wy - dy / d * length * 2.6);
				g.setStroke(new BasicStroke(Math.max(1f, (float) scale)));
				g.setColor(new Color(255, 250, 230, 70));
				g.drawLine(streakX, streakY, tailX, tailY);
				g.setStroke(new BasicStroke(Math.max(1.5f, (float) scale)));
				g.setColor(missile == Gear.Missile.BOLT ? new Color(90, 90, 100) : SHAFT);
				g.drawLine(tailX, tailY, x, y);
				g.setColor(ARROWHEAD);
				g.fillRect(x - 1, y - 1, 3, 3);
			}
			g.setStroke(oldStroke);
			g.setComposite(oldComposite);
		}

		/**
		 * A melee swing: a bright crescent sweeping across the target, its trailing edge fading,
		 * gone in a quarter of a second.
		 */
		private void drawSwing(Graphics2D g, Strike s, double t)
		{
			int x = sx(s.toX);
			int y = sy(s.toY - 10);
			int r = len(15);
			double angle = Math.toDegrees(Math.atan2(-(s.toY - s.fromY), s.toX - s.fromX));
			double ease = 1 - (1 - t) * (1 - t) * (1 - t);
			int lead = (int) Math.round(angle + 75 - 150 * ease);
			int span = (int) Math.round(Math.min(110, 150 * ease) + 10);
			float fade = (float) Math.max(0, 1 - t);
			Composite old = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f * fade));
			g.setColor(new Color(255, 236, 190));
			g.setStroke(new BasicStroke(Math.max(3f, (float) scale * 5)));
			g.drawArc(x - r, y - r, r * 2, r * 2, lead, span);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f * fade));
			g.setColor(SLASH);
			g.setStroke(new BasicStroke(Math.max(1.5f, (float) scale * 2)));
			g.drawArc(x - r, y - r, r * 2, r * 2, lead, Math.max(10, span / 2));
			g.setComposite(old);
		}

		/** Health bars over hurt characters and hitsplats, drawn above everything in the world. */
		private void drawOverheads(Graphics2D g)
		{
			for (Enemy e : session.getEnemies())
			{
				Integer top = heads.get(e);
				if (top != null && e.getHealth() < e.getMaxHealth())
				{
					healthBar(g, sx(e.getX()), top - 6, e.getHealth() / e.getMaxHealth());
				}
			}
			g.setFont(font(assets, TextStyle.BOLD));
			FontMetrics names = g.getFontMetrics();
			for (Enemy e : session.getEnemies())
			{
				Integer top = heads.get(e);
				if (e.isChampion() && top != null)
				{
					String name = e.getTitle();
					shadowText(g, name, sx(e.getX()) - names.stringWidth(name) / 2, top - 10, GOLD);
				}
			}
			Player p = session.getPlayer();
			if (p.getHealth() < p.getMaxHealth())
			{
				healthBar(g, sx(p.getX()), playerTop - 8, p.getHealth() / p.getMaxHealth());
			}

			BufferedImage splat = assets.sprite(UiSprite.HITSPLAT_DAMAGE);
			BufferedImage specialSplat = assets.sprite(UiSprite.SPECIAL_HITSPLAT);
			g.setFont(font(assets, TextStyle.SMALL));
			FontMetrics fm = g.getFontMetrics();
			Map<Object, Integer> stack = new IdentityHashMap<>();
			for (Effects.Hitsplat h : session.getHitsplats())
			{
				Enemy e = h.getTarget();
				Object key = e == null ? p : e;
				double ex = e == null ? p.getX() : e.getX();
				double ey = e == null ? p.getY() : e.getY();
				double radius = e == null ? Player.RADIUS : e.getRadius();
				if (!visible(ex, ey, radius * 4))
				{
					continue;
				}
				if (!h.isLanded())
				{
					continue;
				}
				int n = stack.merge(key, 1, Integer::sum) - 1;
				Integer top = e == null ? Integer.valueOf(playerTop) : heads.get(e);
				int x = sx(ex) + (n % 2 == 1 ? -10 : 0);
				int y = (top != null ? (top + sy(ey)) / 2 : sy(ey) - len(radius)) - (n / 2) * 14 - (n % 2) * 7;
				String text = String.valueOf(h.getDamage());
				BufferedImage image = h.isSpecial() && specialSplat != null ? specialSplat : splat;
				double age = h.getAge();
				// pops in a size larger, settles, and fades at the end
				double pop = age < SPLAT_POP ? 1 + 0.4 * (1 - age / SPLAT_POP) : 1;
				float alpha = (float) Math.min(1, (1 - age) / 0.2);
				Composite before = g.getComposite();
				if (alpha < 1)
				{
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
				}
				if (image != null)
				{
					int w = (int) Math.round(image.getWidth() * pop);
					int hh = (int) Math.round(image.getHeight() * pop);
					g.drawImage(image, x - w / 2, y - hh / 2, w, hh, null);
				}
				else
				{
					int w = (int) Math.round(20 * pop);
					int hh = (int) Math.round(18 * pop);
					g.setColor(e == null ? new Color(170, 0, 0) : new Color(150, 0, 0));
					g.fillOval(x - w / 2, y - hh / 2, w, hh);
				}
				shadowText(g, text, x - fm.stringWidth(text) / 2, y + fm.getAscent() / 2 - 1, h.isSpecial() ? YELLOW : WHITE);
				g.setComposite(before);
			}
		}

		private void healthBar(Graphics2D g, int centreX, int y, double fraction)
		{
			BufferedImage front = assets.sprite(UiSprite.HEALTH_FRONT);
			BufferedImage back = assets.sprite(UiSprite.HEALTH_BACK);
			double f = Math.max(0, Math.min(1, fraction));
			if (front != null && back != null)
			{
				int w = back.getWidth();
				int x = centreX - w / 2;
				g.drawImage(back, x, y, null);
				int fill = (int) Math.round(w * f);
				if (fill > 0)
				{
					g.drawImage(front, x, y, x + fill, y + front.getHeight(), 0, 0, fill, front.getHeight(), null);
				}
				return;
			}
			int w = 30;
			int x = centreX - w / 2;
			g.setColor(HP_RED);
			g.fillRect(x, y, w, 5);
			g.setColor(HP_GREEN);
			g.fillRect(x, y, (int) Math.round(w * f), 5);
		}
	}

	private static final Color HUD_BACK = new Color(16, 13, 9, 170);
	private static final Color HUD_EDGE = new Color(110, 98, 78, 200);
	private static final Color XP_BACK = new Color(14, 22, 12, 230);
	private static final Color XP_SHINE = new Color(140, 235, 120);
	private static final Color BADGE = new Color(58, 49, 36);
	/** Seconds into a run the controls line shows for (it shows again on the pause screen). */
	static final double HINT_SECONDS = 25;

	/**
	 * The heads-up display. Top left: hitpoints, the weapon and running potions. Top middle: the
	 * level and XP bar, and under it the champion's health (or its warning). Top right: time,
	 * kills, coins and the place, with XP drops floating up beneath. The controls line shows at
	 * the start of a run.
	 */
	private static void drawHud(Ui ui, Rectangle view, GameSession session, String exitKey, Fx fx)
	{
		Player player = session.getPlayer();
		Progression prog = session.getProgression();
		Graphics2D g = ui.g;

		// hitpoints orb top left, current weapon beside it in a slot
		int orbRight = drawHpOrb(ui, view.x + 6, view.y + 6, player);
		Gear weapon = session.getWeapon().getGear();
		BufferedImage weaponIcon = weapon == null ? null : ui.assets.gearIcon(weapon);
		if (weaponIcon != null)
		{
			Rectangle slot = new Rectangle(orbRight + 4, view.y + 6, UiLayout.SLOT, UiLayout.SLOT);
			drawSlotTile(ui, slot);
			g.drawImage(weaponIcon, slot.x + (slot.width - weaponIcon.getWidth()) / 2, slot.y + (slot.height - weaponIcon.getHeight()) / 2, null);
		}

		// level badge and XP bar, top centre
		int barW = Math.min(300, view.width / 2);
		int barH = 14;
		int badgeW = 30;
		int barX = view.x + (view.width - barW + badgeW) / 2;
		int barY = view.y + 9;
		double fill = fx != null ? fx.getXpShown() : Math.min(1.0, (double) prog.getXp() / prog.getXpToNext());
		double pulse = fx == null ? 0 : fx.getXpPulse();
		g.setColor(XP_BACK);
		g.fillRect(barX, barY, barW, barH);
		int fw = (int) Math.round((barW - 2) * Math.max(0, Math.min(1, fill)));
		if (fw > 0)
		{
			g.setColor(XP_FILL);
			g.fillRect(barX + 1, barY + 1, fw, barH - 2);
			// a lighter top edge gives the bar some depth
			g.setColor(new Color(XP_SHINE.getRed(), XP_SHINE.getGreen(), XP_SHINE.getBlue(), 120 + (int) (100 * pulse)));
			g.fillRect(barX + 1, barY + 1, fw, 3);
			if (pulse > 0)
			{
				// the leading edge glows as XP comes in
				g.setColor(new Color(220, 255, 200, (int) (200 * pulse)));
				g.fillRect(barX + fw - 2, barY + 1, 3, barH - 2);
			}
		}
		g.setColor(PANEL_EDGE_DARK);
		g.drawRect(barX, barY, barW, barH);
		g.setColor(HUD_EDGE);
		g.drawRect(barX - 1, barY - 1, barW + 2, barH + 2);
		ui.font(TextStyle.SMALL);
		ui.centred(prog.getXp() + " / " + prog.getXpToNext() + " xp", barX + barW / 2, barY + barH - 3, WHITE);
		// the level, in a badge at the bar's left end
		int badgeH = barH + 10;
		int badgeX = barX - badgeW - 4;
		int badgeY = barY + (barH - badgeH) / 2;
		g.setColor(BADGE);
		g.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);
		g.setColor(pulse > 0.5 ? GOLD : HUD_EDGE);
		g.drawRoundRect(badgeX, badgeY, badgeW - 1, badgeH - 1, 8, 8);
		ui.font(TextStyle.BOLD);
		ui.centredFit(String.valueOf(prog.getLevel()), badgeX + badgeW / 2, badgeY + (badgeH + ui.metrics().getAscent()) / 2 - 1,
			badgeW - 4, YELLOW);
		drawChampion(ui, session, barX - badgeW - 4, barY + barH + 8, barW + badgeW + 4, fx);

		// time, kills, coins and the place, top right, on a card
		int cardW = 118;
		int right = view.x + view.width - 8;
		int cardX = right - cardW + 2;
		ui.font(TextStyle.REGULAR);
		int lineH = ui.metrics().getHeight();
		int cardH = 8 + lineH * 3 + 12;
		g.setColor(HUD_BACK);
		g.fillRoundRect(cardX, view.y + 4, cardW, cardH, 10, 10);
		g.setColor(HUD_EDGE);
		g.drawRoundRect(cardX, view.y + 4, cardW - 1, cardH - 1, 10, 10);
		int row = view.y + 6 + ui.metrics().getAscent();
		ui.font(TextStyle.BOLD);
		ui.right(formatTime(session.getElapsed()), right - 6, row, YELLOW);
		ui.font(TextStyle.SMALL);
		ui.text(fit(session.getLocation().getTitle(), ui.metrics(), cardW - 60), cardX + 8, row - 1, HINT);
		row += lineH;
		ui.text("Kills", cardX + 8, row, HINT);
		ui.font(TextStyle.REGULAR);
		ui.right(String.valueOf(session.getKills()), right - 6, row, WHITE);
		row += lineH;
		ui.font(TextStyle.SMALL);
		ui.text("Coins", cardX + 8, row, HINT);
		ui.font(TextStyle.REGULAR);
		ui.right(String.valueOf(session.coinsSoFar()), right - 6, row, GOLD);

		// XP drops float up under the card, like the game's own
		ui.font(TextStyle.REGULAR);
		BufferedImage skill = ui.assets.sprite(UiSprite.XP_SKILL_ICON);
		int dropBase = view.y + 4 + cardH + 6;
		for (Effects.XpDrop d : session.getXpDrops())
		{
			double age = d.getAge();
			int y = (int) Math.round(dropBase + 50 - age * 50);
			Composite old = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, (1 - age) * 3)));
			String text = "+" + d.getAmount();
			int textX = right - 6 - ui.metrics().stringWidth(text);
			if (skill != null)
			{
				g.drawImage(skill, textX - skill.getWidth() - 2, y - skill.getHeight() + 2, null);
			}
			ui.text(text, textX, y, WHITE);
			g.setComposite(old);
		}

		ui.font(TextStyle.SMALL);
		drawPotionTimers(ui, view, session);
		// the controls, while a run is new and on the pause screen
		double since = session.getElapsed();
		float hint = session.getPhase() == Phase.PAUSED ? 1f : (float) Math.max(0, Math.min(1, (HINT_SECONDS - since) / 3));
		if (hint > 0)
		{
			Composite old = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hint));
			ui.centred("WASD / arrows: move   P / Esc: pause   I: bag   M: mute   " + exitKey + ": save & exit",
				view.x + view.width / 2, view.y + view.height - 6, HINT);
			g.setComposite(old);
		}
	}

	/** The living champion's name and health under the XP bar, or a warning that one is coming. */
	private static void drawChampion(Ui ui, GameSession session, int x, int y, int width, Fx fx)
	{
		Graphics2D g = ui.g;
		Enemy champion = session.getChampion();
		ui.font(TextStyle.BOLD);
		if (champion != null)
		{
			int h = 12;
			int top = y + ui.metrics().getAscent() + 3;
			ui.centred(champion.getTitle(), x + width / 2, y + ui.metrics().getAscent(), ORANGE);
			double health = Math.max(0, champion.getHealth() / champion.getMaxHealth());
			// a pale trail shows the damage just done, catching up after a moment
			double trail = fx == null ? health : Math.max(health, fx.getChampionShown());
			g.setColor(BAR_BACK);
			g.fillRect(x, top, width, h);
			g.setColor(new Color(255, 230, 180));
			g.fillRect(x + 1, top + 1, (int) Math.round((width - 2) * trail), h - 2);
			g.setColor(CHAMPION_BAR);
			g.fillRect(x + 1, top + 1, (int) Math.round((width - 2) * health), h - 2);
			g.setColor(new Color(220, 70, 60));
			g.fillRect(x + 1, top + 1, (int) Math.round((width - 2) * health), 3);
			g.setColor(PANEL_EDGE_DARK);
			g.drawRect(x, top, width, h);
			g.setColor(GOLD);
			g.drawRect(x - 1, top - 1, width + 2, h + 2);
			return;
		}
		double countdown = session.getChampionCountdown();
		if (countdown > 0 && countdown <= GameSession.CHAMPION_WARNING)
		{
			// throbs as it gets close
			boolean lit = !ui.on(Option.FLASHES) || ((int) (countdown * 3)) % 2 == 0;
			ui.centred("A champion approaches! (" + (int) Math.ceil(countdown) + ")", x + width / 2, y + ui.metrics().getAscent(),
				lit ? RED : ORANGE);
		}
	}

	/**
	 * A red edge on the screen when the player is hit (stronger for heavy blows), and a slow
	 * heartbeat of it while hitpoints are low.
	 */
	private static void drawVignette(Ui ui, Rectangle view, GameSession session, Fx fx)
	{
		Player p = session.getPlayer();
		if (session.getPhase() == Phase.GAME_OVER)
		{
			return;
		}
		double hp = p.getHealth() / Math.max(1, p.getMaxHealth());
		double low = hp < LOW_HEALTH ? (LOW_HEALTH - hp) / LOW_HEALTH : 0;
		boolean lively = ui.on(Option.FLASHES);
		double beat = lively ? 0.55 + 0.45 * Math.abs(Math.sin(session.getElapsed() * Math.PI * (1 + low))) : 0.8;
		double strength = Math.max(fx == null ? 0 : fx.getHurt() * (lively ? 0.9 : 0.5), low * 0.6 * beat);
		if (strength < 0.02)
		{
			return;
		}
		Graphics2D g = ui.g;
		int depth = Math.max(20, Math.min(view.width, view.height) / 6);
		int layers = 8;
		int step = Math.max(1, depth / layers);
		for (int i = 0; i < layers; i++)
		{
			int inset = i * step;
			int a = (int) Math.round(strength * 60 * (1 - i / (double) layers));
			if (a <= 0)
			{
				continue;
			}
			g.setColor(new Color(160, 0, 0, Math.min(255, a)));
			g.fillRect(view.x + inset, view.y + inset, view.width - 2 * inset, step);
			g.fillRect(view.x + inset, view.y + view.height - inset - step, view.width - 2 * inset, step);
			g.fillRect(view.x + inset, view.y + inset + step, step, view.height - 2 * inset - 2 * step);
			g.fillRect(view.x + view.width - inset - step, view.y + inset + step, step, view.height - 2 * inset - 2 * step);
		}
	}

	/** Below this share of hitpoints the screen's edge beats red. */
	static final double LOW_HEALTH = 0.3;

	/** Notices for big moments, sliding in under the XP bar. */
	private static void drawToasts(Ui ui, Rectangle view, Fx fx)
	{
		if (fx == null || fx.getToasts().isEmpty())
		{
			return;
		}
		Graphics2D g = ui.g;
		Composite old = g.getComposite();
		int y = view.y + 78;
		for (Fx.Toast t : fx.getToasts())
		{
			double in = Math.min(1, t.age / 0.18);
			double out = Math.min(1, (Fx.TOAST_SECONDS - t.age) / 0.45);
			float alpha = (float) Math.max(0, Math.min(in, out));
			int slide = (int) Math.round((1 - in) * -10);
			ui.font(TextStyle.BOLD);
			int titleW = ui.metrics().stringWidth(t.title);
			int ascent = ui.metrics().getAscent();
			ui.font(TextStyle.SMALL);
			int lineW = ui.metrics().stringWidth(t.line);
			int w = Math.min(view.width - 16, Math.max(titleW, lineW) + 36);
			int h = ascent + ui.metrics().getHeight() + 14;
			int x = view.x + (view.width - w) / 2;
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
			g.setColor(new Color(0, 0, 0, 90));
			g.fillRoundRect(x + 2, y + slide + 3, w, h, 12, 12);
			g.setColor(new Color(24, 20, 14, 235));
			g.fillRoundRect(x, y + slide, w, h, 12, 12);
			g.setColor(t.colour);
			g.drawRoundRect(x, y + slide, w - 1, h - 1, 12, 12);
			g.fillRoundRect(x + 6, y + slide + 6, 3, h - 12, 3, 3);
			ui.font(TextStyle.BOLD);
			ui.centredFit(t.title, x + w / 2, y + slide + 5 + ascent, w - 24, t.colour);
			ui.font(TextStyle.SMALL);
			ui.centredFit(t.line, x + w / 2, y + slide + 7 + ascent + ui.metrics().getHeight(), w - 24, NOTE_TEXT);
			y += h + 6;
		}
		g.setComposite(old);
	}

	/** Returns the right edge of what was drawn. */
	private static int drawHpOrb(Ui ui, int x, int y, Player player)
	{
		Graphics2D g = ui.g;
		double fraction = Math.max(0, Math.min(1, player.getHealth() / player.getMaxHealth()));
		String hp = String.valueOf((int) Math.ceil(player.getHealth()));
		Color hpColor = fraction > 0.5 ? HP_GREEN : fraction > 0.25 ? YELLOW : RED;
		BufferedImage frame = ui.assets.sprite(UiSprite.ORB_FRAME);
		BufferedImage empty = ui.assets.sprite(UiSprite.ORB_EMPTY);
		BufferedImage fill = ui.assets.sprite(UiSprite.ORB_HP_FILL);
		BufferedImage icon = ui.assets.sprite(UiSprite.ORB_HP_ICON);
		ui.font(TextStyle.SMALL);
		if (frame != null && empty != null && fill != null)
		{
			g.drawImage(frame, x, y, null);
			// the orb sits at the right of the frame, the number in the space to its left
			int ox = x + frame.getWidth() - fill.getWidth() - 4;
			int oy = y + (frame.getHeight() - fill.getHeight()) / 2;
			g.drawImage(empty, ox, oy, null);
			int filled = (int) Math.round(fill.getHeight() * fraction);
			int cut = fill.getHeight() - filled;
			g.drawImage(fill, ox, oy + cut, ox + fill.getWidth(), oy + fill.getHeight(), 0, cut, fill.getWidth(), fill.getHeight(), null);
			if (icon != null)
			{
				g.drawImage(icon, ox + (fill.getWidth() - icon.getWidth()) / 2, oy + (fill.getHeight() - icon.getHeight()) / 2, null);
			}
			// the number's tab is the lower part of the frame, below the orb's middle
			int tabMiddle = y + frame.getHeight() * 5 / 8;
			ui.centred(hp, x + (ox - x) / 2 + 2, tabMiddle + ui.metrics().getAscent() / 2, hpColor);
			return x + frame.getWidth();
		}
		int w = 120;
		int h = 16;
		g.setColor(BAR_BACK);
		g.fillRect(x, y, w, h);
		g.setColor(new Color(170, 30, 30));
		g.fillRect(x + 1, y + 1, (int) Math.round((w - 2) * fraction), h - 2);
		ui.text("HP " + hp + " / " + (int) player.getMaxHealth(), x + 4, y + h - 3, WHITE);
		return x + w;
	}

	private static void drawMenu(Ui ui, Rectangle view, GameSession session, Point mouse)
	{
		Rectangle panel = UiLayout.panel(view, session);
		if (panel == null)
		{
			return;
		}
		drawPanel(ui, panel);

		boolean options = session.isOptionsOpen();
		String title;
		Color titleColor = ORANGE;
		switch (options ? Phase.PAUSED : session.getPhase())
		{
			case LEVEL_UP:
				title = session.isCasketOpen() ? "You open the champion's casket!"
					: "Congratulations, you've reached level " + session.getProgression().getLevel() + "!";
				break;
			case GAME_OVER:
				title = "Oh dear, you are dead!";
				titleColor = RED;
				break;
			case TITLE:
				title = session.getTitleScreen() == TitleScreen.LOCATIONS ? "Where do you want to fight?"
					: session.getTitleScreen() == TitleScreen.UPGRADES ? "Upgrades"
					: session.getTitleScreen() == TitleScreen.PETS ? "Pets"
					: session.getTitleScreen() == TitleScreen.CHANGELOG ? "What's new"
					: session.getTitleScreen() == TitleScreen.SAVE_CODE ? "Save code" : "OSRS Survivors";
				break;
			case BOSS_DROP:
				// the rarity's banner bounces in above the panel once the reels stop
				title = "The champion's casket";
				break;
			default:
				title = options ? "Options" : "Paused";
				break;
		}
		ui.font(TextStyle.BOLD);
		ui.centredFit(title, panel.x + panel.width / 2, panel.y + 24, panel.width - 2 * UiLayout.PAD, titleColor);
		if (!options && session.getPhase() == Phase.TITLE && session.getTitleScreen() == TitleScreen.MAIN)
		{
			// the version, tucked after the game's name
			int end = panel.x + (panel.width + ui.metrics().stringWidth(title)) / 2;
			ui.font(TextStyle.SMALL);
			ui.text("v" + Changelog.VERSION, end + 5, panel.y + 24, HINT);
		}

		if (options)
		{
			ui.font(TextStyle.SMALL);
			ui.centredFit("Sound and screen settings. Press M to mute at any time.", panel.x + panel.width / 2,
				panel.y + UiLayout.TITLE_H + 6, panel.width - 2 * UiLayout.PAD, YELLOW);
		}
		else switch (session.getPhase())
		{
			case GAME_OVER:
				ui.font(TextStyle.REGULAR);
				Double best = session.getBestTimes().get(session.getLocation());
				String record = best == null ? "" : best <= session.getElapsed() + 1e-6 ? " (new best!)" : " (best " + formatTime(best) + ")";
				ui.centredFit(session.getLocation().getTitle() + ": " + formatTime(session.getElapsed()) + record
						+ "   Level " + session.getProgression().getLevel() + "   Kills " + session.getKills(),
					panel.x + panel.width / 2, panel.y + UiLayout.TITLE_H + 14, panel.width - 2 * UiLayout.PAD, YELLOW);
				ui.font(TextStyle.BOLD);
				int earned = Math.max(0, session.getCoinsEarned());
				MetaProgress profile = session.getMeta();
				ui.centredFit("+" + earned + " coins" + (profile == null ? "" : "   (" + profile.coins + " to spend on upgrades)"),
					panel.x + panel.width / 2, panel.y + UiLayout.TITLE_H + 14 + UiLayout.COINS_H, panel.width - 2 * UiLayout.PAD, GOLD);
				break;
			case BOSS_DROP:
				drawBossDrop(ui, panel, session.getBossDrop());
				break;
			case TITLE:
				ui.font(TextStyle.SMALL);
				String line;
				Color colour = YELLOW;
				if (session.getStatus() != null)
				{
					line = session.getStatus();
					colour = WHITE;
				}
				else if (session.getTitleScreen() == TitleScreen.LOCATIONS)
				{
					UiLayout.Button hovered = UiLayout.buttonAt(view, session, mouse);
					if (hovered != null && hovered.action == UiLayout.Action.CHOOSE_LOCATION)
					{
						Location l = Location.values()[hovered.index];
						boolean open = session.isUnlocked(l);
						line = open ? l.getBlurb() : l.unlockHint();
						colour = open ? YELLOW : RED;
					}
					else
					{
						line = "Real places from the game's map. Harder places hit harder, and pay more.";
					}
				}
				else if (session.getTitleScreen() == TitleScreen.UPGRADES)
				{
					UiLayout.Button hovered = UiLayout.buttonAt(view, session, mouse);
					MetaProgress meta = session.getMeta();
					line = hovered != null && hovered.action == UiLayout.Action.BUY_PERK ? Perk.values()[hovered.index].getEffect() + " per rank"
						: (meta == null ? 0 : meta.coins) + " coins. Runs earn coins; spend them on upgrades that last.";
					colour = hovered != null ? YELLOW : GOLD;
				}
				else if (session.getTitleScreen() == TitleScreen.PETS)
				{
					line = "Champions' caskets can hold a pet. Each one helps in every run.";
				}
				else if (session.getTitleScreen() == TitleScreen.CHANGELOG)
				{
					line = "You're playing version " + Changelog.VERSION + ". Newest first: scroll down for older.";
					colour = HINT;
				}
				else if (session.getTitleScreen() == TitleScreen.SAVE_CODE)
				{
					line = "Take your progress to another device or browser.";
				}
				else
				{
					MetaProgress meta = session.getMeta();
					int pets = meta == null ? 0 : meta.getPets().size();
					line = pets > 0 ? "Pets: " + pets + " / " + Pet.values().length + ". Champions drop the rest, if you're lucky."
						: "Pick a place, gear up from your bare fists, survive.";
				}
				ui.centredFit(line, panel.x + panel.width / 2, panel.y + UiLayout.TITLE_H + 6, panel.width - 2 * UiLayout.PAD, colour);
				drawChangelog(ui, view, session, mouse);
				if (session.getTitleScreen() == TitleScreen.SAVE_CODE)
				{
					drawSaveCodeHelp(ui, panel);
				}
				break;
			case PAUSED:
				drawLoadout(ui, panel, session);
				break;
			default:
				break;
		}

		for (UiLayout.Button b : UiLayout.buttons(view, session))
		{
			boolean enabled = enabled(session, b);
			boolean hover = enabled && mouse != null && b.bounds.contains(mouse);
			boolean pressed = hover && ui.mouseDown;
			if (b.action == UiLayout.Action.VOLUME_BAR)
			{
				drawVolumeRow(ui, b, Volume.values()[b.index], panel);
				continue;
			}
			AffineTransform beforeLift = g(ui).getTransform();
			if (b.action == UiLayout.Action.CHOOSE_UPGRADE && hover && !pressed)
			{
				// the hovered card rises to meet the cursor
				g(ui).translate(0, -3);
			}
			drawStoneButton(ui, b.bounds, hover, pressed, enabled);
			if (pressed)
			{
				g(ui).translate(0, 1);
			}
			if (b.action == UiLayout.Action.CHOOSE_UPGRADE)
			{
				drawCard(ui, b, session.getChoices().get(b.index), session.getBuild(), hover);
			}
			else if (b.action == UiLayout.Action.CHOOSE_LOCATION)
			{
				Location location = Location.values()[b.index];
				drawLocationCard(ui, b, location, session.getBestTimes().get(location), hover,
					session.isUnlocked(location));
			}
			else if (b.action == UiLayout.Action.BUY_PERK)
			{
				drawPerkCard(ui, b, Perk.values()[b.index], session.getMeta(), hover);
			}
			else if (b.action == UiLayout.Action.TOGGLE_OPTION)
			{
				drawOptionRow(ui, b, Option.values()[b.index], hover);
			}
			else if (b.action == UiLayout.Action.PET_CARD)
			{
				Pet pet = Pet.values()[b.index];
				drawPetCard(ui, b, pet, session.getMeta() != null && session.getMeta().hasPet(pet));
			}
			else
			{
				ui.font(TextStyle.BOLD);
				ui.centredFit(b.label, b.bounds.x + b.bounds.width / 2,
					b.bounds.y + (b.bounds.height + ui.metrics().getAscent()) / 2 - 2, b.bounds.width - 8,
					!enabled ? LOCKED : hover ? WHITE : ORANGE);
			}
			g(ui).setTransform(beforeLift);
		}
		if (session.getPhase() == Phase.PAUSED && !options)
		{
			drawLoadoutTooltip(ui, view, panel, session, mouse);
		}
		if (session.getPhase() == Phase.TITLE && session.getTitleScreen() == TitleScreen.MAIN && !options && session.hasChangelogNews())
		{
			for (UiLayout.Button b : UiLayout.buttons(view, session))
			{
				if (b.action == UiLayout.Action.CHANGELOG)
				{
					drawChangelogNews(ui, view, b.bounds, session.getMenuTime());
				}
			}
		}
		if (session.getPhase() == Phase.TITLE && session.getTitleScreen() == TitleScreen.MAIN && !options)
		{
			ui.font(TextStyle.SMALL);
			int cx = view.x + view.width / 2;
			int bottom = view.y + view.height - 6;
			if (ui.metrics().stringWidth(CREDITS) <= view.width - 32)
			{
				ui.centred(CREDITS, cx, bottom, HINT);
			}
			else
			{
				// narrow screens: one sentence per line
				int split = CREDITS.indexOf(". ") + 1;
				ui.centred(CREDITS.substring(0, split), cx, bottom - ui.metrics().getHeight(), HINT);
				ui.centred(CREDITS.substring(split + 1), cx, bottom, HINT);
			}
		}
	}

	/**
	 * The pause screen: the equipment grid with a table of combat numbers under it, and on the
	 * right the weapon, then abilities, prayers and skills, set bonuses and pets as tiles.
	 */
	private static void drawLoadout(Ui ui, Rectangle panel, GameSession session)
	{
		Graphics2D g = ui.g;
		Build build = session.getBuild();
		for (Map.Entry<Slot, Rectangle> e : UiLayout.equipmentSlots(panel).entrySet())
		{
			Rectangle r = e.getValue();
			drawSlotTile(ui, r);
			Gear gear = build.get(e.getKey());
			BufferedImage icon = gear == null ? null : ui.assets.gearIcon(gear);
			if (icon != null)
			{
				g.drawImage(icon, r.x + (r.width - icon.getWidth()) / 2, r.y + (r.height - icon.getHeight()) / 2, null);
			}
			else if (gear != null)
			{
				ui.font(TextStyle.SMALL);
				ui.centred(fit(gear.getTitle(), ui.metrics(), r.width - 4), r.x + r.width / 2, r.y + r.height / 2 + 4, YELLOW);
			}
		}

		// combat numbers, label on the left and value on the right
		Weapon weapon = session.getWeapon();
		int left = panel.x + UiLayout.PAD;
		int right = left + UiLayout.LOADOUT_LEFT_W;
		int y = UiLayout.statsTop(panel);
		String[][] stats = {
			{"Max hit", String.valueOf(Math.round(weapon.getMaxHit()))},
			{"Attack every", String.format("%.2f s", weapon.getCooldown())},
			{"Range", String.valueOf(Math.round(weapon.getRange()))},
			{"Targets", String.valueOf(weapon.getTargets())},
			{"Damage taken", "-" + Math.round(build.damageReduction() * 100) + "%"},
			{"Hitpoints", (int) Math.ceil(session.getPlayer().getHealth()) + " / " + (int) session.getPlayer().getMaxHealth()},
			{"Level speed-up", "+" + Math.round((1 / build.levelHaste() - 1) * 100) + "%"},
		};
		ui.font(TextStyle.SMALL);
		for (String[] row : stats)
		{
			y += UiLayout.STAT_LINE;
			ui.text(row[0], left, y, HINT);
			ui.right(row[1], right, y, WHITE);
			g.setColor(new Color(0, 0, 0, 50));
			g.drawLine(left, y + 3, right, y + 3);
		}

		// the weapon, then the sections
		int x = UiLayout.loadoutRightX(panel);
		int width = panel.x + panel.width - UiLayout.PAD - x;
		g.setColor(PANEL_EDGE_DARK);
		g.drawLine(x - 10, panel.y + UiLayout.TITLE_H, x - 10, UiLayout.statsTop(panel) + UiLayout.STAT_ROWS * UiLayout.STAT_LINE);
		ui.font(TextStyle.BOLD);
		String name = weapon.getGear() == null ? "Fists" : weapon.getGear().getTitle();
		ui.text(fit(name + " (" + build.getStyle().getLabel() + ")", ui.metrics(), width), x, panel.y + UiLayout.TITLE_H + 12, WHITE);
		for (UiLayout.Section section : UiLayout.loadoutSections(panel, session))
		{
			ui.font(TextStyle.SMALL);
			int head = section.y + ui.metrics().getAscent();
			ui.text(section.title, x, head, ORANGE);
			g.setColor(PANEL_EDGE_LIGHT);
			int after = x + ui.metrics().stringWidth(section.title) + 6;
			g.drawLine(after, head - 4, x + width, head - 4);
			int ly = section.y + UiLayout.SECTION_HEAD - 4;
			for (String line : section.lines)
			{
				ly += UiLayout.SECTION_LINE;
				ui.text(fit(line, ui.metrics(), width), x, ly, YELLOW);
			}
			for (UiLayout.Tile t : section.tiles)
			{
				drawLoadoutTile(ui, t);
			}
		}
	}

	private static void drawSlotTile(Ui ui, Rectangle r)
	{
		BufferedImage slotTile = ui.assets.sprite(UiSprite.EQUIPMENT_SLOT);
		if (slotTile != null)
		{
			ui.g.drawImage(slotTile, r.x, r.y, r.width, r.height, null);
		}
		else
		{
			ui.g.setColor(SLOT_BACK);
			ui.g.fill(r);
			ui.g.setColor(PANEL_EDGE_DARK);
			ui.g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
		}
	}

	/** A passive (icon and rank pips), a pet, or an empty ability place. */
	private static void drawLoadoutTile(Ui ui, UiLayout.Tile t)
	{
		Graphics2D g = ui.g;
		Rectangle r = t.bounds;
		drawSlotTile(ui, r);
		if (t.passive == null && t.pet == null)
		{
			// a free ability place
			g.setColor(new Color(255, 255, 255, 30));
			g.drawLine(r.x + r.width / 2 - 5, r.y + r.height / 2, r.x + r.width / 2 + 5, r.y + r.height / 2);
			g.drawLine(r.x + r.width / 2, r.y + r.height / 2 - 5, r.x + r.width / 2, r.y + r.height / 2 + 5);
			return;
		}
		BufferedImage icon = t.pet != null ? ui.assets.petIcon(t.pet) : ui.assets.offerIcon(Offer.passive(t.passive, t.rank));
		if (icon != null)
		{
			int w = Math.min(r.width - 4, icon.getWidth());
			int h = w * icon.getHeight() / Math.max(1, icon.getWidth());
			g.drawImage(icon, r.x + (r.width - w) / 2, r.y + (r.height - h) / 2 - (t.passive != null ? 2 : 0), w, h, null);
		}
		else
		{
			ui.font(TextStyle.SMALL);
			String label = t.pet != null ? t.pet.getTitle() : t.passive.title(t.rank);
			ui.centred(fit(label, ui.metrics(), r.width - 4), r.x + r.width / 2, r.y + r.height / 2 + 4, YELLOW);
		}
		if (t.passive == null)
		{
			return;
		}
		int max = t.passive.getMaxRank();
		if (max <= 1)
		{
			return;
		}
		boolean maxed = t.rank >= max;
		if (max <= 6)
		{
			// rank pips along the bottom edge
			int pip = 3;
			int gap = 2;
			int total = max * pip + (max - 1) * gap;
			int px = r.x + (r.width - total) / 2;
			int py = r.y + r.height - pip - 3;
			for (int i = 0; i < max; i++)
			{
				g.setColor(i < t.rank ? (maxed ? GOLD : GREEN) : new Color(0, 0, 0, 160));
				g.fillRect(px + i * (pip + gap), py, pip, pip);
			}
		}
		else
		{
			ui.font(TextStyle.SMALL);
			ui.right(t.rank + "/" + max, r.x + r.width - 2, r.y + r.height - 2, maxed ? GOLD : GREEN);
		}
	}

	/** The name and effect of whatever slot, passive or pet the mouse is over. */
	private static void drawLoadoutTooltip(Ui ui, Rectangle view, Rectangle panel, GameSession session, Point mouse)
	{
		if (mouse == null)
		{
			return;
		}
		for (UiLayout.Tile t : UiLayout.loadoutTiles(panel, session))
		{
			if (!t.bounds.contains(mouse))
			{
				continue;
			}
			String title;
			String body;
			if (t.gear != null)
			{
				drawTooltip(ui, view, mouse, t.gear.getTitle(), rarityColour(t.gear.getRarity(), ui.time), t.gear.getRarity().getTitle(),
					t.gear.describe(), null);
				return;
			}
			else if (t.passive != null)
			{
				title = t.passive.title(t.rank);
				body = t.passive.getEffect() + (t.passive.getMaxRank() > 1 ? " per rank (rank " + t.rank + " of " + t.passive.getMaxRank() + ")" : "");
				Combination leads = Combination.from(session.getBuild().get(Slot.WEAPON), t.passive);
				if (leads != null)
				{
					body += ". At max rank, makes " + leads.getResult().getTitle();
				}
			}
			else if (t.pet != null)
			{
				title = t.pet.getTitle();
				body = t.pet.getEffect() + ", in every run";
			}
			else
			{
				return;
			}
			drawTooltip(ui, view, mouse, title, ORANGE, null, body, null);
			return;
		}
	}

	/**
	 * A tooltip by the mouse: the name (in its rarity's colour, with the rarity at the right for
	 * gear), what it does, and how to use it under a rule.
	 *
	 * @param tag shown at the right of the title row, or null
	 * @param hint controls, under a rule at the bottom, or null
	 */
	private static void drawTooltip(Ui ui, Rectangle view, Point mouse, String title, Color titleColour, String tag, String body,
		String hint)
	{
		Graphics2D g = ui.g;
		int width = 220;
		ui.font(TextStyle.SMALL);
		List<String> lines = wrap(body, ui.metrics(), width - 14);
		List<String> hints = hint == null ? new ArrayList<>() : wrap(hint, ui.metrics(), width - 14);
		int line = ui.metrics().getHeight();
		int height = 10 + line + lines.size() * line + (hints.isEmpty() ? 4 : 8 + hints.size() * line) + 4;
		// beside the cursor, flipped to its other side near the screen's edges
		int x = mouse.x + 16 + width <= view.x + view.width ? mouse.x + 16 : mouse.x - 16 - width;
		int y = Math.max(view.y + 2, Math.min(mouse.y + 14, view.y + view.height - height - 2));
		x = Math.max(view.x + 2, x);
		g.setColor(new Color(0, 0, 0, 80));
		g.fillRect(x + 3, y + 3, width, height);
		g.setColor(new Color(20, 17, 12, 240));
		g.fillRect(x, y, width, height);
		g.setColor(PANEL_EDGE_DARK);
		g.drawRect(x, y, width - 1, height - 1);
		g.setColor(new Color(titleColour.getRed(), titleColour.getGreen(), titleColour.getBlue(), 160));
		g.drawRect(x + 1, y + 1, width - 3, height - 3);
		int tagW = 0;
		if (tag != null)
		{
			ui.font(TextStyle.SMALL);
			tagW = ui.metrics().stringWidth(tag) + 8;
		}
		ui.font(TextStyle.BOLD);
		int titleBase = y + 5 + ui.metrics().getAscent();
		ui.text(fit(title, ui.metrics(), width - 14 - tagW), x + 7, titleBase, titleColour);
		ui.font(TextStyle.SMALL);
		if (tag != null)
		{
			ui.right(tag, x + width - 7, titleBase, titleColour);
		}
		int ty = y + 8 + line;
		for (String l : lines)
		{
			ty += ui.metrics().getAscent();
			ui.text(l, x + 7, ty, YELLOW);
			ty += line - ui.metrics().getAscent();
		}
		if (!hints.isEmpty())
		{
			ty += 3;
			g.setColor(new Color(110, 98, 78, 140));
			g.drawLine(x + 7, ty, x + width - 8, ty);
			ty += 2;
			for (String l : hints)
			{
				ty += ui.metrics().getAscent();
				ui.text(l, x + 7, ty, HINT);
				ty += line - ui.metrics().getAscent();
			}
		}
	}

	private static final Color SIDE_STONE = new Color(62, 53, 41);
	private static final Color SIDE_STONE_EDGE = new Color(28, 24, 18);
	private static final Color POTION_FALLBACK = new Color(150, 90, 200);
	private static final Color FOOD_FALLBACK = new Color(200, 120, 60);
	private static final Color GEAR_FALLBACK = new Color(120, 120, 130);

	/**
	 * The bag in the bottom right corner, the game's own side panel and inventory: tab stones,
	 * pillars and stone backing, the items on it, the one being dragged under the mouse, a tooltip
	 * for the one hovered, and any message about the bag. With the bag hidden only the stones show.
	 */
	private static void drawBag(Ui ui, Rectangle view, GameSession session, Point mouse, int dragging)
	{
		Rectangle side = UiLayout.sidePanel(view, session);
		if (side == null)
		{
			return;
		}
		Graphics2D g = ui.g;
		Rectangle bag = UiLayout.bag(view, session);
		BufferedImage stones = ui.assets.sprite(bag != null ? UiSprite.SIDE_PANEL : UiSprite.SIDE_TABS);
		if (stones != null)
		{
			g.drawImage(stones, side.x, side.y, null);
		}
		else
		{
			g.setColor(SIDE_STONE);
			g.fill(side);
			g.setColor(SIDE_STONE_EDGE);
			g.drawRect(side.x, side.y, side.width - 1, side.height - 1);
		}
		if (bag == null)
		{
			return;
		}
		Inventory items = session.getInventory();
		int hovered = -1;
		for (int i = 0; i < Inventory.SIZE; i++)
		{
			Rectangle r = UiLayout.bagSlot(bag, i);
			if (mouse != null && r.contains(mouse))
			{
				hovered = i;
			}
			Item item = items.get(i);
			if (item != null && i != dragging)
			{
				drawItem(ui, item, r.x + r.width / 2, r.y + r.height / 2, 1f);
			}
		}
		String notice = session.getNotice();
		if (notice != null)
		{
			ui.font(TextStyle.SMALL);
			List<String> lines = wrap(notice, ui.metrics(), bag.width);
			int y = bag.y - 4 - (lines.size() - 1) * ui.metrics().getHeight();
			for (String line : lines)
			{
				ui.right(line, bag.x + bag.width, y, RED);
				y += ui.metrics().getHeight();
			}
		}
		Item held = items.get(dragging);
		if (held != null && mouse != null)
		{
			drawItem(ui, held, mouse.x, mouse.y, 0.5f);
			return;
		}
		Item over = items.get(hovered);
		if (over != null)
		{
			String use = over.getGear() != null ? "wear" : over.getPotion() != null ? "drink" : "eat";
			Gear gear = over.getGear();
			drawTooltip(ui, view, mouse, over.getTitle(), gear != null ? rarityColour(gear.getRarity(), ui.time) : ORANGE,
				gear != null ? gear.getRarity().getTitle() : null, over.describe(),
				"Click to " + use + ". Shift-click or right-click to drop. Drag to move.");
		}
	}

	/** An item's picture centred on a point, or a plain stand-in without artwork. */
	private static void drawItem(Ui ui, Item item, int cx, int cy, float alpha)
	{
		Graphics2D g = ui.g;
		Composite old = g.getComposite();
		if (alpha < 1)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		}
		BufferedImage icon = ui.assets.itemIcon(item);
		if (icon != null)
		{
			g.drawImage(icon, cx - icon.getWidth() / 2, cy - icon.getHeight() / 2, null);
		}
		else
		{
			g.setColor(item.getGear() != null ? GEAR_FALLBACK : item.getPotion() != null ? POTION_FALLBACK : FOOD_FALLBACK);
			g.fillRoundRect(cx - 12, cy - 11, 24, 22, 8, 8);
			ui.font(TextStyle.SMALL);
			String label = item.getPotion() != null ? String.valueOf(item.getDoses()) : item.getTitle().substring(0, 2);
			ui.centred(label, cx, cy + 4, WHITE);
		}
		g.setComposite(old);
	}

	/** Potions working now, under the hitpoints orb: picture and seconds left, blinking near the end. */
	private static void drawPotionTimers(Ui ui, Rectangle view, GameSession session)
	{
		int x = view.x + 8;
		int y = view.y + 52;
		ui.font(TextStyle.SMALL);
		for (Map.Entry<Boost, Double> e : session.getBuild().getPotions().entrySet())
		{
			double left = e.getValue();
			if (left < 5 && (int) (left * 4) % 2 == 0)
			{
				y += 20;
				continue;
			}
			BufferedImage icon = ui.assets.offerIcon(Offer.boost(e.getKey()));
			if (icon != null)
			{
				ui.g.drawImage(icon, x, y, icon.getWidth() * 2 / 3, icon.getHeight() * 2 / 3, null);
			}
			int s = (int) Math.ceil(left);
			ui.text(String.format("%d:%02d", s / 60, s % 60), x + 26, y + 14, left < 10 ? ORANGE : WHITE);
			y += 20;
		}
	}

	private static final Color CARD_BACK = new Color(0, 0, 0, 70);
	private static final Color CARD_EDGE = new Color(150, 132, 100, 90);
	private static final Color PILL = new Color(120, 70, 10);
	private static final Color NEW_PILL = new Color(20, 120, 40);
	private static final Color NOTE_TEXT = new Color(240, 232, 210);
	private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	/** Edge of a note's picture on the changelog. */
	private static final int NOTE_ART = 20;

	private static final String[] SAVE_CODE_HELP = {
		"Progress is kept in this browser only. To play somewhere else, copy your code, send it to "
			+ "yourself (an email, a note, a chat), then choose Load a code there and paste it.",
		"The code holds your coins, upgrades, pets, best times and saved run. Loading one replaces "
			+ "the progress in that browser.",
	};

	/** The save code page's explanation, between its subtitle and its buttons. */
	private static void drawSaveCodeHelp(Ui ui, Rectangle panel)
	{
		ui.font(TextStyle.SMALL);
		int width = panel.width - 2 * UiLayout.PAD;
		int line = ui.metrics().getHeight();
		int y = panel.y + UiLayout.TITLE_H + UiLayout.SUBTITLE_H + 4 + ui.metrics().getAscent();
		int end = panel.y + UiLayout.TITLE_H + UiLayout.SUBTITLE_H + UiLayout.SAVE_CODE_TEXT_H;
		for (String paragraph : SAVE_CODE_HELP)
		{
			for (String l : wrap(paragraph, ui.metrics(), width))
			{
				if (y > end)
				{
					return;
				}
				ui.text(l, panel.x + UiLayout.PAD, y, NOTE_TEXT);
				y += line;
			}
			y += line / 2;
		}
	}

	private static final Color SCROLL_TRACK = new Color(0, 0, 0, 90);
	private static final Color SCROLL_THUMB = new Color(150, 132, 100);
	private static final Color SCROLL_THUMB_HOVER = new Color(200, 176, 128);

	/** The changelog's list of releases, clipped to its viewport, with a scrollbar beside it when it doesn't all fit. */
	private static void drawChangelog(Ui ui, Rectangle view, GameSession session, Point mouse)
	{
		Rectangle port = UiLayout.changelogViewport(view, session);
		if (port == null)
		{
			return;
		}
		Graphics2D g = ui.g;
		java.awt.Shape oldClip = g.getClip();
		g.clipRect(port.x, port.y, port.width, port.height);
		for (UiLayout.ReleaseCard card : UiLayout.releaseCards(view, session))
		{
			drawReleaseCard(ui, card);
		}
		g.setClip(oldClip);
		Rectangle track = UiLayout.changelogTrack(view, session);
		Rectangle thumb = UiLayout.changelogThumb(view, session);
		if (track == null || thumb == null)
		{
			return;
		}
		g.setColor(SCROLL_TRACK);
		g.fillRoundRect(track.x, track.y, track.width, track.height, track.width, track.width);
		g.setColor(mouse != null && track.contains(mouse) ? SCROLL_THUMB_HOVER : SCROLL_THUMB);
		g.fillRoundRect(thumb.x + 1, thumb.y, thumb.width - 2, thumb.height, thumb.width - 2, thumb.width - 2);
	}

	/**
	 * One release on the changelog: a version pill, the headline and the date across the top, then
	 * a row per change with its picture (or a bullet).
	 */
	private static void drawReleaseCard(Ui ui, UiLayout.ReleaseCard card)
	{
		Graphics2D g = ui.g;
		Rectangle r = card.bounds;
		Changelog.Release release = card.release;
		g.setColor(CARD_BACK);
		g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
		g.setColor(CARD_EDGE);
		g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 10, 10);
		if (card.latest)
		{
			g.setColor(GOLD);
			g.fillRoundRect(r.x, r.y + 6, 3, r.height - 12, 3, 3);
		}

		int x = r.x + 12;
		int baseline = r.y + 22;
		ui.font(TextStyle.BOLD);
		x = drawPill(ui, "v" + release.version, x, baseline, PILL, WHITE) + 6;
		if (card.latest)
		{
			ui.font(TextStyle.SMALL);
			x = drawPill(ui, "NEW", x, baseline, NEW_PILL, WHITE) + 6;
		}
		ui.font(TextStyle.SMALL);
		String date = formatDate(release.date);
		int dateW = ui.metrics().stringWidth(date);
		ui.right(date, r.x + r.width - 10, baseline, HINT);
		ui.font(TextStyle.BOLD);
		ui.text(fit(release.headline, ui.metrics(), r.x + r.width - 18 - dateW - x), x, baseline, ORANGE);

		int y = r.y + UiLayout.RELEASE_HEAD;
		for (Changelog.Note note : release.notes)
		{
			int cy = y + UiLayout.NOTE_H / 2 - 2;
			int ax = r.x + 12;
			BufferedImage art = noteArt(ui.assets, note.art);
			if (art != null)
			{
				double scale = Math.min(1, NOTE_ART / (double) Math.max(art.getWidth(), art.getHeight()));
				int w = (int) Math.round(art.getWidth() * scale);
				int h = (int) Math.round(art.getHeight() * scale);
				g.drawImage(art, ax + (NOTE_ART - w) / 2, cy - h / 2, w, h, null);
			}
			else
			{
				g.setColor(GOLD);
				g.fillOval(ax + NOTE_ART / 2 - 3, cy - 3, 6, 6);
			}
			int tx = ax + NOTE_ART + 10;
			int width = r.x + r.width - 10 - tx;
			ui.font(TextStyle.REGULAR);
			if (ui.metrics().stringWidth(note.text) > width)
			{
				ui.font(TextStyle.SMALL);
			}
			ui.text(fit(note.text, ui.metrics(), width), tx, cy + ui.metrics().getAscent() / 2 - 1, NOTE_TEXT);
			y += UiLayout.NOTE_H;
		}
	}

	/** A rounded label behind text, its baseline at {@code baseline}; returns its right edge. */
	private static int drawPill(Ui ui, String text, int x, int baseline, Color back, Color fore)
	{
		FontMetrics fm = ui.metrics();
		int w = fm.stringWidth(text) + 12;
		int h = fm.getAscent() + 6;
		ui.g.setColor(back);
		ui.g.fillRoundRect(x, baseline - fm.getAscent() - 3, w, h, h, h);
		ui.text(text, x + 6, baseline, fore);
		return x + w;
	}

	/** Picture for a changelog note: an item, a pet, a place or a monster; null for none. */
	private static BufferedImage noteArt(GameAssets assets, Object art)
	{
		if (art instanceof Location)
		{
			return assets.locationIcon((Location) art);
		}
		if (art instanceof Gear)
		{
			return assets.gearIcon((Gear) art);
		}
		if (art instanceof Pet)
		{
			return assets.petIcon((Pet) art);
		}
		if (art instanceof Perk)
		{
			return assets.perkIcon((Perk) art);
		}
		if (art instanceof Boost)
		{
			return assets.offerIcon(Offer.boost((Boost) art));
		}
		if (art instanceof EnemyKind)
		{
			ActorSprite sprite = assets.enemy((EnemyKind) art, false, Math.PI / 2, false, 0, 0.5);
			return sprite == null ? null : sprite.image;
		}
		return null;
	}

	/** 2026-10-26 as 26 Oct 2026. */
	static String formatDate(String iso)
	{
		String[] parts = iso.split("-");
		if (parts.length != 3)
		{
			return iso;
		}
		int month = Integer.parseInt(parts[1]);
		return Integer.parseInt(parts[2]) + " " + MONTHS[Math.max(1, Math.min(12, month)) - 1] + " " + parts[0];
	}

	/**
	 * After an update, until the changelog is opened: a gold glow round its button, a "NEW" badge,
	 * and a callout beside it saying what's there. The glow pulses only with flashes on.
	 */
	private static void drawChangelogNews(Ui ui, Rectangle view, Rectangle b, double time)
	{
		Graphics2D g = ui.g;
		boolean lively = ui.on(Option.FLASHES);
		double pulse = lively ? 0.5 + 0.5 * Math.sin(time * 4) : 1;
		Stroke oldStroke = g.getStroke();
		for (int i = 3; i >= 1; i--)
		{
			g.setStroke(new BasicStroke(i * 2));
			g.setColor(new Color(255, 200, 40, (int) Math.round((40 + 50 * pulse) / i)));
			g.drawRoundRect(b.x - i, b.y - i, b.width + 2 * i - 1, b.height + 2 * i - 1, 8, 8);
		}
		g.setStroke(new BasicStroke(1));
		g.setColor(new Color(255, 215, 90, (int) Math.round(160 + 95 * pulse)));
		g.drawRoundRect(b.x - 1, b.y - 1, b.width + 1, b.height + 1, 6, 6);
		g.setStroke(oldStroke);
		ui.font(TextStyle.SMALL);
		int badgeW = ui.metrics().stringWidth("NEW") + 12;
		drawPill(ui, "NEW", b.x + b.width - badgeW - 6, b.y + (b.height + ui.metrics().getAscent()) / 2 - 1, NEW_PILL, WHITE);

		// the callout: beside the button if there's room, else under it
		String head = "Updated to v" + Changelog.VERSION;
		String body = "See what's changed in What's new.";
		ui.font(TextStyle.BOLD);
		int w = ui.metrics().stringWidth(head);
		ui.font(TextStyle.SMALL);
		w = Math.max(w, ui.metrics().stringWidth(body)) + 20;
		int line = ui.metrics().getHeight();
		int h = 10 + line * 2 + 6;
		int bob = lively ? (int) Math.round(Math.sin(time * 3) * 2) : 0;
		boolean beside = b.x + b.width + 14 + w <= view.x + view.width - 4;
		int x;
		int y;
		int[] px;
		int[] py;
		if (beside)
		{
			x = b.x + b.width + 14 + bob;
			y = b.y + (b.height - h) / 2;
			int my = b.y + b.height / 2;
			px = new int[]{x - 8, x + 1, x + 1};
			py = new int[]{my, my - 7, my + 7};
		}
		else
		{
			x = Math.max(view.x + 4, Math.min(b.x + (b.width - w) / 2, view.x + view.width - w - 4));
			y = b.y + b.height + 12 + bob;
			int mx = b.x + b.width / 2;
			px = new int[]{mx, mx - 7, mx + 7};
			py = new int[]{y - 8, y + 1, y + 1};
		}
		g.setColor(new Color(20, 17, 12, 240));
		g.fillRoundRect(x, y, w, h, 10, 10);
		g.fillPolygon(px, py, 3);
		g.setColor(GOLD);
		g.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
		ui.font(TextStyle.BOLD);
		ui.text(head, x + 10, y + 6 + ui.metrics().getAscent(), GOLD);
		ui.font(TextStyle.SMALL);
		ui.text(body, x + 10, y + 8 + line + ui.metrics().getAscent(), NOTE_TEXT);
	}

	/** A pet on the pets page: its picture, name and bonus, faded until it's been won. */
	/** A setting: its name on the left, On or Off on the right, and what it covers underneath. */
	private static void drawOptionRow(Ui ui, UiLayout.Button b, Option option, boolean hover)
	{
		Rectangle r = b.bounds;
		boolean on = ui.on(option);
		int width = r.width - 16;
		ui.font(TextStyle.BOLD);
		int y = r.y + 6 + ui.metrics().getAscent();
		ui.text(option.getTitle(), r.x + 8, y, hover ? WHITE : ORANGE);
		ui.right(on ? "On" : "Off", r.x + r.width - 8, y, on ? GREEN : RED);
		ui.font(TextStyle.SMALL);
		if (r.height >= UiLayout.OPTION_H)
		{
			ui.text(fit(option.getDescription(), ui.metrics(), width), r.x + 8, y + ui.metrics().getHeight() + 2, YELLOW);
		}
	}

	/**
	 * A volume row on the options page: its name on the left, then minus and plus buttons (drawn
	 * with the others) round a bar of {@link Volume#MAX} steps, and the percentage.
	 */
	private static void drawVolumeRow(Ui ui, UiLayout.Button bar, Volume volume, Rectangle panel)
	{
		Graphics2D g = ui.g;
		Rectangle r = bar.bounds;
		int level = ui.options.getVolume(volume);
		boolean muted = ui.options.isMuted();
		ui.font(TextStyle.BOLD);
		int baseline = r.y + (r.height + ui.metrics().getAscent()) / 2 - 1;
		ui.text(volume.getTitle(), panel.x + UiLayout.PAD + 8, baseline, ORANGE);
		boolean hover = ui.mouse != null && r.contains(ui.mouse);
		g.setColor(SLOT_BACK);
		g.fillRect(r.x, r.y + 4, r.width, r.height - 8);
		g.setColor(PANEL_EDGE_DARK);
		g.drawRect(r.x, r.y + 4, r.width - 1, r.height - 9);
		int steps = Volume.MAX;
		int gap = 2;
		int w = (r.width - 4 - gap * (steps - 1)) / steps;
		int preview = hover ? UiLayout.volumeAt(bar, ui.mouse) : -1;
		for (int i = 0; i < steps; i++)
		{
			int x = r.x + 2 + i * (w + gap);
			boolean on = i < level;
			Color c = on ? (muted && volume == Volume.MASTER ? LOCKED : GREEN) : new Color(0, 0, 0, 120);
			if (!on && i < preview)
			{
				c = new Color(0, 230, 0, 70);
			}
			// steps grow taller towards the loud end
			int h = (r.height - 12) * (i + 4) / (steps + 3);
			g.setColor(c);
			g.fillRect(x, r.y + r.height - 6 - h, w, h);
		}
		ui.font(TextStyle.SMALL);
		String label = muted && volume == Volume.MASTER ? "Muted" : level * 100 / Volume.MAX + "%";
		int minusX = r.x - 4 - UiLayout.VOLUME_BUTTON_W;
		ui.right(label, minusX - 6, baseline, muted && volume == Volume.MASTER ? RED : YELLOW);
	}

	/** Whether a button can be used now; others are drawn dull and don't light up. */
	private static boolean enabled(GameSession session, UiLayout.Button b)
	{
		switch (b.action)
		{
			case BUY_PERK:
			{
				MetaProgress meta = session.getMeta();
				int cost = meta == null ? -1 : meta.cost(Perk.values()[b.index]);
				return cost >= 0 && meta.coins >= cost;
			}
			case CHOOSE_LOCATION:
				return session.isUnlocked(Location.values()[b.index]);
			case PET_CARD:
				return false;
			default:
				return true;
		}
	}

	private static Graphics2D g(Ui ui)
	{
		return ui.g;
	}

	private static void drawPetCard(Ui ui, UiLayout.Button b, Pet pet, boolean owned)
	{
		Graphics2D g = ui.g;
		Rectangle r = b.bounds;
		BufferedImage icon = ui.assets.petIcon(pet);
		int textX = r.x + 6;
		if (icon != null)
		{
			Composite old = g.getComposite();
			if (!owned)
			{
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
			}
			g.drawImage(icon, r.x + 6, r.y + (r.height - icon.getHeight()) / 2, null);
			g.setComposite(old);
			textX += icon.getWidth() + 6;
		}
		int width = r.x + r.width - 4 - textX;
		ui.font(TextStyle.BOLD);
		int y = r.y + 4 + ui.metrics().getAscent();
		ui.text(fit(pet.getTitle(), ui.metrics(), width), textX, y, owned ? ORANGE : HINT);
		ui.font(TextStyle.SMALL);
		y += ui.metrics().getHeight();
		ui.text(fit(pet.getEffect(), ui.metrics(), width), textX, y, owned ? YELLOW : HINT);
		y += ui.metrics().getHeight();
		if (y <= r.y + r.height - 2)
		{
			ui.text(owned ? "Owned" : "Not found yet", textX, y, owned ? GREEN : new Color(140, 130, 110));
		}
	}

	/** A location on the location menu: its icon, name, difficulty and best time there. */
	private static void drawLocationCard(Ui ui, UiLayout.Button b, Location location, Double best, boolean hover, boolean unlocked)
	{
		Rectangle r = b.bounds;
		int x = r.x + 6;
		BufferedImage icon = ui.assets.locationIcon(location);
		if (icon != null)
		{
			Composite old = ui.g.getComposite();
			if (!unlocked)
			{
				ui.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
			}
			ui.g.drawImage(icon, x, r.y + (r.height - icon.getHeight()) / 2, null);
			ui.g.setComposite(old);
			x += Math.max(icon.getWidth(), 24) + 6;
		}
		int width = r.x + r.width - 4 - x;
		ui.font(TextStyle.BOLD);
		int y = r.y + 4 + ui.metrics().getAscent();
		ui.text(fit(location.getTitle(), ui.metrics(), width), x, y, !unlocked ? LOCKED : hover ? WHITE : ORANGE);
		ui.font(TextStyle.SMALL);
		int line = ui.metrics().getHeight();
		if (y + line <= r.y + r.height - 2)
		{
			String difficulty = !unlocked ? "Locked" : "Difficulty " + location.getDifficulty() + "/5";
			ui.text(fit(difficulty, ui.metrics(), width), x, y + line, unlocked ? YELLOW : RED);
		}
		if (best != null && y + line * 2 <= r.y + r.height - 2)
		{
			ui.text("Best " + formatTime(best), x, y + line * 2, HINT);
		}
	}

	/** An upgrade in the shop: icon, name, rank and the price of the next rank. */
	private static void drawPerkCard(Ui ui, UiLayout.Button b, Perk perk, MetaProgress meta, boolean hover)
	{
		Rectangle r = b.bounds;
		int x = r.x + 6;
		BufferedImage icon = ui.assets.perkIcon(perk);
		if (icon != null)
		{
			ui.g.drawImage(icon, x, r.y + (r.height - icon.getHeight()) / 2, null);
			x += Math.max(icon.getWidth(), 24) + 6;
		}
		int width = r.x + r.width - 4 - x;
		int rank = meta == null ? 0 : meta.rank(perk);
		ui.font(TextStyle.BOLD);
		int y = r.y + 4 + ui.metrics().getAscent();
		ui.text(fit(perk.getTitle(), ui.metrics(), width), x, y, hover ? WHITE : ORANGE);
		ui.font(TextStyle.SMALL);
		int line = ui.metrics().getHeight();
		if (y + line <= r.y + r.height - 2)
		{
			ui.text("Rank " + rank + " / " + perk.getMaxRank(), x, y + line, YELLOW);
		}
		if (y + line * 2 <= r.y + r.height - 2)
		{
			int cost = meta == null ? -1 : meta.cost(perk);
			String price = cost < 0 ? "Maxed" : cost + " coins";
			ui.text(price, x, y + line * 2, cost < 0 ? HINT : meta.coins >= cost ? GREEN : RED);
		}
	}

	private static final Color COMMON_ITEM = new Color(205, 200, 190);
	private static final Color UNCOMMON_ITEM = new Color(60, 220, 60);
	private static final Color RARE_ITEM = new Color(70, 150, 255);
	private static final Color EPIC_ITEM = new Color(190, 95, 255);
	private static final Color LEGENDARY_ITEM = new Color(255, 135, 20);

	/** An item rarity's colour; the megarare's shifts with {@code time}. */
	static Color rarityColour(ItemRarity rarity, double time)
	{
		switch (rarity)
		{
			case UNCOMMON:
				return UNCOMMON_ITEM;
			case RARE:
				return RARE_ITEM;
			case EPIC:
				return EPIC_ITEM;
			case LEGENDARY:
				return LEGENDARY_ITEM;
			case MEGARARE:
				return megarareColour(0.5 + 0.5 * Math.sin(time * 3));
			default:
				return COMMON_ITEM;
		}
	}

	/** The megarare's colours: violet through magenta, {@code t} from 0 to 1. */
	static Color megarareColour(double t)
	{
		return Color.getHSBColor((float) (0.74 + 0.16 * t), 0.7f, 1f);
	}

	private static Color rarityColour(BossDrop.Rarity rarity, double time)
	{
		switch (rarity)
		{
			case UNCOMMON:
				return new Color(90, 200, 255);
			case RARE:
				return GOLD;
			case JACKPOT:
				// cycles through the rainbow
				return Color.getHSBColor((float) (time * 0.6 % 1.0), 0.75f, 1f);
			case MEGARARE:
				return megarareColour(0.5 + 0.5 * Math.sin(time * 3));
			default:
				return WHITE;
		}
	}

	/** Pseudo-random 0..1 from an integer, so particles move the same way every frame. */
	static double noise(int i)
	{
		double v = Math.sin(i * 12.9898 + 78.233) * 43758.5453;
		return v - Math.floor(v);
	}

	/**
	 * Behind the panel: searchlights while the reels spin; once they stop, fireworks, and light
	 * rays turning for a rare drop or a pet.
	 */
	private static void drawDropBackdrop(Ui ui, Rectangle view, BossDrop drop)
	{
		DropShow.drawSearchlights(ui.g, view, drop);
		drawRays(ui, view, drop);
		DropShow.drawFireworks(ui.g, view, drop);
	}

	private static void drawRays(Ui ui, Rectangle view, BossDrop drop)
	{
		double since = drop.getTime() - BossDrop.STOPS[BossDrop.STOPS.length - 1];
		if (since < 0 || drop.getRarity().ordinal() < BossDrop.Rarity.RARE.ordinal())
		{
			return;
		}
		Graphics2D g = ui.g;
		Composite old = g.getComposite();
		double cx = view.x + view.width / 2.0;
		double cy = view.y + view.height / 2.0;
		double reach = Math.hypot(view.width, view.height);
		boolean mega = drop.getRarity() == BossDrop.Rarity.MEGARARE;
		int rays = mega ? 20 : drop.getRarity() == BossDrop.Rarity.JACKPOT ? 16 : 10;
		float alpha = (float) Math.min(0.3, since * 0.6);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		for (int i = 0; i < rays; i++)
		{
			double a = drop.getTime() * 0.5 + i * 2 * Math.PI / rays;
			double w = Math.PI / rays * 0.5;
			int[] xs = {(int) cx, (int) (cx + Math.cos(a - w) * reach), (int) (cx + Math.cos(a + w) * reach)};
			int[] ys = {(int) cy, (int) (cy + Math.sin(a - w) * reach), (int) (cy + Math.sin(a + w) * reach)};
			g.setColor(mega ? megarareColour(i / (double) rays) : drop.getRarity() == BossDrop.Rarity.JACKPOT
				? Color.getHSBColor((float) (i / (double) rays), 0.6f, 1f) : GOLD);
			g.fillPolygon(xs, ys, 3);
		}
		g.setComposite(old);
	}

	/** The slot machine: chasing lights, three reels, the payline and, once stopped, the prize. */
	private static void drawBossDrop(Ui ui, Rectangle panel, BossDrop drop)
	{
		if (drop == null)
		{
			return;
		}
		Graphics2D g = ui.g;
		double time = drop.getTime();
		boolean spinning = drop.isSpinning();
		Color win = rarityColour(drop.getRarity(), time);

		// marquee bulbs around the panel: chase while spinning, flash together once won
		int spacing = 16;
		int perimeter = 2 * (panel.width + panel.height);
		int bulbs = perimeter / spacing;
		for (int i = 0; i < bulbs; i++)
		{
			int d = i * spacing;
			int bx;
			int by;
			if (d < panel.width)
			{
				bx = panel.x + d;
				by = panel.y + 3;
			}
			else if (d < panel.width + panel.height)
			{
				bx = panel.x + panel.width - 4;
				by = panel.y + d - panel.width;
			}
			else if (d < 2 * panel.width + panel.height)
			{
				bx = panel.x + panel.width - (d - panel.width - panel.height);
				by = panel.y + panel.height - 4;
			}
			else
			{
				bx = panel.x + 3;
				by = panel.y + panel.height - (d - 2 * panel.width - panel.height);
			}
			// the chase speeds up as the spin goes on
			double chase = time * (10 + 14 * Math.min(1, time / BossDrop.STOPS[BossDrop.STOPS.length - 1]));
			boolean blink = ui.on(Option.FLASHES);
			boolean lit = spinning ? !blink || (i + (int) chase) % 3 == 0 : !blink || ((int) (time * 6)) % 2 == 0;
			g.setColor(lit ? (spinning ? YELLOW : win) : BULB_OFF);
			g.fillOval(bx - 3, by - 3, 6, 6);
		}

		// reels
		int symbol = UiLayout.REEL_H / 2;
		for (int i = 0; i < drop.reelCount(); i++)
		{
			Rectangle r = UiLayout.reel(panel, i);
			g.setColor(REEL_BACK);
			g.fill(r);
			java.awt.Shape oldClip = g.getClip();
			g.clip(r);
			double pos = drop.reelPosition(i);
			List<BossDrop.Prize> strip = drop.reel(i);
			int centre = r.y + r.height / 2;
			boolean stopped = drop.isStopped(i);
			for (int k = (int) Math.floor(pos) - 2; k <= (int) Math.floor(pos) + 2; k++)
			{
				if (k < 0 || k >= strip.size())
				{
					continue;
				}
				int y = centre + (int) Math.round((k - pos) * symbol);
				Composite old = g.getComposite();
				if (!stopped)
				{
					// motion blur: a fainter copy trailing each symbol
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
					drawPrizeSymbol(ui, strip.get(k), r.x + r.width / 2, y - symbol / 4, r.width - 12);
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
				}
				drawPrizeSymbol(ui, strip.get(k), r.x + r.width / 2, y, r.width - 12);
				g.setComposite(old);
			}
			g.setClip(oldClip);
			// a reel flashes as it clicks into place
			double sinceStop = time - BossDrop.STOPS[i];
			if (stopped && sinceStop < 0.35 && ui.on(Option.FLASHES))
			{
				Composite old = g.getComposite();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.7 * (1 - sinceStop / 0.35))));
				g.setColor(WHITE);
				g.fill(r);
				g.setComposite(old);
			}
			if (ui.on(Option.DROP_SHOW))
			{
				if (!stopped && i == drop.reelCount() - 1 && i > 0 && drop.isStopped(i - 1))
				{
					DropShow.drawAnticipation(g, r, drop);
				}
				DropShow.drawReelLanding(g, r, drop, i, win);
			}
			Stroke oldStroke = g.getStroke();
			g.setStroke(new BasicStroke(stopped && !spinning ? 3f : 2f));
			g.setColor(stopped && !spinning ? win : PANEL_EDGE_LIGHT);
			g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
			g.setStroke(oldStroke);
		}
		// payline
		Rectangle first = UiLayout.reel(panel, 0);
		Rectangle last = UiLayout.reel(panel, drop.reelCount() - 1);
		int lineY = first.y + first.height / 2;
		g.setColor(new Color(255, 60, 40, 170));
		g.fillRect(first.x - 8, lineY - 1, last.x + last.width - first.x + 16, 2);
		int[] lx = {first.x - 10, first.x - 3, first.x - 10};
		int[] ly = {lineY - 6, lineY, lineY + 6};
		g.fillPolygon(lx, ly, 3);
		int[] rx = {last.x + last.width + 10, last.x + last.width + 3, last.x + last.width + 10};
		g.fillPolygon(rx, ly, 3);

		// the prize, once the last reel stops
		int textTop = first.y + first.height + UiLayout.GAP;
		int cx = panel.x + panel.width / 2;
		int width = panel.width - 2 * UiLayout.PAD;
		if (spinning)
		{
			int lastReel = drop.reelCount() - 1;
			if (lastReel > 0 && drop.isStopped(lastReel - 1))
			{
				// one reel to go: the hype line throbs with the heartbeat
				ui.font(TextStyle.BOLD);
				int ty = textTop + ui.metrics().getAscent() + 4;
				AffineTransform before = g.getTransform();
				double s = ui.on(Option.DROP_SHOW) ? 1 + 0.25 * DropShow.beat(time) : 1;
				g.translate(cx, ty);
				g.scale(s, s);
				g.translate(-cx, -ty);
				ui.centred("Here it comes...", cx, ty, !ui.on(Option.FLASHES) || ((int) (time * 8)) % 2 == 0 ? YELLOW : ORANGE);
				g.setTransform(before);
				return;
			}
			ui.font(TextStyle.SMALL);
			ui.centred("Click to stop the reels", cx, textTop + ui.metrics().getAscent() + 4, HINT);
			return;
		}
		ui.font(TextStyle.BOLD);
		BossDrop.Prize prize = drop.getPrize();
		// the prize name punches in big and settles
		double since = time - BossDrop.STOPS[BossDrop.STOPS.length - 1];
		int titleY = textTop + ui.metrics().getAscent();
		double punch = !ui.on(Option.DROP_SHOW) ? 1 : 1 + 0.6 * Math.exp(-since * 6) * Math.abs(Math.cos(since * 11)) + 0.06 * DropShow.beat(time);
		AffineTransform beforeTitle = g.getTransform();
		g.translate(cx, titleY);
		g.scale(punch, punch);
		g.translate(-cx, -titleY);
		ui.centredFit(prize.getTitle(), cx, titleY, width, win);
		g.setTransform(beforeTitle);
		ui.font(TextStyle.SMALL);
		int y = textTop + 18;
		for (String line : wrap(prize.getDescription(), ui.metrics(), width))
		{
			y += ui.metrics().getAscent();
			if (y > panel.y + panel.height - UiLayout.BUTTON_H - UiLayout.PAD)
			{
				break;
			}
			ui.centred(line, cx, y, YELLOW);
			y += 2;
		}
	}

	/** One symbol on a reel: the prize's icon, or a coloured stand-in. */
	private static void drawPrizeSymbol(Ui ui, BossDrop.Prize prize, int cx, int cy, int max)
	{
		BufferedImage icon = ui.assets.prizeIcon(prize);
		if (icon != null)
		{
			int w = Math.min(max, icon.getWidth() + 8);
			int h = w * icon.getHeight() / Math.max(1, icon.getWidth());
			ui.g.drawImage(icon, cx - w / 2, cy - h / 2, w, h, null);
			return;
		}
		Color c;
		switch (prize.getKind())
		{
			case COINS:
				c = GOLD;
				break;
			case LAMP:
				c = new Color(90, 200, 255);
				break;
			case GEAR:
				c = ORANGE;
				break;
			default:
				c = new Color(255, 90, 220);
				break;
		}
		ui.g.setColor(c);
		ui.g.fillOval(cx - 12, cy - 12, 24, 24);
		ui.g.setColor(Color.BLACK);
		ui.g.drawOval(cx - 12, cy - 12, 24, 24);
	}

	/**
	 * The win: a white flash as the last reel lands, then a fountain of coins and sparks that
	 * grows with the drop's rarity, and the banner bouncing in over the panel.
	 */
	/** "Level up!" bouncing above the level-up panel, with sparks bursting from behind it. */
	private static void drawLevelUpBanner(Ui ui, Rectangle view, Rectangle panel, double time)
	{
		Graphics2D g = ui.g;
		int cx = (int) panel.getCenterX();
		boolean show = ui.on(Option.LEVEL_UP_SHOW);
		float size = (float) (16 * (show ? LevelUpShow.bannerScale(time) : 2.2));
		int y = Math.max(view.y + (int) size + 30, panel.y - 10);
		if (show)
		{
			LevelUpShow.drawSparks(g, cx, y - (int) (size / 3), time);
		}
		if (size < 4)
		{
			return;
		}
		Composite old = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, LevelUpShow.bannerAlpha(time)));
		g.setFont(font(ui.assets, TextStyle.BOLD).deriveFont(size));
		String banner = "Level up!";
		int textW = g.getFontMetrics().stringWidth(banner);
		shadowText(g, banner, cx - textW / 2, y, show ? LevelUpShow.bannerColour(time) : GOLD);
		g.setComposite(old);
	}

	private static void drawCelebration(Ui ui, Rectangle view, BossDrop drop)
	{
		double since = drop.getTime() - BossDrop.STOPS[BossDrop.STOPS.length - 1];
		if (since < 0)
		{
			return;
		}
		Graphics2D g = ui.g;
		boolean show = ui.on(Option.DROP_SHOW);
		if (show)
		{
			DropShow.drawShockwave(g, view, drop, rarityColour(drop.getRarity(), drop.getTime()));
		}
		Composite old = g.getComposite();
		int rarity = drop.getRarity().ordinal();
		if (rarity >= BossDrop.Rarity.RARE.ordinal() && since < 0.45 && ui.on(Option.FLASHES))
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.7 * (1 - since / 0.45))));
			g.setColor(WHITE);
			g.fill(view);
		}
		int count = show ? new int[]{18, 36, 70, 130}[rarity] : 0;
		double cx = view.x + view.width / 2.0;
		double cy = view.y + view.height / 2.0 - 30;
		double gravity = 260;
		for (int i = 0; i < count; i++)
		{
			// particles leave in waves, so the fountain keeps going for a while
			double start = (i % 4) * 0.25 * (rarity + 1) / 2.0;
			double t = since - start;
			if (t < 0 || t > 2.2)
			{
				continue;
			}
			double angle = -Math.PI / 2 + (noise(i) - 0.5) * Math.PI * 1.4;
			double speed = 160 + noise(i + 500) * 260;
			double x = cx + Math.cos(angle) * speed * t;
			double y = cy + Math.sin(angle) * speed * t + gravity * t * t;
			float alpha = (float) Math.max(0, Math.min(1, (2.2 - t) / 0.6));
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
			boolean coin = noise(i + 900) < 0.6;
			if (coin)
			{
				// a spinning coin: an ellipse whose width flips with time
				int w = (int) Math.max(2, Math.abs(Math.cos(t * 12 + i)) * 8);
				g.setColor(GOLD);
				g.fillOval((int) x - w / 2, (int) y - 4, w, 8);
				g.setColor(new Color(150, 100, 0));
				g.drawOval((int) x - w / 2, (int) y - 4, w, 8);
			}
			else
			{
				Color c = rarity == BossDrop.Rarity.MEGARARE.ordinal() ? megarareColour(noise(i + 70))
					: rarity == BossDrop.Rarity.JACKPOT.ordinal() ? Color.getHSBColor((float) noise(i + 70), 0.7f, 1f)
					: rarityColour(drop.getRarity(), drop.getTime());
				g.setColor(c);
				int r = 2 + (int) (noise(i + 300) * 3);
				g.fillRect((int) x - r, (int) y - 1, r * 2, 2);
				g.fillRect((int) x - 1, (int) y - r, 2, r * 2);
			}
		}
		g.setComposite(old);

		// the banner bounces in above the panel
		double bounce = show ? (1 + 0.8 * Math.exp(-since * 6) * Math.abs(Math.cos(since * 14))) * Math.min(1, since * 6) : 1;
		float size = (float) ((18 + 6 * rarity) * bounce);
		if (size < 4)
		{
			return;
		}
		Font base = font(ui.assets, TextStyle.BOLD);
		g.setFont(base.deriveFont(size));
		String banner = drop.getRarity().getBanner();
		int textW = g.getFontMetrics().stringWidth(banner);
		int y = (int) (view.y + view.height / 2.0 - UiLayout.REEL_H - 40);
		shadowText(g, banner, (int) (cx - textW / 2.0), Math.max(view.y + (int) size + 4, y), rarityColour(drop.getRarity(), drop.getTime()));
	}

	private static void drawCard(Ui ui, UiLayout.Button b, Offer offer, Build build, boolean hover)
	{
		Combination combination = offer.getGear() == null ? null : Combination.making(offer.getGear());
		Rectangle r = b.bounds;
		// a strip along the top in the offer's colour: rarity for gear, ability or prayer for passives
		Color strip = combination != null ? GOLD : offer.getGear() != null ? rarityColour(offer.getGear().getRarity(), ui.time)
			: offer.getPassive() != null ? (offer.getPassive().isAbility() ? ABILITY_STRIP : PASSIVE_STRIP) : SUPPLY_STRIP;
		ui.g.setColor(strip);
		ui.g.fillRect(r.x + 3, r.y + 3, r.width - 6, 3);
		ui.g.setColor(new Color(strip.getRed(), strip.getGreen(), strip.getBlue(), hover ? 40 : 18));
		ui.g.fillRect(r.x + 3, r.y + 6, r.width - 6, 34);
		int cx = r.x + r.width / 2;
		int y = r.y + 8;
		BufferedImage icon = ui.assets.offerIcon(offer);
		if (icon != null)
		{
			ui.g.drawImage(icon, cx - icon.getWidth() / 2, y + Math.max(0, (32 - icon.getHeight()) / 2), null);
		}
		y += 36;

		ui.font(TextStyle.BOLD);
		for (String line : wrap(offer.getTitle(), ui.metrics(), r.width - 10))
		{
			y += ui.metrics().getAscent();
			ui.centred(line, cx, y, hover ? WHITE : ORANGE);
			y += 2;
		}
		ui.font(TextStyle.SMALL);
		y += 2;
		// what goes into a combination, or what this passive will combine into
		String recipe = null;
		if (combination != null)
		{
			recipe = combination.recipe();
		}
		else if (offer.getPassive() != null)
		{
			Combination leads = Combination.from(build.get(Slot.WEAPON), offer.getPassive());
			if (leads != null)
			{
				recipe = "At max rank, makes " + leads.getResult().getTitle();
			}
		}
		if (recipe != null)
		{
			for (String line : wrap(recipe, ui.metrics(), r.width - 12))
			{
				y += ui.metrics().getAscent();
				ui.centred(line, cx, y, GOLD);
				y += 2;
			}
			y += 2;
		}
		for (String line : wrap(offer.getDescription(), ui.metrics(), r.width - 12))
		{
			y += ui.metrics().getAscent();
			if (y > r.y + r.height - 4)
			{
				break;
			}
			ui.centred(line, cx, y, YELLOW);
			y += 2;
		}
		ui.text(String.valueOf(b.index + 1), r.x + 5, r.y + 4 + ui.metrics().getAscent(), HINT);
		if (offer.getGear() != null && combination == null)
		{
			// the item's rarity: its name in the corner and a border in its colour
			ItemRarity rarity = offer.getGear().getRarity();
			Color colour = rarityColour(rarity, ui.time);
			if (rarity.ordinal() >= ItemRarity.RARE.ordinal())
			{
				Stroke oldStroke = ui.g.getStroke();
				ui.g.setStroke(new BasicStroke(rarity.ordinal() >= ItemRarity.LEGENDARY.ordinal() ? 2f : 1f));
				ui.g.setColor(colour);
				ui.g.drawRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3);
				ui.g.setStroke(oldStroke);
			}
			ui.right(rarity.getTitle(), r.x + r.width - 5, r.y + 4 + ui.metrics().getAscent(), colour);
		}
		if (combination != null)
		{
			Stroke oldStroke = ui.g.getStroke();
			ui.g.setStroke(new BasicStroke(2f));
			ui.g.setColor(GOLD);
			ui.g.drawRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3);
			ui.g.setStroke(oldStroke);
			ui.font(TextStyle.SMALL);
			ui.right("Combination!", r.x + r.width - 5, r.y + 4 + ui.metrics().getAscent(), GOLD);
		}
	}

	private static void drawPanel(Ui ui, Rectangle r)
	{
		Graphics2D g = ui.g;
		// a soft shadow lifts the panel off the game
		for (int i = 1; i <= 3; i++)
		{
			g.setColor(new Color(0, 0, 0, 50 - i * 12));
			g.fillRect(r.x - i + 3, r.y - i + 5, r.width + 2 * i, r.height + 2 * i);
		}
		g.setColor(PANEL);
		g.fill(r);
		if (r.height > UiLayout.TITLE_H * 2)
		{
			// the title's band, a shade darker, with an engraved line under it
			g.setColor(PANEL_BAND);
			g.fillRect(r.x + 2, r.y + 2, r.width - 4, UiLayout.TITLE_H - 6);
			g.setColor(PANEL_EDGE_DARK);
			g.drawLine(r.x + 6, r.y + UiLayout.TITLE_H - 4, r.x + r.width - 7, r.y + UiLayout.TITLE_H - 4);
			g.setColor(new Color(110, 98, 78, 120));
			g.drawLine(r.x + 6, r.y + UiLayout.TITLE_H - 3, r.x + r.width - 7, r.y + UiLayout.TITLE_H - 3);
		}
		g.setColor(PANEL_EDGE_DARK);
		g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
		g.setColor(PANEL_EDGE_LIGHT);
		g.drawRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3);
		BufferedImage tl = ui.assets.sprite(UiSprite.BORDER_TOP_LEFT);
		BufferedImage tr = ui.assets.sprite(UiSprite.BORDER_TOP_RIGHT);
		BufferedImage bl = ui.assets.sprite(UiSprite.BORDER_BOTTOM_LEFT);
		BufferedImage br = ui.assets.sprite(UiSprite.BORDER_BOTTOM_RIGHT);
		if (tl != null && tr != null && bl != null && br != null)
		{
			g.drawImage(tl, r.x, r.y, null);
			g.drawImage(tr, r.x + r.width - tr.getWidth(), r.y, null);
			g.drawImage(bl, r.x, r.y + r.height - bl.getHeight(), null);
			g.drawImage(br, r.x + r.width - br.getWidth(), r.y + r.height - br.getHeight(), null);
		}
	}

	/** A button in the game's carved-stone style (nine-slice), lighter while hovered. */
	private static void drawStoneButton(Ui ui, Rectangle r, boolean hover)
	{
		drawStoneButton(ui, r, hover, false, true);
	}

	/**
	 * A stone button: lighter with a gold edge while hovered, darker while pressed, dull when it
	 * can't be used.
	 */
	private static void drawStoneButton(Ui ui, Rectangle r, boolean hover, boolean pressed, boolean enabled)
	{
		Graphics2D g = ui.g;
		g.setColor(!enabled ? STONE_DISABLED : pressed ? STONE_PRESSED : hover ? STONE_HOVER : STONE);
		g.fill(r);
		drawStoneEdges(ui, r, hover);
		if (hover)
		{
			g.setColor(pressed ? new Color(255, 200, 40, 90) : new Color(255, 200, 40, 150));
			g.drawRect(r.x + 2, r.y + 2, r.width - 5, r.height - 5);
		}
		if (pressed)
		{
			// pressed in: shade along the top edge
			g.setColor(new Color(0, 0, 0, 70));
			g.fillRect(r.x + 3, r.y + 3, r.width - 6, 2);
		}
	}

	private static void drawStoneEdges(Ui ui, Rectangle r, boolean hover)
	{
		Graphics2D g = ui.g;
		BufferedImage tl = ui.assets.sprite(UiSprite.BUTTON_TOP_LEFT);
		BufferedImage tr = ui.assets.sprite(UiSprite.BUTTON_TOP_RIGHT);
		BufferedImage bl = ui.assets.sprite(UiSprite.BUTTON_BOTTOM_LEFT);
		BufferedImage br = ui.assets.sprite(UiSprite.BUTTON_BOTTOM_RIGHT);
		BufferedImage left = ui.assets.sprite(UiSprite.BUTTON_LEFT);
		BufferedImage top = ui.assets.sprite(UiSprite.BUTTON_TOP);
		BufferedImage right = ui.assets.sprite(UiSprite.BUTTON_RIGHT);
		BufferedImage bottom = ui.assets.sprite(UiSprite.BUTTON_BOTTOM);
		if (tl == null || tr == null || bl == null || br == null || left == null || top == null || right == null || bottom == null
			|| r.width < tl.getWidth() + tr.getWidth() || r.height < tl.getHeight() + bl.getHeight())
		{
			g.setColor(PANEL_EDGE_DARK);
			g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
			g.setColor(hover ? WHITE : PANEL_EDGE_LIGHT);
			g.drawRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3);
			return;
		}
		int x2 = r.x + r.width;
		int y2 = r.y + r.height;
		g.drawImage(top, r.x + tl.getWidth(), r.y, r.width - tl.getWidth() - tr.getWidth(), top.getHeight(), null);
		g.drawImage(bottom, r.x + bl.getWidth(), y2 - bottom.getHeight(), r.width - bl.getWidth() - br.getWidth(), bottom.getHeight(), null);
		g.drawImage(left, r.x, r.y + tl.getHeight(), left.getWidth(), r.height - tl.getHeight() - bl.getHeight(), null);
		g.drawImage(right, x2 - right.getWidth(), r.y + tr.getHeight(), right.getWidth(), r.height - tr.getHeight() - br.getHeight(), null);
		g.drawImage(tl, r.x, r.y, null);
		g.drawImage(tr, x2 - tr.getWidth(), r.y, null);
		g.drawImage(bl, r.x, y2 - bl.getHeight(), null);
		g.drawImage(br, x2 - br.getWidth(), y2 - br.getHeight(), null);
	}

	/** Text helpers bound to one graphics context and asset set. */
	private static final class Ui
	{
		final Graphics2D g;
		final GameAssets assets;
		final Options options;
		/** Real seconds for little animations on the menus (the level-up show's, or the start menu's). */
		final double time;
		/** Where the mouse is (or null), and whether its button is held. */
		final Point mouse;
		final boolean mouseDown;

		Ui(Graphics2D g, GameAssets assets, Options options, double time)
		{
			this(g, assets, options, time, null, false);
		}

		Ui(Graphics2D g, GameAssets assets, Options options, double time, Point mouse, boolean mouseDown)
		{
			this.g = g;
			this.assets = assets;
			this.options = options;
			this.time = time;
			this.mouse = mouse;
			this.mouseDown = mouseDown;
		}

		boolean on(Option option)
		{
			return options.isOn(option);
		}

		void font(TextStyle style)
		{
			g.setFont(GameRenderer.font(assets, style));
		}

		FontMetrics metrics()
		{
			return g.getFontMetrics();
		}

		void text(String s, int x, int y, Color color)
		{
			shadowText(g, s, x, y, color);
		}

		void centred(String s, int cx, int y, Color color)
		{
			text(s, cx - metrics().stringWidth(s) / 2, y, color);
		}

		/** Centred, shrunk to the small font if it would not fit. */
		void centredFit(String s, int cx, int y, int width, Color color)
		{
			if (metrics().stringWidth(s) > width)
			{
				font(TextStyle.SMALL);
			}
			centred(s, cx, y, color);
		}

		void right(String s, int rightX, int y, Color color)
		{
			text(s, rightX - metrics().stringWidth(s), y, color);
		}
	}

	static Font font(GameAssets assets, TextStyle style)
	{
		Font f = assets.font(style);
		if (f != null)
		{
			return f;
		}
		switch (style)
		{
			case BOLD:
				return new Font(Font.SANS_SERIF, Font.BOLD, 13);
			case SMALL:
				return new Font(Font.SANS_SERIF, Font.PLAIN, 11);
			default:
				return new Font(Font.SANS_SERIF, Font.PLAIN, 13);
		}
	}

	/** The game's text style: a hard one-pixel black shadow. */
	static void shadowText(Graphics2D g, String s, int x, int y, Color color)
	{
		g.setColor(Color.BLACK);
		g.drawString(s, x + 1, y + 1);
		g.setColor(color);
		g.drawString(s, x, y);
	}

	private static Color kindColor(EnemyKind kind)
	{
		switch (kind)
		{
			case RAT:
				return new Color(120, 100, 80);
			case SKELETON:
				return new Color(220, 215, 200);
			case MOSS_GIANT:
				return new Color(90, 130, 60);
			case GREATER_DEMON:
				return new Color(150, 30, 30);
			case GREEN_DRAGON:
				return new Color(40, 140, 50);
			default:
				return new Color(110, 140, 100);
		}
	}

	/** Deterministic per-tile shade, so the field has the game's patchy tiled look. */
	static int tileShade(int tx, int ty)
	{
		int h = tx * 73856093 ^ ty * 19349663;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return (h & 0x7fffffff) % GRASS.length;
	}

	private static Color[] grass()
	{
		Color[] shades = new Color[8];
		for (int i = 0; i < shades.length; i++)
		{
			double t = i / (double) (shades.length - 1);
			shades[i] = new Color((int) (74 + 7 * t), (int) (98 + 9 * t), (int) (40 + 4 * t));
		}
		return shades;
	}

	static String formatTime(double seconds)
	{
		int s = (int) seconds;
		return String.format("%d:%02d", s / 60, s % 60);
	}

	/** Cuts text to fit a width, adding an ellipsis. */
	static String fit(String text, FontMetrics fm, int width)
	{
		if (fm.stringWidth(text) <= width)
		{
			return text;
		}
		String s = text;
		while (s.length() > 1 && fm.stringWidth(s + "...") > width)
		{
			s = s.substring(0, s.length() - 1);
		}
		return s + "...";
	}

	static List<String> wrap(String text, FontMetrics fm, int width)
	{
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			if (line.length() > 0 && fm.stringWidth(line + " " + word) > width)
			{
				lines.add(line.toString());
				line.setLength(0);
			}
			if (line.length() > 0)
			{
				line.append(' ');
			}
			line.append(word);
		}
		if (line.length() > 0)
		{
			lines.add(line.toString());
		}
		return lines;
	}
}
