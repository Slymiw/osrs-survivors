package com.osrssurvivors.game;

/** A display setting on the options page; all are on unless the player turns them off. */
public enum Option
{
	FLASHES("Screen flashes", "White flashes, blinking lights, and monsters flashing white when hit."),
	LEVEL_UP_SHOW("Level-up animation", "Light rays, sparks and the bouncing banner when you level up."),
	DROP_SHOW("Boss drop animation", "Shaking, searchlights and fireworks around the champion's casket.");

	private final String title;
	private final String description;

	Option(String title, String description)
	{
		this.title = title;
		this.description = description;
	}

	public String getTitle()
	{
		return title;
	}

	public String getDescription()
	{
		return description;
	}
}
