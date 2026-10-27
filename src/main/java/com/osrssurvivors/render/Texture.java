package com.osrssurvivors.render;

/**
 * A texture from the game's cache, as drawn on worn items: square RGB texels (0 for see-through),
 * and how it scrolls, like the lava of the fire and infernal capes.
 */
public final class Texture
{
	/** Seconds a texture of speed 1 takes to scroll once round; faster ones loop sooner. */
	static final double LOOP_SECONDS = 4.0;

	final int[] texels;
	final int size;
	/** The game's scroll direction: 0 none, 1 and 3 along v, 2 and 4 along u. */
	final int direction;
	final int speed;

	public Texture(int[] texels, int size, int direction, int speed)
	{
		this.texels = texels;
		this.size = size;
		this.direction = direction;
		this.speed = speed;
	}

	/** Whether it scrolls by itself. */
	public boolean flows()
	{
		return direction != 0 && speed > 0;
	}

	/** How far it has scrolled after {@code seconds}, as a share of its size, with its sign. */
	public double scroll(double seconds)
	{
		if (!flows())
		{
			return 0;
		}
		double loop = LOOP_SECONDS / speed;
		double share = seconds / loop - Math.floor(seconds / loop);
		return direction == 1 || direction == 2 ? share : -share;
	}

	boolean alongV()
	{
		return direction == 1 || direction == 3;
	}
}
