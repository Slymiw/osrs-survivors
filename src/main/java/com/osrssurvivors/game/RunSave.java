package com.osrssurvivors.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A run's progress in a plain, JSON-friendly form: where, time, kills, level, hitpoints, the
 * build and the player's position. Version 1 saves (no location or position) still load;
 * version 2 positions are on the smaller arenas of the time and are moved onto the bigger ones.
 */
public final class RunSave
{
	public static final int CURRENT_VERSION = 3;

	public int version = CURRENT_VERSION;
	/** Location name, or null for older saves. */
	public String location;
	public double elapsed;
	public int kills;
	public int level;
	public int xp;
	public int pendingLevelUps;
	public double health;
	/** Player position in arena units; negative when unknown. */
	public double x = -1;
	public double y = -1;
	/** Slot name to gear name. */
	public Map<String, String> gear = new LinkedHashMap<>();
	/** Passive name to rank. */
	public Map<String, Integer> passives = new LinkedHashMap<>();
	/** Potion name to doses taken. */
	public Map<String, Integer> boosts = new LinkedHashMap<>();
	public int championsSlain;
	/** Rerolls and revivals left; null in older saves (then the run gets its full share). */
	public Integer rerolls;
	public Integer revives;
	/** Coins won from boss drops, paid out with the run's coins when it ends. */
	public int bonusCoins;
	/** Pets won this run when there is no lasting profile to keep them in. */
	public List<String> runPets = new ArrayList<>();

	/** Whether this version of the game can read the save. */
	public boolean isReadable()
	{
		return version >= 1 && version <= CURRENT_VERSION;
	}

	/** The saved run's location; null when unknown (such as the open field, which is gone). */
	public Location location()
	{
		if (location != null)
		{
			for (Location l : Location.values())
			{
				if (l.name().equals(location))
				{
					return l;
				}
			}
		}
		return null;
	}

	/** Short description for the continue button, e.g. "Varrock, level 7, 4:32". */
	public String summary()
	{
		int s = (int) elapsed;
		return String.format("%s, level %d, %d:%02d", location().getTitle(), level, s / 60, s % 60);
	}
}
