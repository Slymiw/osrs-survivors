package com.osrssurvivors.render;

/** A piece of scenery (tree, wall, rock...) ready to draw, standing at a world position. */
public final class Prop
{
	public final double x;
	public final double y;
	public final ActorSprite sprite;

	public Prop(double x, double y, ActorSprite sprite)
	{
		this.x = x;
		this.y = y;
		this.sprite = sprite;
	}
}
