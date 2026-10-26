package com.osrssurvivors.render;

import com.osrssurvivors.game.Abilities;
import com.osrssurvivors.game.Arena;
import com.osrssurvivors.game.BossDrop;
import com.osrssurvivors.game.Build;
import com.osrssurvivors.game.Casket;
import com.osrssurvivors.game.Combination;
import com.osrssurvivors.game.Effects;
import com.osrssurvivors.game.Enemy;
import com.osrssurvivors.game.EnemyKind;
import com.osrssurvivors.game.EnemyShot;
import com.osrssurvivors.game.GameSession;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.GearSet;
import com.osrssurvivors.game.Location;
import com.osrssurvivors.game.MetaProgress;
import com.osrssurvivors.game.Offer;
import com.osrssurvivors.game.Passive;
import com.osrssurvivors.game.Perk;
import com.osrssurvivors.game.Pet;
import com.osrssurvivors.game.Phase;
import com.osrssurvivors.game.Player;
import com.osrssurvivors.game.Progression;
import com.osrssurvivors.game.Slot;
import com.osrssurvivors.game.Strike;
import com.osrssurvivors.game.TitleScreen;
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
	static final String CREDITS = "Free unofficial fan game, not affiliated with Jagex. Old School RuneScape and its artwork are the property of Jagex Ltd.";
	static final Color PANEL = new Color(62, 53, 41, 240);
	static final Color PANEL_EDGE_DARK = new Color(24, 20, 15);
	static final Color PANEL_EDGE_LIGHT = new Color(110, 98, 78);
	static final Color STONE = new Color(78, 68, 55);
	static final Color STONE_HOVER = new Color(104, 91, 73);
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
	private static final Color GOD = new Color(255, 215, 90);
	private static final Color GOLD = new Color(255, 200, 40);
	private static final Color GREEN = new Color(0, 230, 0);
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
			Ui ui = new Ui(g, assets);
			new World(view, session, assets).draw(g);
			if (session.getPhase() != Phase.TITLE)
			{
				drawHud(ui, view, session, exitKey);
			}
			if (session.getPhase() != Phase.RUNNING)
			{
				g.setColor(SHADE);
				g.fill(view);
				BossDrop drop = session.getPhase() == Phase.BOSS_DROP ? session.getBossDrop() : null;
				Rectangle panel = drop == null ? null : UiLayout.panel(view, session);
				double levelUp = session.getPhase() == Phase.LEVEL_UP && !session.isCasketOpen() ? session.getLevelUpTime() : -1;
				Rectangle levelPanel = LevelUpShow.isPlaying(levelUp) ? UiLayout.panel(view, session) : null;
				if (drop != null)
				{
					drawDropBackdrop(ui, view, drop);
				}
				if (levelPanel != null)
				{
					LevelUpShow.drawBehind(g, view, levelPanel, levelUp);
				}
				AffineTransform plain = g.getTransform();
				if (panel != null)
				{
					// the machine pumps with a heartbeat and shakes as the reels land
					g.transform(DropShow.transform(drop, panel));
				}
				if (levelPanel != null)
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
		}
		finally
		{
			g.dispose();
		}
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
		int playerTop;

		World(Rectangle view, GameSession session, GameAssets assets)
		{
			this.view = view;
			this.session = session;
			this.assets = assets;
			this.scale = scale(view);
			Player p = session.getPlayer();
			this.originX = view.x + view.width / 2.0 - p.getX() * scale;
			this.originY = view.y + view.height / 2.0 - p.getY() * scale;
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
			drawBursts(g);
			drawChampionRings(g);
			drawGems(g);
			drawCaskets(g);
			drawSorted(g);
			drawPets(g);
			drawStrikes(g);
			drawShots(g);
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
				if (!visible(b.getX(), b.getY(), b.getRadius()))
				{
					continue;
				}
				float fade = (float) (1 - b.getAge());
				int r = len(b.getRadius() * (0.6 + 0.4 * b.getAge()));
				int x = sx(b.getX());
				int y = sy(b.getY());
				Color c = burstColour(b.getType());
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f * fade));
				g.setColor(c);
				g.fillOval(x - r, y - r / 2, r * 2, r);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f * fade));
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
			for (Casket c : session.getCaskets())
			{
				int x = sx(c.getX());
				int y = sy(c.getY());
				// a slow bob so the loot catches the eye
				int bob = (int) Math.round(Math.sin(session.getElapsed() * 4) * 3);
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

		private void drawGems(Graphics2D g)
		{
			int size = Math.max(10, len(XpOrb.RADIUS * 3.2));
			for (XpOrb orb : session.getOrbs())
			{
				if (!visible(orb.getX(), orb.getY(), XpOrb.RADIUS * 3))
				{
					continue;
				}
				int x = sx(orb.getX());
				int y = sy(orb.getY());
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
			}
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
						g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) c.getStrength()));
						ActorSprite s = assets.enemy(c.getKind(), c.getRadius() > c.getKind().getRadius(), c.getFacing(), false, 0, scale);
						drawActor(g, s, c.getX(), c.getY(), c.getRadius(), kindColor(c.getKind()), 0);
						g.setComposite(old);
					});
				}
			}
			List<Runnable> onTop = new ArrayList<>();
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
						int top = drawActor(g, s, e.getX(), e.getY(), e.getRadius(), e.isFlashing() ? WHITE : kindColor(e.getKind()), 0);
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
			int x = sx(wx);
			int y = sy(wy);
			int r = len(radius);
			g.setColor(SHADOW);
			g.fillOval(x - r, y - r / 2, r * 2, r);
			if (sprite != null)
			{
				int top = y - sprite.anchorY + bob;
				g.drawImage(sprite.image, x - sprite.anchorX, top, null);
				return top;
			}
			g.setColor(fallback);
			g.fillOval(x - r, y - r * 2, r * 2, r * 2);
			g.setColor(Color.BLACK);
			g.drawOval(x - r, y - r * 2, r * 2, r * 2);
			return y - r * 2;
		}

		private void drawStrikes(Graphics2D g)
		{
			Stroke oldStroke = g.getStroke();
			Composite oldComposite = g.getComposite();
			for (Strike s : session.getStrikes())
			{
				double t = 1 - s.getStrength();
				Gear.Missile missile = s.weapon == null ? Gear.Missile.NONE : s.weapon.getMissile();
				if (s.source == Strike.Source.CANNON || s.source == Strike.Source.CHINCHOMPA || missile == Gear.Missile.CHINCHOMPA)
				{
					// lobbed: cannonballs and chinchompas
					double wx = s.fromX + (s.toX - s.fromX) * t;
					double wy = s.fromY + (s.toY - s.fromY) * t - 12 - Math.sin(t * Math.PI) * (s.source == Strike.Source.CANNON ? 6 : 30);
					BufferedImage icon = assets.sprite(s.source == Strike.Source.CANNON ? UiSprite.CANNONBALL : UiSprite.CHINCHOMPA);
					int x = sx(wx);
					int y = sy(wy);
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
				if (missile == Gear.Missile.NONE)
				{
					// a melee swing: a quick arc across the target
					int x = sx(s.toX);
					int y = sy(s.toY - 10);
					int r = len(14);
					double angle = Math.toDegrees(Math.atan2(-(s.toY - s.fromY), s.toX - s.fromX));
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) s.getStrength()));
					g.setColor(SLASH);
					g.setStroke(new BasicStroke(Math.max(2f, (float) scale * 2)));
					g.drawArc(x - r, y - r, r * 2, r * 2, (int) angle - 60, 120);
					g.setComposite(oldComposite);
					continue;
				}
				// projectiles fly from chest height on the player to the target
				double wx = s.fromX + (s.toX - s.fromX) * t;
				double wy = s.fromY + (s.toY - s.fromY) * t - 12;
				int x = sx(wx);
				int y = sy(wy);
				if (missile == Gear.Missile.SPELL)
				{
					BufferedImage spell = assets.projectile(s.weapon);
					int size = Math.max(12, len(10));
					if (spell != null)
					{
						g.drawImage(spell, x - size / 2, y - size / 2, size, size, null);
					}
					else
					{
						g.setColor(SPELL_FALLBACK);
						g.fillOval(x - size / 4, y - size / 4, size / 2, size / 2);
					}
					continue;
				}
				double dx = s.toX - s.fromX;
				double dy = s.toY - s.fromY;
				double d = Math.max(1, Math.hypot(dx, dy));
				double length = missile == Gear.Missile.DART ? 5 : 9;
				int tailX = sx(wx - dx / d * length);
				int tailY = sy(wy - dy / d * length);
				g.setStroke(new BasicStroke(Math.max(1.5f, (float) scale)));
				g.setColor(SHAFT);
				g.drawLine(tailX, tailY, x, y);
				g.setColor(ARROWHEAD);
				g.fillRect(x - 1, y - 1, 3, 3);
			}
			g.setStroke(oldStroke);
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
				int n = stack.merge(key, 1, Integer::sum) - 1;
				Integer top = e == null ? Integer.valueOf(playerTop) : heads.get(e);
				int x = sx(ex) + (n % 2 == 1 ? -10 : 0);
				int y = (top != null ? (top + sy(ey)) / 2 : sy(ey) - len(radius)) - (n / 2) * 14 - (n % 2) * 7;
				String text = String.valueOf(h.getDamage());
				BufferedImage image = h.isSpecial() && specialSplat != null ? specialSplat : splat;
				if (image != null)
				{
					g.drawImage(image, x - image.getWidth() / 2, y - image.getHeight() / 2, null);
				}
				else
				{
					g.setColor(new Color(170, 0, 0));
					g.fillOval(x - 10, y - 9, 20, 18);
				}
				shadowText(g, text, x - fm.stringWidth(text) / 2, y + fm.getAscent() / 2 - 1, h.isSpecial() ? YELLOW : WHITE);
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

	private static void drawHud(Ui ui, Rectangle view, GameSession session, String exitKey)
	{
		Player player = session.getPlayer();
		Progression prog = session.getProgression();
		Graphics2D g = ui.g;

		// hitpoints orb top left, current weapon beside it
		int orbRight = drawHpOrb(ui, view.x + 6, view.y + 6, player);
		Gear weapon = session.getWeapon().getGear();
		BufferedImage weaponIcon = weapon == null ? null : ui.assets.gearIcon(weapon);
		if (weaponIcon != null)
		{
			g.drawImage(weaponIcon, orbRight + 4, view.y + 6, null);
		}

		// level and XP bar, top centre
		int barW = Math.min(260, view.width / 2);
		int barH = 16;
		int barX = view.x + (view.width - barW) / 2;
		int barY = view.y + 8;
		g.setColor(BAR_BACK);
		g.fillRect(barX, barY, barW, barH);
		g.setColor(XP_FILL);
		g.fillRect(barX + 1, barY + 1, (int) Math.round((barW - 2) * Math.min(1.0, (double) prog.getXp() / prog.getXpToNext())), barH - 2);
		g.setColor(PANEL_EDGE_DARK);
		g.drawRect(barX, barY, barW, barH);
		BufferedImage skill = ui.assets.sprite(UiSprite.XP_SKILL_ICON);
		if (skill != null)
		{
			g.drawImage(skill, barX - skill.getWidth() - 3, barY + (barH - skill.getHeight()) / 2, null);
		}
		ui.font(TextStyle.SMALL);
		ui.centred("Level " + prog.getLevel() + "   " + prog.getXp() + " / " + prog.getXpToNext() + " xp",
			barX + barW / 2, barY + barH - 3, WHITE);

		// time and kills, top right
		ui.font(TextStyle.BOLD);
		int right = view.x + view.width - 8;
		int lineH = ui.metrics().getHeight();
		ui.right(formatTime(session.getElapsed()), right, view.y + 6 + ui.metrics().getAscent(), YELLOW);
		ui.font(TextStyle.REGULAR);
		ui.right("Kills: " + session.getKills(), right, view.y + 6 + lineH + ui.metrics().getAscent(), WHITE);
		ui.font(TextStyle.SMALL);
		ui.right(session.getLocation().getTitle(), right, view.y + 6 + lineH * 2 + ui.metrics().getAscent(), HINT);
		ui.right("Coins: " + session.coinsSoFar(), right, view.y + 6 + lineH * 3 + ui.metrics().getAscent() - 4, YELLOW);
		drawChampion(ui, session, barX, barY + barH + 6, barW);

		// XP drops float up the right side, like the game's own
		ui.font(TextStyle.REGULAR);
		int dropBase = view.y + 6 + lineH * 4;
		for (Effects.XpDrop d : session.getXpDrops())
		{
			double age = d.getAge();
			int y = (int) Math.round(dropBase + 50 - age * 50);
			Composite old = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, (1 - age) * 3)));
			String text = "+" + d.getAmount();
			int textX = right - ui.metrics().stringWidth(text);
			if (skill != null)
			{
				g.drawImage(skill, textX - skill.getWidth() - 2, y - skill.getHeight() + 2, null);
			}
			ui.text(text, textX, y, WHITE);
			g.setComposite(old);
		}

		ui.font(TextStyle.SMALL);
		ui.centred("WASD / arrows: move   P / Esc: pause   " + exitKey + ": save & exit",
			view.x + view.width / 2, view.y + view.height - 6, HINT);
	}

	/** The living champion's name and health under the XP bar, or a warning that one is coming. */
	private static void drawChampion(Ui ui, GameSession session, int x, int y, int width)
	{
		Graphics2D g = ui.g;
		Enemy champion = session.getChampion();
		ui.font(TextStyle.BOLD);
		if (champion != null)
		{
			int h = 12;
			int top = y + ui.metrics().getAscent() + 2;
			ui.centred(champion.getTitle(), x + width / 2, y + ui.metrics().getAscent(), ORANGE);
			g.setColor(BAR_BACK);
			g.fillRect(x, top, width, h);
			g.setColor(CHAMPION_BAR);
			g.fillRect(x + 1, top + 1, (int) Math.round((width - 2) * Math.max(0, champion.getHealth() / champion.getMaxHealth())), h - 2);
			g.setColor(PANEL_EDGE_DARK);
			g.drawRect(x, top, width, h);
			return;
		}
		double countdown = session.getChampionCountdown();
		if (countdown > 0 && countdown <= GameSession.CHAMPION_WARNING)
		{
			ui.centred("A champion approaches! (" + (int) Math.ceil(countdown) + ")", x + width / 2, y + ui.metrics().getAscent(), RED);
		}
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

		String title;
		Color titleColor = ORANGE;
		switch (session.getPhase())
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
					: session.getTitleScreen() == TitleScreen.PETS ? "Pets" : "OSRS Survivors";
				break;
			case BOSS_DROP:
				// the rarity's banner bounces in above the panel once the reels stop
				title = "The champion's casket";
				break;
			default:
				title = "Paused";
				break;
		}
		ui.font(TextStyle.BOLD);
		ui.centredFit(title, panel.x + panel.width / 2, panel.y + 24, panel.width - 2 * UiLayout.PAD, titleColor);

		switch (session.getPhase())
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
						boolean open = l.isUnlocked(session.getBestTimes());
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
				else
				{
					MetaProgress meta = session.getMeta();
					int pets = meta == null ? 0 : meta.getPets().size();
					line = pets > 0 ? "Pets: " + pets + " / " + Pet.values().length + ". Champions drop the rest, if you're lucky."
						: "Pick a place, gear up from your bare fists, survive.";
				}
				ui.centredFit(line, panel.x + panel.width / 2, panel.y + UiLayout.TITLE_H + 6, panel.width - 2 * UiLayout.PAD, colour);
				break;
			case PAUSED:
				drawLoadout(ui, panel, session);
				break;
			default:
				break;
		}

		for (UiLayout.Button b : UiLayout.buttons(view, session))
		{
			boolean hover = mouse != null && b.bounds.contains(mouse);
			drawStoneButton(ui, b.bounds, hover);
			if (b.action == UiLayout.Action.CHOOSE_UPGRADE)
			{
				drawCard(ui, b, session.getChoices().get(b.index), session.getBuild(), hover);
			}
			else if (b.action == UiLayout.Action.CHOOSE_LOCATION)
			{
				Location location = Location.values()[b.index];
				drawLocationCard(ui, b, location, session.getBestTimes().get(location), hover,
					location.isUnlocked(session.getBestTimes()));
			}
			else if (b.action == UiLayout.Action.BUY_PERK)
			{
				drawPerkCard(ui, b, Perk.values()[b.index], session.getMeta(), hover);
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
					b.bounds.y + (b.bounds.height + ui.metrics().getAscent()) / 2 - 2, b.bounds.width - 8, hover ? WHITE : ORANGE);
			}
		}
		if (session.getPhase() == Phase.PAUSED)
		{
			drawLoadoutTooltip(ui, view, panel, session, mouse);
		}
		if (session.getPhase() == Phase.TITLE && session.getTitleScreen() == TitleScreen.MAIN)
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
				title = t.gear.getTitle();
				body = t.gear.describe();
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
			drawTooltip(ui, view, mouse, title, body);
			return;
		}
	}

	private static void drawTooltip(Ui ui, Rectangle view, Point mouse, String title, String body)
	{
		Graphics2D g = ui.g;
		int width = 210;
		ui.font(TextStyle.SMALL);
		List<String> lines = wrap(body, ui.metrics(), width - 12);
		int line = ui.metrics().getHeight();
		int height = 8 + line + lines.size() * line + 6;
		int x = Math.min(mouse.x + 14, view.x + view.width - width - 2);
		int y = Math.min(mouse.y + 14, view.y + view.height - height - 2);
		g.setColor(new Color(20, 17, 12, 235));
		g.fillRect(x, y, width, height);
		g.setColor(PANEL_EDGE_LIGHT);
		g.drawRect(x, y, width - 1, height - 1);
		ui.font(TextStyle.BOLD);
		ui.text(fit(title, ui.metrics(), width - 12), x + 6, y + 4 + ui.metrics().getAscent(), ORANGE);
		ui.font(TextStyle.SMALL);
		int ty = y + 6 + line;
		for (String l : lines)
		{
			ty += ui.metrics().getAscent();
			ui.text(l, x + 6, ty, YELLOW);
			ty += line - ui.metrics().getAscent();
		}
	}

	/** A pet on the pets page: its picture, name and bonus, faded until it's been won. */
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
		int rays = drop.getRarity() == BossDrop.Rarity.JACKPOT ? 16 : 10;
		float alpha = (float) Math.min(0.3, since * 0.6);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		for (int i = 0; i < rays; i++)
		{
			double a = drop.getTime() * 0.5 + i * 2 * Math.PI / rays;
			double w = Math.PI / rays * 0.5;
			int[] xs = {(int) cx, (int) (cx + Math.cos(a - w) * reach), (int) (cx + Math.cos(a + w) * reach)};
			int[] ys = {(int) cy, (int) (cy + Math.sin(a - w) * reach), (int) (cy + Math.sin(a + w) * reach)};
			g.setColor(drop.getRarity() == BossDrop.Rarity.JACKPOT ? Color.getHSBColor((float) (i / (double) rays), 0.6f, 1f) : GOLD);
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
			boolean lit = spinning ? (i + (int) chase) % 3 == 0 : ((int) (time * 6)) % 2 == 0;
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
			if (stopped && sinceStop < 0.35)
			{
				Composite old = g.getComposite();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.7 * (1 - sinceStop / 0.35))));
				g.setColor(WHITE);
				g.fill(r);
				g.setComposite(old);
			}
			if (!stopped && i == drop.reelCount() - 1 && i > 0 && drop.isStopped(i - 1))
			{
				DropShow.drawAnticipation(g, r, drop);
			}
			DropShow.drawReelLanding(g, r, drop, i, win);
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
				double s = 1 + 0.25 * DropShow.beat(time);
				g.translate(cx, ty);
				g.scale(s, s);
				g.translate(-cx, -ty);
				ui.centred("Here it comes...", cx, ty, ((int) (time * 8)) % 2 == 0 ? YELLOW : ORANGE);
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
		double punch = 1 + 0.6 * Math.exp(-since * 6) * Math.abs(Math.cos(since * 11)) + 0.06 * DropShow.beat(time);
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
		float size = (float) (16 * LevelUpShow.bannerScale(time));
		int y = Math.max(view.y + (int) size + 30, panel.y - 10);
		LevelUpShow.drawSparks(g, cx, y - (int) (size / 3), time);
		if (size < 4)
		{
			return;
		}
		Composite old = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, LevelUpShow.bannerAlpha(time)));
		g.setFont(font(ui.assets, TextStyle.BOLD).deriveFont(size));
		String banner = "Level up!";
		int textW = g.getFontMetrics().stringWidth(banner);
		shadowText(g, banner, cx - textW / 2, y, LevelUpShow.bannerColour(time));
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
		DropShow.drawShockwave(g, view, drop, rarityColour(drop.getRarity(), drop.getTime()));
		Composite old = g.getComposite();
		int rarity = drop.getRarity().ordinal();
		if (rarity >= BossDrop.Rarity.RARE.ordinal() && since < 0.45)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.7 * (1 - since / 0.45))));
			g.setColor(WHITE);
			g.fill(view);
		}
		int count = new int[]{18, 36, 70, 130}[rarity];
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
				Color c = rarity == BossDrop.Rarity.JACKPOT.ordinal() ? Color.getHSBColor((float) noise(i + 70), 0.7f, 1f)
					: rarityColour(drop.getRarity(), drop.getTime());
				g.setColor(c);
				int r = 2 + (int) (noise(i + 300) * 3);
				g.fillRect((int) x - r, (int) y - 1, r * 2, 2);
				g.fillRect((int) x - 1, (int) y - r, 2, r * 2);
			}
		}
		g.setComposite(old);

		// the banner bounces in above the panel
		double bounce = 1 + 0.8 * Math.exp(-since * 6) * Math.abs(Math.cos(since * 14));
		float size = (float) ((18 + 6 * rarity) * Math.min(1, since * 6) * bounce);
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
		g.setColor(PANEL);
		g.fill(r);
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
		Graphics2D g = ui.g;
		g.setColor(hover ? STONE_HOVER : STONE);
		g.fill(r);
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

		Ui(Graphics2D g, GameAssets assets)
		{
			this.g = g;
			this.assets = assets;
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
