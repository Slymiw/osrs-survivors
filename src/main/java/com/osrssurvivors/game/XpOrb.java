package com.osrssurvivors.game;

public final class XpOrb
{
	public static final double RADIUS = 5;
	/** World units per second while being pulled in by the player's magnet. */
	static final double PULL_SPEED = 420;

	double x;
	double y;
	final int value;

	XpOrb(double x, double y, int value)
	{
		this.x = x;
		this.y = y;
		this.value = value;
	}

	public double getX()
	{
		return x;
	}

	public double getY()
	{
		return y;
	}

	public int getValue()
	{
		return value;
	}
}
