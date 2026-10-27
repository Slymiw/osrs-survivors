package com.osrssurvivors.render;

import com.osrssurvivors.game.Cue;
import com.osrssurvivors.game.Enemy;
import com.osrssurvivors.game.GameSession;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.Option;
import com.osrssurvivors.game.Options;
import com.osrssurvivors.game.Passive;
import com.osrssurvivors.game.Player;
import com.osrssurvivors.game.Style;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Screen effects that answer what happens in a run: sparks where hits land, a puff where monsters
 * fall, shockwaves for champions and area spells, a red edge when the player is hurt, a gentle
 * shake for heavy blows, and short notices for big moments. Fed the session's {@link Cue}s each
 * frame; cues for missiles still in flight wait until they land. Purely for show: nothing here
 * touches the game.
 *
 * <p>Kept cheap for crowds: at most {@link #MAX_PARTICLES} particles and {@link #MAX_RINGS} rings
 * (new ones are skipped when full), and at most {@link #IMPACTS_PER_FRAME} impact bursts a frame,
 * however many hits land.
 */
public final class Fx
{
	static final int MAX_PARTICLES = 420;
	static final int MAX_RINGS = 24;
	static final int IMPACTS_PER_FRAME = 10;
	/** Seconds of 1.0 shake that one unit of trauma gives; it fades this much a second. */
	static final double TRAUMA_DECAY = 1.8;
	/** Largest shake, in screen pixels. */
	static final double MAX_SHAKE = 6;
	static final double TOAST_SECONDS = 2.6;

	private enum Kind
	{
		/** A small square that falls. */
		CHUNK,
		/** A streak along its motion. */
		SPARK,
		/** A soft blob that grows and rises as it fades. */
		SMOKE,
		/** A twinkle: a little cross. */
		STAR
	}

	// the particle pool, as parallel arrays so a crowd's worth costs no allocation
	private final double[] x = new double[MAX_PARTICLES];
	private final double[] y = new double[MAX_PARTICLES];
	/** Height above the ground, in world units. */
	private final double[] z = new double[MAX_PARTICLES];
	private final double[] vx = new double[MAX_PARTICLES];
	private final double[] vy = new double[MAX_PARTICLES];
	private final double[] vz = new double[MAX_PARTICLES];
	private final double[] age = new double[MAX_PARTICLES];
	private final double[] life = new double[MAX_PARTICLES];
	private final double[] size = new double[MAX_PARTICLES];
	private final int[] colour = new int[MAX_PARTICLES];
	private final Kind[] kind = new Kind[MAX_PARTICLES];
	private final double[] gravity = new double[MAX_PARTICLES];
	private int count;

	/** A ring spreading over the ground. */
	private static final class Ring
	{
		double x;
		double y;
		double from;
		double to;
		double age;
		double life;
		int colour;
		float width;
	}

	/** A cue waiting for its missile to land. */
	private static final class Pending
	{
		final Cue cue;
		double left;

		Pending(Cue cue)
		{
			this.cue = cue;
			this.left = cue.delay;
		}
	}

	/** A notice across the top of the screen. */
	static final class Toast
	{
		final String title;
		final String line;
		final Color colour;
		double age;

		Toast(String title, String line, Color colour)
		{
			this.title = title;
			this.line = line;
			this.colour = colour;
		}
	}

	private final List<Ring> rings = new ArrayList<>();
	private final List<Pending> pending = new ArrayList<>();
	private final List<Toast> toasts = new ArrayList<>();
	private final Random random = new Random();
	private double trauma;
	/** 0..1: how red the screen's edge is from the last hits. */
	private double hurt;
	private double clock;
	private double shakeX;
	private double shakeY;
	private int impactsThisFrame;
	private Options options = new Options();
	/** The XP bar's fill as shown (it slides towards the real value), and the level it's for. */
	private double xpShown;
	private int xpLevel = -1;
	/** 1 as XP comes in, fading: the bar's edge glows. */
	private double xpPulse;
	/** The champion's health as the trail behind its bar shows it. */
	private double championShown = 1;

	/** Takes this frame's cues; those for missiles in flight wait for {@link #update} to land them. */
	public void handle(List<Cue> cues, GameSession session)
	{
		options = session.getOptions();
		impactsThisFrame = 0;
		for (Cue c : cues)
		{
			if (c.delay > 0)
			{
				pending.add(new Pending(c));
			}
			else
			{
				apply(c, session);
			}
		}
	}

	/**
	 * Moves everything on by {@code dt} real seconds. The world's effects only move while the game
	 * runs (they freeze with it under a menu); notices always do.
	 */
	public void update(double dt, GameSession session)
	{
		if (!(dt > 0))
		{
			return;
		}
		dt = Math.min(dt, GameSession.MAX_FRAME);
		updateBars(dt, session);
		for (Iterator<Toast> it = toasts.iterator(); it.hasNext(); )
		{
			Toast t = it.next();
			t.age += dt;
			if (t.age > TOAST_SECONDS)
			{
				it.remove();
			}
		}
		if (session.getPhase() != com.osrssurvivors.game.Phase.RUNNING)
		{
			shakeX = 0;
			shakeY = 0;
			return;
		}
		clock += dt;
		for (Iterator<Pending> it = pending.iterator(); it.hasNext(); )
		{
			Pending p = it.next();
			p.left -= dt;
			if (p.left <= 0)
			{
				it.remove();
				apply(p.cue, session);
			}
		}
		double drag = Math.pow(0.15, dt);
		for (int i = 0; i < count; i++)
		{
			age[i] += dt;
			if (age[i] >= life[i])
			{
				// swap the last one into this place
				count--;
				move(count, i);
				i--;
				continue;
			}
			x[i] += vx[i] * dt;
			y[i] += vy[i] * dt;
			z[i] += vz[i] * dt;
			vz[i] -= gravity[i] * dt;
			if (z[i] < 0 && gravity[i] > 0)
			{
				// land and skid to a stop
				z[i] = 0;
				vz[i] = -vz[i] * 0.3;
				vx[i] *= 0.6;
				vy[i] *= 0.6;
			}
			if (kind[i] == Kind.SMOKE)
			{
				vx[i] *= drag;
				vy[i] *= drag;
			}
		}
		for (Iterator<Ring> it = rings.iterator(); it.hasNext(); )
		{
			Ring r = it.next();
			r.age += dt;
			if (r.age >= r.life)
			{
				it.remove();
			}
		}
		trauma = Math.max(0, trauma - TRAUMA_DECAY * dt);
		hurt = Math.max(0, hurt - 1.6 * dt);
		double shake = options.isOn(Option.SCREEN_SHAKE) ? trauma * trauma * MAX_SHAKE : 0;
		shakeX = shake * (smoothNoise(clock * 27) * 2 - 1);
		shakeY = shake * (smoothNoise(clock * 31 + 100) * 2 - 1);
	}

	private void updateBars(double dt, GameSession session)
	{
		com.osrssurvivors.game.Progression p = session.getProgression();
		double target = Math.min(1, p.getXp() / (double) Math.max(1, p.getXpToNext()));
		if (p.getLevel() != xpLevel)
		{
			xpLevel = p.getLevel();
			xpShown = target < xpShown ? 0 : xpShown;
		}
		xpShown += (target - xpShown) * Math.min(1, dt * 10);
		xpPulse = Math.max(0, xpPulse - dt * 2.5);
		Enemy champion = session.getChampion();
		if (champion == null)
		{
			championShown = 1;
		}
		else
		{
			double health = Math.max(0, champion.getHealth() / champion.getMaxHealth());
			championShown = health >= championShown ? health : Math.max(health, championShown - dt * 0.35);
		}
	}

	/** The XP bar's fill to draw, 0..1. */
	public double getXpShown()
	{
		return xpShown;
	}

	public double getXpPulse()
	{
		return xpPulse;
	}

	public double getChampionShown()
	{
		return championShown;
	}

	/** Shake offset for the world this frame, in screen pixels (the interface stays still). */
	public double getShakeX()
	{
		return shakeX;
	}

	public double getShakeY()
	{
		return shakeY;
	}

	/** 0..1: how hard the player was hit just now. */
	public double getHurt()
	{
		return hurt;
	}

	public List<Toast> getToasts()
	{
		return toasts;
	}

	/** Particles alive, for tests and tuning. */
	public int particleCount()
	{
		return count;
	}

	/** Shows a notice at the top of the screen (at most two at once; the oldest goes). */
	public void toast(String title, String line, Color colour)
	{
		if (toasts.size() >= 2)
		{
			toasts.remove(0);
		}
		toasts.add(new Toast(title, line, colour));
	}

	/** Forgets everything, for a new run. */
	public void clear()
	{
		count = 0;
		rings.clear();
		pending.clear();
		toasts.clear();
		trauma = 0;
		hurt = 0;
		shakeX = 0;
		shakeY = 0;
	}

	// ---------------------------------------------------------------- what each cue looks like

	private void apply(Cue c, GameSession session)
	{
		Player player = session.getPlayer();
		switch (c.type)
		{
			case IMPACT:
				if (impactsThisFrame++ < IMPACTS_PER_FRAME)
				{
					impact(c);
				}
				break;
			case KILL:
				if (c.value == 1)
				{
					championDeath(c, session);
				}
				else
				{
					deathPuff(c);
				}
				break;
			case PLAYER_HURT:
			{
				double share = c.value / Math.max(1, player.getMaxHealth());
				hurt = Math.min(1, hurt + 0.25 + share * 4);
				// only real blows shake: a tenth of the hitpoints is a full jolt
				addTrauma(Math.min(0.45, share * 4));
				break;
			}
			case PLAYER_DEATH:
				hurt = 1;
				addTrauma(0.6);
				break;
			case ABILITY:
				if (c.value == 1)
				{
					abilityLands(c);
				}
				break;
			case XP:
				xpPulse = 1;
				for (int i = 0; i < 2; i++)
				{
					spawn(Kind.STAR, player.getX() + spread(10), player.getY() + spread(6), 14 + random.nextDouble() * 10,
						spread(12), spread(6), 30 + random.nextDouble() * 20, 0, 0.45, 3, 0xff9ef0ff);
				}
				break;
			case CASKET_PICKUP:
				burst(c.x, c.y, 18, 0xffffd75a, Kind.STAR, 90, 0.8);
				ring(c.x, c.y, 6, 70, 0.5, 0xffffd75a, 3);
				break;
			case CHAMPION_ARRIVES:
			{
				ring(c.x, c.y, 10, 150, 0.9, 0xffffc828, 4);
				ring(c.x, c.y, 10, 100, 0.7, 0xffb41414, 3);
				addTrauma(0.3);
				Enemy champion = session.getChampion();
				toast(champion == null ? "A champion arrives!" : champion.getTitle(), "has entered the fight!", GameRenderer.GOLD);
				break;
			}
			case REVIVE:
				ring(c.x, c.y, 10, 200, 0.9, 0xffffe68c, 5);
				burst(c.x, c.y, 30, 0xffffffff, Kind.STAR, 140, 1.0);
				toast("Revived!", "Your Revival upgrade saved you.", GameRenderer.GOD);
				break;
			case PRAYER_PROC:
				if (c.passive == Passive.RETRIBUTION)
				{
					ring(c.x, c.y, 10, 110, 0.5, 0xffff8c1e, 5);
					burst(c.x, c.y, 24, 0xffff9628, Kind.SPARK, 220, 0.5);
					addTrauma(0.35);
					toast("Retribution!", "Your prayer lashes out.", GameRenderer.ORANGE);
				}
				else
				{
					ring(c.x, c.y, 8, 60, 0.6, 0xff78ff8c, 4);
					burst(c.x, c.y, 16, 0xff9cffa8, Kind.STAR, 70, 0.9);
					toast("Redemption!", "Your prayer heals you.", GameRenderer.GREEN);
				}
				break;
			case EAT:
			case DRINK:
				for (int i = 0; i < 6; i++)
				{
					spawn(Kind.STAR, player.getX() + spread(12), player.getY() + spread(6), 6 + random.nextDouble() * 18,
						spread(10), spread(6), 35 + random.nextDouble() * 25, 0, 0.7, 3, c.type == Cue.Type.EAT ? 0xff7cff7c : 0xffc89cff);
				}
				break;
			case LEVEL_UP:
				ring(player.getX(), player.getY(), 6, 90, 0.7, 0xffffd75a, 4);
				break;
			case CHOOSE:
				if (c.gear != null && com.osrssurvivors.game.Combination.making(c.gear) != null)
				{
					// a combination weapon: the moment the run's build comes together
					ring(player.getX(), player.getY(), 8, 130, 0.9, 0xffffd75a, 5);
					burst(player.getX(), player.getY(), 40, 0xffffe68c, Kind.STAR, 160, 1.1);
					toast(c.gear.getTitle() + "!", com.osrssurvivors.game.Combination.making(c.gear).recipe(), GameRenderer.GOLD);
				}
				break;
			default:
				break;
		}
	}

	private void addTrauma(double amount)
	{
		trauma = Math.min(1, trauma + amount);
	}

	/** Sparks where a hit lands, coloured by what made it. */
	private void impact(Cue c)
	{
		double hx = c.x;
		double hy = c.y;
		double h = 14;
		Gear g = c.gear;
		if (c.passive != null)
		{
			for (int i = 0; i < 3; i++)
			{
				spawn(Kind.SPARK, hx, hy, h, spread(110), spread(60), 40 + random.nextDouble() * 60, 260, 0.22, 2, 0xffffe6a0);
			}
			return;
		}
		if (g != null && g.getMissile() == Gear.Missile.SPELL)
		{
			int col = spellColour(g).getRGB();
			for (int i = 0; i < 5; i++)
			{
				spawn(Kind.CHUNK, hx, hy, h, spread(90), spread(50), 30 + random.nextDouble() * 70, 200, 0.35, 3, col);
			}
			ring(hx, hy, 3, 22, 0.25, col, 2);
			return;
		}
		boolean ranged = g != null && g.getStyle() == Style.RANGED;
		int col = ranged ? 0xffe8d8b0 : 0xffffffff;
		for (int i = 0; i < (ranged ? 3 : 4); i++)
		{
			spawn(Kind.SPARK, hx, hy, h, spread(150), spread(80), 30 + random.nextDouble() * 80, 300, 0.2, 2, col);
		}
		// a little blood with the steel
		if (!ranged)
		{
			spawn(Kind.CHUNK, hx, hy, h, spread(60), spread(40), 40 + random.nextDouble() * 40, 260, 0.45, 2, 0xffa01e1e);
		}
	}

	/** Dust and bits where a monster falls. */
	private void deathPuff(Cue c)
	{
		for (int i = 0; i < 5; i++)
		{
			spawn(Kind.SMOKE, c.x + spread(8), c.y + spread(4), 4 + random.nextDouble() * 6, spread(30), spread(14),
				12 + random.nextDouble() * 18, 0, 0.55 + random.nextDouble() * 0.25, 5 + random.nextDouble() * 4, 0xffb4aa96);
		}
		for (int i = 0; i < 3; i++)
		{
			spawn(Kind.CHUNK, c.x, c.y, 10, spread(80), spread(40), 60 + random.nextDouble() * 60, 320, 0.6, 2, 0xff8c1c1c);
		}
	}

	/** A champion falls: a gold shockwave, a shower of sparks, a jolt and a notice. */
	private void championDeath(Cue c, GameSession session)
	{
		ring(c.x, c.y, 10, 220, 0.9, 0xffffd75a, 6);
		ring(c.x, c.y, 10, 140, 0.6, 0xffffffff, 3);
		burst(c.x, c.y, 50, 0xffffd75a, Kind.SPARK, 300, 0.9);
		for (int i = 0; i < 10; i++)
		{
			spawn(Kind.SMOKE, c.x + spread(16), c.y + spread(8), 6 + random.nextDouble() * 10, spread(50), spread(20),
				15 + random.nextDouble() * 20, 0, 0.9, 8 + random.nextDouble() * 6, 0xffc8bea0);
		}
		addTrauma(0.65);
		toast((c.kind == null ? "Champion" : c.kind.getTitle()) + " defeated!", "Grab the casket it dropped.", GameRenderer.GOLD);
	}

	/** Area spells going off: ice, blood, god spells, dragonfire, chinchompa blasts. */
	private void abilityLands(Cue c)
	{
		switch (c.passive)
		{
			case ICE_BARRAGE:
				ring(c.x, c.y, 10, 80, 0.45, 0xffaadcff, 4);
				burst(c.x, c.y, 16, 0xffdcf0ff, Kind.CHUNK, 140, 0.5);
				break;
			case BLOOD_BARRAGE:
				ring(c.x, c.y, 10, 80, 0.45, 0xffb41428, 4);
				burst(c.x, c.y, 16, 0xffc81e32, Kind.CHUNK, 120, 0.6);
				break;
			case GOD_SPELLS:
				for (int i = 0; i < 12; i++)
				{
					spawn(Kind.SPARK, c.x + spread(10), c.y + spread(5), 70 + random.nextDouble() * 30, spread(20), spread(10),
						-160 - random.nextDouble() * 80, 0, 0.35, 2, 0xffffe68c);
				}
				ring(c.x, c.y, 4, 40, 0.35, 0xffffe68c, 3);
				break;
			case DRAGONFIRE:
				for (int i = 0; i < 6; i++)
				{
					spawn(Kind.SMOKE, c.x + spread(14), c.y + spread(7), 6 + random.nextDouble() * 10, spread(30), spread(16),
						30 + random.nextDouble() * 30, 0, 0.5, 5 + random.nextDouble() * 4, 0xffff8c28);
				}
				break;
			case CHINCHOMPAS:
				ring(c.x, c.y, 6, 60, 0.35, 0xffffa032, 4);
				burst(c.x, c.y, 14, 0xffffc864, Kind.SPARK, 200, 0.4);
				for (int i = 0; i < 4; i++)
				{
					spawn(Kind.SMOKE, c.x + spread(10), c.y + spread(5), 6, spread(30), spread(15), 20, 0, 0.7, 7, 0xff786e64);
				}
				addTrauma(0.08);
				break;
			default:
				break;
		}
	}

	private void burst(double cx, double cy, int n, int col, Kind k, double speed, double seconds)
	{
		for (int i = 0; i < n; i++)
		{
			double a = random.nextDouble() * Math.PI * 2;
			double s = speed * (0.4 + random.nextDouble() * 0.6);
			spawn(k, cx, cy, 12, Math.cos(a) * s, Math.sin(a) * s * 0.55, 40 + random.nextDouble() * 80, k == Kind.STAR ? 0 : 260,
				seconds * (0.6 + random.nextDouble() * 0.4), k == Kind.STAR ? 3 : 2, col);
		}
	}

	private void ring(double cx, double cy, double from, double to, double seconds, int col, float width)
	{
		if (rings.size() >= MAX_RINGS)
		{
			return;
		}
		Ring r = new Ring();
		r.x = cx;
		r.y = cy;
		r.from = from;
		r.to = to;
		r.life = seconds;
		r.colour = col;
		r.width = width;
		rings.add(r);
	}

	private double spread(double v)
	{
		return (random.nextDouble() * 2 - 1) * v;
	}

	private void spawn(Kind k, double px, double py, double pz, double pvx, double pvy, double pvz, double g, double seconds,
		double s, int col)
	{
		if (count >= MAX_PARTICLES)
		{
			return;
		}
		int i = count++;
		kind[i] = k;
		x[i] = px;
		y[i] = py;
		z[i] = pz;
		vx[i] = pvx;
		vy[i] = pvy;
		vz[i] = pvz;
		gravity[i] = g;
		age[i] = 0;
		life[i] = seconds;
		size[i] = s;
		colour[i] = col;
	}

	private void move(int from, int to)
	{
		kind[to] = kind[from];
		x[to] = x[from];
		y[to] = y[from];
		z[to] = z[from];
		vx[to] = vx[from];
		vy[to] = vy[from];
		vz[to] = vz[from];
		gravity[to] = gravity[from];
		age[to] = age[from];
		life[to] = life[from];
		size[to] = size[from];
		colour[to] = colour[from];
	}

	/** 0..1, smooth, from a number: for the shake. */
	private static double smoothNoise(double t)
	{
		int i = (int) Math.floor(t);
		double f = t - i;
		double a = GameRenderer.noise(i);
		double b = GameRenderer.noise(i + 1);
		double s = f * f * (3 - 2 * f);
		return a + (b - a) * s;
	}

	// ---------------------------------------------------------------- drawing

	/** Rings on the ground, under the characters. */
	void drawGround(Graphics2D g, double originX, double originY, double scale)
	{
		Stroke old = g.getStroke();
		for (Ring r : rings)
		{
			double t = r.age / r.life;
			double ease = 1 - (1 - t) * (1 - t);
			int radius = (int) Math.round((r.from + (r.to - r.from) * ease) * scale);
			int cx = (int) Math.round(originX + r.x * scale);
			int cy = (int) Math.round(originY + r.y * scale);
			int a = (int) Math.round(255 * (1 - t) * ((r.colour >>> 24) / 255.0));
			if (a <= 0 || radius <= 0)
			{
				continue;
			}
			g.setStroke(new BasicStroke((float) Math.max(1, r.width * scale * (1 - t * 0.6))));
			g.setColor(withAlpha(r.colour, a));
			g.drawOval(cx - radius, cy - radius / 2, radius * 2, radius);
			g.setColor(withAlpha(r.colour, a / 5));
			g.fillOval(cx - radius, cy - radius / 2, radius * 2, radius);
		}
		g.setStroke(old);
	}

	/** Particles, over the characters. */
	void drawAir(Graphics2D g, double originX, double originY, double scale, Rectangle view)
	{
		Stroke old = g.getStroke();
		Stroke thin = new BasicStroke((float) Math.max(1, scale * 1.4));
		for (int i = 0; i < count; i++)
		{
			double t = age[i] / life[i];
			int sx = (int) Math.round(originX + x[i] * scale);
			int sy = (int) Math.round(originY + (y[i] - z[i]) * scale);
			if (sx < view.x - 20 || sx > view.x + view.width + 20 || sy < view.y - 20 || sy > view.y + view.height + 20)
			{
				continue;
			}
			int base = colour[i] >>> 24;
			switch (kind[i])
			{
				case SPARK:
				{
					int a = (int) (base * (1 - t));
					g.setStroke(thin);
					g.setColor(withAlpha(colour[i], a));
					double len = 0.035;
					int tx = (int) Math.round(sx - vx[i] * len * scale);
					int ty = (int) Math.round(sy - (vy[i] - vz[i]) * len * scale);
					g.drawLine(tx, ty, sx, sy);
					break;
				}
				case SMOKE:
				{
					int a = (int) (base * 0.45 * (1 - t));
					int r = (int) Math.round(size[i] * (1 + t * 1.2) * scale);
					g.setColor(withAlpha(colour[i], a));
					g.fillOval(sx - r, sy - r, r * 2, r * 2);
					break;
				}
				case STAR:
				{
					int a = (int) (base * (t < 0.2 ? t / 0.2 : 1 - (t - 0.2) / 0.8));
					int r = (int) Math.max(1, Math.round(size[i] * scale * (1 - t * 0.5)));
					g.setColor(withAlpha(colour[i], a));
					g.fillRect(sx - r, sy, r * 2 + 1, 1);
					g.fillRect(sx, sy - r, 1, r * 2 + 1);
					break;
				}
				default:
				{
					int a = (int) (base * Math.min(1, (1 - t) * 2));
					int r = (int) Math.max(1, Math.round(size[i] * scale * 0.6));
					g.setColor(withAlpha(colour[i], a));
					g.fillRect(sx - r / 2, sy - r / 2, r, r);
					break;
				}
			}
		}
		g.setStroke(old);
	}

	static Color withAlpha(int argb, int alpha)
	{
		return new Color((argb >> 16) & 0xff, (argb >> 8) & 0xff, argb & 0xff, Math.max(0, Math.min(255, alpha)));
	}

	/** The colour of a weapon's spell, for its glow, trail and splash. */
	static Color spellColour(Gear g)
	{
		if (g == null)
		{
			return new Color(220, 235, 255);
		}
		String n = g.name();
		if (n.contains("FIRE") || n.contains("SANGUI") || n.contains("ZORYA") || n.contains("NIGHTMARE") || n.contains("IBAN"))
		{
			return new Color(255, 140, 40);
		}
		if (n.contains("SWAMP") || n.contains("TOXIC") || n.contains("ACCURSED") || n.contains("MASTER_WAND"))
		{
			return new Color(110, 220, 90);
		}
		if (n.contains("ICE") || n.contains("KODAI"))
		{
			return new Color(160, 220, 255);
		}
		if (n.contains("SEAS") || n.contains("ANCIENT") || n.contains("STAFF_OF_THE_DEAD"))
		{
			return new Color(80, 150, 255);
		}
		if (n.contains("SHADOW") || n.contains("AYAK") || n.contains("AHRIM"))
		{
			return new Color(190, 120, 255);
		}
		return new Color(225, 240, 255);
	}
}
