package com.osrssurvivors.render;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;

/**
 * The fanfare when a level-up screen opens: a white flash, a shockwave and slowly turning rays
 * behind the panel, the panel popping in, a "Level up!" banner bouncing above it, and a fountain
 * of twinkling sparks. Like DropShow, everything is a pure function of the time since the
 * level-up, so nothing is kept between frames.
 */
final class LevelUpShow
{
	/** How long the show lasts; the rays and banner fade out by then. */
	static final double DURATION = 2.4;

	private static final Color GOLD = new Color(255, 200, 40);
	private static final Color PALE = new Color(255, 245, 190);
	private static final Color ORANGE = new Color(255, 140, 20);
	private static final int RAYS = 14;
	private static final int SPARKS = 56;
	private static final double GRAVITY = 420;
	private static final Stroke RING = new BasicStroke(4f);

	private LevelUpShow()
	{
	}

	static boolean isPlaying(double time)
	{
		return time >= 0 && time < DURATION;
	}

	/** 0..1: fades in over the first moments and out over the last 0.8 s. */
	private static double strength(double time)
	{
		return Math.min(1, time / 0.12) * Math.max(0, Math.min(1, (DURATION - time) / 0.8));
	}

	/** The panel pops in from small, overshoots, and settles with a wobble. */
	static AffineTransform transform(Rectangle panel, double time)
	{
		double s;
		if (time < 0.14)
		{
			s = 0.6 + 0.55 * time / 0.14;
		}
		else
		{
			double t = time - 0.14;
			s = 1 + 0.15 * Math.exp(-t * 10) * Math.cos(t * 22);
		}
		double cx = panel.getCenterX();
		double cy = panel.getCenterY();
		AffineTransform at = new AffineTransform();
		at.translate(cx, cy);
		at.scale(s, s);
		at.translate(-cx, -cy);
		return at;
	}

	/**
	 * Behind the panel: the flash (if {@code flash}), and turning rays and a shockwave from the
	 * panel's centre (if {@code show}).
	 */
	static void drawBehind(Graphics2D g, Rectangle view, Rectangle panel, double time, boolean flash, boolean show)
	{
		Composite old = g.getComposite();
		Stroke oldStroke = g.getStroke();
		int cx = (int) panel.getCenterX();
		int cy = (int) panel.getCenterY();

		if (flash && time < 0.22)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.5 * (1 - time / 0.22))));
			g.setColor(Color.WHITE);
			g.fill(view);
		}

		if (!show)
		{
			g.setComposite(old);
			return;
		}
		double fade = strength(time);
		if (fade > 0)
		{
			double reach = Math.hypot(view.width, view.height);
			double turn = time * 0.6;
			double half = Math.PI / RAYS * 0.45;
			g.setColor(GOLD);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.28 * fade)));
			for (int i = 0; i < RAYS; i++)
			{
				double a = turn + i * 2 * Math.PI / RAYS;
				int[] xs = {cx, cx + (int) (Math.cos(a - half) * reach), cx + (int) (Math.cos(a + half) * reach)};
				int[] ys = {cy, cy + (int) (Math.sin(a - half) * reach), cy + (int) (Math.sin(a + half) * reach)};
				g.fillPolygon(xs, ys, 3);
			}
		}

		for (int ring = 0; ring < 2; ring++)
		{
			double t = time - ring * 0.12;
			if (t > 0 && t < 0.6)
			{
				int r = (int) (30 + 900 * t);
				g.setStroke(RING);
				g.setColor(ring == 0 ? PALE : GOLD);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.8 * (1 - t / 0.6))));
				g.drawOval(cx - r, cy - r, r * 2, r * 2);
			}
		}
		g.setStroke(oldStroke);
		g.setComposite(old);
	}

	/** In front of the panel: sparks shooting up from behind the banner and raining down. */
	static void drawSparks(Graphics2D g, int cx, int cy, double time)
	{
		Composite old = g.getComposite();
		for (int i = 0; i < SPARKS; i++)
		{
			// two waves, the second a little later and softer
			double t = time - (i % 2) * 0.18;
			if (t < 0 || t > 1.8)
			{
				continue;
			}
			double angle = -Math.PI / 2 + (GameRenderer.noise(i + 31) - 0.5) * Math.PI * 1.7;
			double speed = 220 + GameRenderer.noise(i + 377) * 380;
			double x = cx + Math.cos(angle) * speed * t;
			double y = cy + Math.sin(angle) * speed * t + GRAVITY * t * t;
			double twinkle = 0.6 + 0.4 * Math.sin(t * 30 + i);
			float alpha = (float) Math.max(0, Math.min(1, (1.8 - t) / 0.5) * twinkle);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
			double pick = GameRenderer.noise(i + 911);
			g.setColor(pick < 0.5 ? GOLD : pick < 0.8 ? PALE : ORANGE);
			int r = 2 + (int) (GameRenderer.noise(i + 53) * 4);
			int px = (int) x;
			int py = (int) y;
			// a four-pointed star
			g.fillPolygon(new int[]{px, px + r / 2, px + r * 2, px + r / 2, px, px - r / 2, px - r * 2, px - r / 2},
				new int[]{py - r * 2, py - r / 2, py, py + r / 2, py + r * 2, py + r / 2, py, py - r / 2}, 8);
		}
		g.setComposite(old);
	}

	/** The banner's size: grows from nothing past full size, then settles and breathes. */
	static double bannerScale(double time)
	{
		if (time < 0.18)
		{
			return 2.8 * time / 0.18;
		}
		double t = time - 0.18;
		return 2.2 + 0.6 * Math.exp(-t * 8) * Math.cos(t * 18) + 0.05 * Math.sin(time * 6);
	}

	/** The banner's colour shimmers between gold and pale. */
	static Color bannerColour(double time)
	{
		double k = 0.5 + 0.5 * Math.sin(time * 9);
		return new Color(255, (int) (200 + 45 * k), (int) (40 + 150 * k));
	}

	static float bannerAlpha(double time)
	{
		return (float) Math.max(0, Math.min(1, (DURATION - time) / 0.8));
	}
}
