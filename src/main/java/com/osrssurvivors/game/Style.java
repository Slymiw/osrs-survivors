package com.osrssurvivors.game;

public enum Style
{
	MELEE("melee"),
	RANGED("ranged"),
	MAGIC("magic");

	private final String label;

	Style(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}
}
