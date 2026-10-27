package com.osrssurvivors.game;

/** A volume setting on the options page, from 0 (silent) to {@link #MAX}. */
public enum Volume
{
	MASTER("Master volume", 6),
	EFFECTS("Combat sounds", 8),
	INTERFACE("Interface sounds", 8);

	public static final int MAX = 10;

	private final String title;
	private final int initial;

	Volume(String title, int initial)
	{
		this.title = title;
		this.initial = initial;
	}

	public String getTitle()
	{
		return title;
	}

	public int getInitial()
	{
		return initial;
	}
}
