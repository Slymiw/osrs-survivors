package com.osrssurvivors.render;

import java.awt.Point;

/**
 * What fingers are doing on a touch screen, for drawing: the movement stick (a finger dragged
 * anywhere on open ground) and an item held in the bag. Set by the browser glue every frame.
 */
public final class TouchControls
{
	/** How far the stick's knob can move from its centre; a finger dragged further pulls the centre along. */
	public static final int STICK_RADIUS = 34;
	/** Closer to the centre than this the player stands still. */
	public static final int STICK_DEADZONE = 5;

	/** Where the steering finger came down (pulled along when it goes further than the radius), or null. */
	public Point stickOrigin;
	/** Where the steering finger is now, or null. */
	public Point stickAt;
	/** Whether the bag item under the finger is being looked at (a long press), so its tooltip shows. */
	public boolean inspecting;
	/** Whether the held bag item has been dragged out of the bag, where letting go drops it. */
	public boolean dropping;
	/** Whether the player has steered this run, so the "drag to move" hint can go. */
	public boolean steered;

	/** The stick's direction, or zero inside the dead zone or with no finger down. */
	public double[] direction()
	{
		if (stickOrigin == null || stickAt == null)
		{
			return new double[2];
		}
		double dx = stickAt.x - stickOrigin.x;
		double dy = stickAt.y - stickOrigin.y;
		return Math.hypot(dx, dy) < STICK_DEADZONE ? new double[2] : new double[]{dx, dy};
	}

	/** Moves the finger; past the radius the stick's centre follows it, so turning back responds at once. */
	public void drag(Point at)
	{
		stickAt = at;
		if (stickOrigin == null)
		{
			return;
		}
		double dx = at.x - stickOrigin.x;
		double dy = at.y - stickOrigin.y;
		double d = Math.hypot(dx, dy);
		if (d >= STICK_DEADZONE)
		{
			steered = true;
		}
		if (d > STICK_RADIUS)
		{
			double k = (d - STICK_RADIUS) / d;
			stickOrigin = new Point((int) Math.round(stickOrigin.x + dx * k), (int) Math.round(stickOrigin.y + dy * k));
		}
	}

	public void release()
	{
		stickOrigin = null;
		stickAt = null;
	}
}
