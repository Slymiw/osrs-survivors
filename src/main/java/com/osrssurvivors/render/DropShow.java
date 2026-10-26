package com.osrssurvivors.render;

import com.osrssurvivors.game.BossDrop;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;

/**
 * The showmanship around a boss drop: a heartbeat that pumps the slot machine faster as the
 * reels slow, a thump and sparks as each reel lands, searchlights sweeping while it spins, and
 * fireworks once the prize is out. Everything is a pure function of the drop's clock, so nothing
 * is kept between frames.
 */
final class DropShow
{
	private static final Color GOLD = new Color(255, 200, 40);
	private static final double GRAVITY = 70;
	/** How long a firework shell takes to climb, and how long its burst lasts. */
	private static final double RISE = 0.75;
	private static final double BURST = 1.6;
	private static final Stroke STREAK = new BasicStroke(2f);
	private static final Stroke THIN = new BasicStroke(1f);

	private DropShow()
	{
	}

	private static double lastStop()
	{
		return BossDrop.STOPS[BossDrop.STOPS.length - 1];
	}

	/**
	 * The heartbeat, 0..1: short sharp beats that come faster as the spin goes on. Once the reels
	 * stop it settles into a slow, steady pulse.
	 */
	static double beat(double time)
	{
		double last = lastStop();
		double phase;
		if (time < last)
		{
			// beats per second rise from 1.4 to 4.4 over the spin; phase is its integral
			phase = 1.4 * time + 1.5 * time * time / last;
		}
		else
		{
			phase = 1.4 * last + 1.5 * last + 1.6 * (time - last);
		}
		double s = Math.sin(2 * Math.PI * phase);
		return s > 0 ? Math.pow(s, 6) : 0;
	}

	/** How much bigger than normal the machine is drawn right now. */
	static double pump(BossDrop drop)
	{
		double time = drop.getTime();
		int stopped = 0;
		double thump = 0;
		for (int i = 0; i < drop.reelCount(); i++)
		{
			if (drop.isStopped(i))
			{
				stopped++;
				double since = time - BossDrop.STOPS[i];
				boolean last = i == drop.reelCount() - 1;
				double strength = last ? 0.07 + 0.025 * drop.getRarity().ordinal() : 0.045;
				thump += strength * Math.exp(-since * 9) * Math.cos(since * 22);
			}
		}
		double heart = drop.isSpinning() ? (0.012 + 0.01 * stopped) : 0.008;
		return 1 + heart * beat(time) + thump;
	}

	/**
	 * A small jitter that grows while the last reel crawls towards the payline, and a hard shake
	 * when a rare drop lands.
	 */
	static double[] shake(BossDrop drop)
	{
		double time = drop.getTime();
		double last = lastStop();
		double amount = 0;
		int lastReel = drop.reelCount() - 1;
		if (drop.isSpinning() && lastReel > 0 && drop.isStopped(lastReel - 1))
		{
			double tease = (time - BossDrop.STOPS[lastReel - 1]) / (last - BossDrop.STOPS[lastReel - 1]);
			amount = 2.2 * tease * tease;
		}
		else if (!drop.isSpinning() && drop.getRarity().ordinal() >= BossDrop.Rarity.RARE.ordinal())
		{
			amount = 9 * Math.exp(-(time - last) * 5);
		}
		if (amount < 0.2)
		{
			return new double[]{0, 0};
		}
		return new double[]{Math.sin(time * 71) * amount, Math.cos(time * 53) * amount};
	}

	/** Scale (pump) and shake for drawing the machine, about the panel's centre. */
	static AffineTransform transform(BossDrop drop, Rectangle panel)
	{
		double s = pump(drop);
		double[] shake = shake(drop);
		double cx = panel.getCenterX();
		double cy = panel.getCenterY();
		AffineTransform t = new AffineTransform();
		t.translate(cx + shake[0], cy + shake[1]);
		t.scale(s, s);
		t.translate(-cx, -cy);
		return t;
	}

