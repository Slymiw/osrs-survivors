package com.osrssurvivors.game;

/** Loot dropped by a champion; picking it up offers a choice of gear. */
public final class Casket
{
	public static final double RADIUS = 10;

	double x;
	double y;

	Casket(double x, double y)
	{
		this.x = x;
		this.y = y;
	}

	public double getX()
	{
		return x;
	}

	public double getY()
	{
		return y;
	}
}
