package com.osrssurvivors.render;

import java.awt.image.BufferedImage;

/** A pre-rendered character image; the anchor is the pixel where its feet touch the ground. */
public final class ActorSprite
{
	public final BufferedImage image;
	public final int anchorX;
	public final int anchorY;

	public ActorSprite(BufferedImage image, int anchorX, int anchorY)
	{
		this.image = image;
		this.anchorX = anchorX;
		this.anchorY = anchorY;
	}
}