	/** Behind the machine while it spins: two searchlights sweeping, brighter as the end nears. */
	static void drawSearchlights(Graphics2D g, Rectangle view, BossDrop drop)
	{
		if (!drop.isSpinning())
		{
			return;
		}
		double time = drop.getTime();
		double build = Math.min(1, time / lastStop());
		Composite old = g.getComposite();
		double reach = Math.hypot(view.width, view.height);
		for (int side = 0; side < 2; side++)
		{
			double ox = side == 0 ? view.x + view.width * 0.08 : view.x + view.width * 0.92;
			double oy = view.y + view.height;
			// sweep back and forth, the two lights in counterpoint, faster as it builds
			double sweep = Math.sin(time * (1.6 + 2.2 * build) + side * Math.PI) * 0.5;
			double a = -Math.PI / 2 + (side == 0 ? 0.35 : -0.35) + sweep;
			double w = 0.09;
			int[] xs = {(int) ox, (int) (ox + Math.cos(a - w) * reach), (int) (ox + Math.cos(a + w) * reach)};
			int[] ys = {(int) oy, (int) (oy + Math.sin(a - w) * reach), (int) (oy + Math.sin(a + w) * reach)};
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.06 + 0.12 * build + 0.08 * beat(time))));
			g.setColor(new Color(255, 240, 190));
			g.fillPolygon(xs, ys, 3);
		}
		// the edges of the screen throb with the heartbeat
		float edge = (float) (0.25 * build * beat(time));
		if (edge > 0.01)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, edge));
			g.setColor(GOLD);
			int band = Math.max(6, Math.min(view.width, view.height) / 14);
			for (int i = 0; i < band; i += 3)
			{
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, edge * (1 - i / (float) band)));
				g.drawRect(view.x + i, view.y + i, view.width - 1 - 2 * i, view.height - 1 - 2 * i);
			}
		}
		g.setComposite(old);
	}

	/** Sparks thrown off the sides of a reel as it clicks into place, and a ring for the last one. */
	static void drawReelLanding(Graphics2D g, Rectangle reel, BossDrop drop, int index, Color win)
	{
		double since = drop.getTime() - BossDrop.STOPS[index];
		if (!drop.isStopped(index) || since > 0.7)
		{
			return;
		}
		boolean last = index == drop.reelCount() - 1;
		Composite old = g.getComposite();
		float alpha = (float) Math.max(0, 1 - since / 0.7);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		double cx = reel.getCenterX();
		double cy = reel.getCenterY();
		int sparks = last ? 40 : 22;
		for (int i = 0; i < sparks; i++)
		{
			int seed = index * 100 + i;
			double a = GameRenderer.noise(seed) * 2 * Math.PI;
			double speed = 90 + GameRenderer.noise(seed + 7) * 200;
			double d = speed * since;
			double x = cx + Math.cos(a) * (reel.width / 2.0 + d);
			double y = cy + Math.sin(a) * (reel.height / 2.0 + d) + 120 * since * since;
			g.setColor(GameRenderer.noise(seed + 13) < 0.3 ? Color.WHITE : last ? win : GOLD);
			int r = GameRenderer.noise(seed + 3) < 0.5 ? 2 : 3;
			g.fillRect((int) x - r / 2, (int) y - r / 2, r, r);
		}
		if (last)
		{
			ring(g, cx, cy, 30 + since * 420, 6 * alpha, win);
		}
		g.setComposite(old);
	}

	/** A glowing, throbbing frame round the reel that's still spinning after the others stopped. */
	static void drawAnticipation(Graphics2D g, Rectangle reel, BossDrop drop)
	{
		double glow = 0.45 + 0.55 * beat(drop.getTime());
		Composite old = g.getComposite();
		Stroke oldStroke = g.getStroke();
		for (int i = 4; i >= 1; i--)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (glow * 0.22 * (5 - i) / 4)));
			g.setStroke(new BasicStroke(i * 3f));
			g.setColor(((int) (drop.getTime() * 8)) % 2 == 0 ? GOLD : new Color(255, 90, 40));
			g.drawRect(reel.x - 2, reel.y - 2, reel.width + 3, reel.height + 3);
		}
		g.setStroke(oldStroke);
		g.setComposite(old);
	}

	/** An expanding shockwave from the machine as the last reel lands. */
	static void drawShockwave(Graphics2D g, Rectangle view, BossDrop drop, Color colour)
	{
		double since = drop.getTime() - lastStop();
		if (since < 0 || since > 0.8)
		{
			return;
		}
		Composite old = g.getComposite();
		double reach = Math.hypot(view.width, view.height) * 0.6;
		double fade = 1 - since / 0.8;
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.8 * fade)));
		double cx = view.getCenterX();
		double cy = view.getCenterY();
		ring(g, cx, cy, reach * Math.sqrt(since / 0.8), 14 * fade + 1, colour);
		if (drop.getRarity().ordinal() >= BossDrop.Rarity.UNCOMMON.ordinal())
		{
			ring(g, cx, cy, reach * Math.sqrt(since / 0.8) * 0.65, 8 * fade + 1, Color.WHITE);
		}
		g.setComposite(old);
	}

	private static void ring(Graphics2D g, double cx, double cy, double radius, double width, Color colour)
	{
		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke((float) Math.max(1, width)));
		g.setColor(colour);
		int r = (int) radius;
		g.drawOval((int) cx - r, (int) cy - r, 2 * r, 2 * r);
		g.setStroke(oldStroke);
	}

	/**
	 * Fireworks once the reels stop. A handful for loot, a show for a lamp, and for a rare drop or
	 * a pet a display that keeps going until the prize is claimed.
	 */
	static void drawFireworks(Graphics2D g, Rectangle view, BossDrop drop)
	{
		double since = drop.getTime() - lastStop();
		if (since < 0)
		{
			return;
		}
		int rarity = drop.getRarity().ordinal();
		double gap = new double[]{0.9, 0.55, 0.32, 0.2}[rarity];
		int limit = new int[]{5, 9, Integer.MAX_VALUE, Integer.MAX_VALUE}[rarity];
		// shells still in the air or bursting: launched within the last RISE + BURST seconds
		int newest = (int) Math.floor(since / gap);
		int oldest = Math.max(0, (int) Math.floor((since - RISE - BURST) / gap));
		Composite old = g.getComposite();
		for (int k = oldest; k <= Math.min(newest, limit - 1); k++)
		{
			// the first few go up together, for a big opening
			double launch = k < 3 ? k * 0.08 : k * gap;
			double t = since - launch;
			if (t < 0 || t > RISE + BURST)
			{
				continue;
			}
			shell(g, view, drop, k, t, rarity);
		}
		g.setComposite(old);
	}

	private static void shell(Graphics2D g, Rectangle view, BossDrop drop, int k, double t, int rarity)
	{
		double baseX = view.x + view.width * (0.08 + 0.84 * GameRenderer.noise(k * 31 + 1));
		double apexX = baseX + (GameRenderer.noise(k * 31 + 2) - 0.5) * view.width * 0.15;
		double apexY = view.y + view.height * (0.1 + 0.35 * GameRenderer.noise(k * 31 + 3));
		double groundY = view.y + view.height;
		Color colour = shellColour(drop, k);
		if (t < RISE)
		{
			// climbing, slowing near the top, with a sputtering trail
			double u = t / RISE;
			double e = 1 - (1 - u) * (1 - u);
			double x = baseX + (apexX - baseX) * e;
			double y = groundY + (apexY - groundY) * e;
			for (int i = 0; i < 6; i++)
			{
				double back = i * 0.025;
				double ub = Math.max(0, (t - back) / RISE);
				double eb = 1 - (1 - ub) * (1 - ub);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.8 * (1 - i / 6.0))));
				g.setColor(i == 0 ? Color.WHITE : new Color(255, 210, 120));
				int s = i == 0 ? 4 : 2;
				g.fillRect((int) (baseX + (apexX - baseX) * eb) - s / 2, (int) (groundY + (apexY - groundY) * eb) - s / 2, s, s);
			}
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
			g.setColor(Color.WHITE);
			g.fillRect((int) x - 2, (int) y - 2, 4, 4);
			return;
		}
		double b = t - RISE;
		double fade = Math.max(0, 1 - b / BURST);
		Composite before = g.getComposite();
		Stroke beforeStroke = g.getStroke();
		// the flash of the burst
		if (b < 0.18)
		{
			double r = 10 + b * 160;
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.55 * (1 - b / 0.18))));
			g.setColor(Color.WHITE);
			g.fillOval((int) (apexX - r), (int) (apexY - r), (int) (2 * r), (int) (2 * r));
		}
		int particles = 40 + 10 * rarity;
		double size = (0.8 + 0.5 * GameRenderer.noise(k * 31 + 4)) * Math.min(view.width, view.height) / 400.0;
		// drag slows the stars quickly, gravity pulls them down after
		double spread = (1 - Math.exp(-b * 3.2)) / 3.2;
		double spreadEarlier = (1 - Math.exp(-Math.max(0, b - 0.06) * 3.2)) / 3.2;
		boolean ringShell = GameRenderer.noise(k * 31 + 5) < 0.35;
		for (int i = 0; i < particles; i++)
		{
			int seed = k * 997 + i;
			double a = 2 * Math.PI * i / particles + GameRenderer.noise(seed) * 0.2;
			double speed = (ringShell ? 300 : 150 + GameRenderer.noise(seed + 1) * 170) * size;
			double fall = GRAVITY * b * b;
			double x = apexX + Math.cos(a) * speed * spread;
			double y = apexY + Math.sin(a) * speed * spread + fall;
			double px = apexX + Math.cos(a) * speed * spreadEarlier;
			double py = apexY + Math.sin(a) * speed * spreadEarlier + GRAVITY * Math.max(0, b - 0.06) * Math.max(0, b - 0.06);
			// stars crackle (twinkle) as they die out
			boolean twinkleOff = fade < 0.45 && GameRenderer.noise(seed + (int) (b * 20)) < 0.4;
			if (twinkleOff)
			{
				continue;
			}
			Color c = fade < 0.45 && GameRenderer.noise(seed + 2) < 0.3 ? Color.WHITE : colour;
			g.setColor(c);
			// a soft glow, the streak it leaves, and the bright star itself
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (fade * 0.3)));
			int glow = fade > 0.6 ? 9 : 6;
			g.fillOval((int) x - glow / 2, (int) y - glow / 2, glow, glow);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (fade * 0.75)));
			g.setStroke(STREAK);
			g.drawLine((int) px, (int) py, (int) x, (int) y);
			g.setStroke(THIN);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) fade));
			int s = fade > 0.6 ? 4 : 3;
			g.fillRect((int) x - s / 2, (int) y - s / 2, s, s);
		}
		g.setComposite(before);
		g.setStroke(beforeStroke);
	}

	private static Color shellColour(BossDrop drop, int k)
	{
		double n = GameRenderer.noise(k * 31 + 6);
		switch (drop.getRarity())
		{
			case JACKPOT:
				return Color.getHSBColor((float) n, 0.75f, 1f);
			case MEGARARE:
				return n < 0.8 ? GameRenderer.megarareColour(n / 0.8) : Color.WHITE;
			case RARE:
				return n < 0.5 ? GOLD : n < 0.8 ? new Color(255, 140, 40) : new Color(255, 245, 200);
			case UNCOMMON:
				return n < 0.6 ? new Color(90, 200, 255) : new Color(170, 120, 255);
			default:
				return n < 0.5 ? GOLD : new Color(120, 230, 120);
		}
	}
}
