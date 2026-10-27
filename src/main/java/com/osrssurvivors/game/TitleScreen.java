package com.osrssurvivors.game;

/** Which page of the start menu is showing. */
public enum TitleScreen
{
	/** New run, continue, upgrades, pets, options, save code, what's new. */
	MAIN,
	/** Choosing where to fight. */
	LOCATIONS,
	/** Spending coins on permanent upgrades. */
	UPGRADES,
	/** The pets won so far and what they do. */
	PETS,
	/** What's new in each version. */
	CHANGELOG,
	/** Copying progress to another device, or loading it from one, as a save code. */
	SAVE_CODE
}
