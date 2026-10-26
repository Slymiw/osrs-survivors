package com.osrssurvivors.render;

import java.awt.image.BufferedImage;

/** A picture of the arena's ground, covering it from the world origin. */
public final class Terrain
{
	public final BufferedImage image;
	public final double unitsPerPixel;

	public Terrain(BufferedImage image, double unitsPerPixel)
	{
		this.image = image;
		this.unitsPerPixel = unitsPerPixel;
	}
}
