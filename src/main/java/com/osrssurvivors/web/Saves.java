package com.osrssurvivors.web;

import com.osrssurvivors.game.Location;
import com.osrssurvivors.game.MetaProgress;
import com.osrssurvivors.game.RunSave;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The three stored records, kept in the browser's local storage as JSON: the saved run, best
 * times per location, and the lasting profile. Unreadable records are ignored.
 */
final class Saves
{
	private static final String SAVE_KEY = "osrssurvivors.savedRun";
	private static final String BEST_TIMES_KEY = "osrssurvivors.bestTimes";
	private static final String META_KEY = "osrssurvivors.profile";

	private Saves()
	{
	}

	private static Map<String, Object> read(String key)
	{
		String json = Browser.getItem(key);
		if (json == null || json.isEmpty())
		{
			return null;
		}
		try
		{
			return Json.object(Json.parse(json));
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	// ---------------------------------------------------------------- the saved run

	static void storeRun(RunSave save)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("version", save.version);
		m.put("location", save.location);
		m.put("elapsed", save.elapsed);
		m.put("kills", save.kills);
		m.put("level", save.level);
		m.put("xp", save.xp);
		m.put("pendingLevelUps", save.pendingLevelUps);
		m.put("health", save.health);
		m.put("x", save.x);
		m.put("y", save.y);
		m.put("gear", save.gear);
		m.put("passives", save.passives);
		m.put("championsSlain", save.championsSlain);
		m.put("rerolls", save.rerolls);
		m.put("revives", save.revives);
		m.put("bonusCoins", save.bonusCoins);
		m.put("runPets", save.runPets);
		Browser.setItem(SAVE_KEY, Json.write(m));
	}

	/** The saved run if there is one this version can continue, else null. */
	static RunSave loadRun()
	{
		Map<String, Object> m = read(SAVE_KEY);
		if (m == null)
		{
			return null;
		}
		try
		{
			RunSave s = new RunSave();
			s.version = Json.integer(m.get("version"), 0);
			s.location = Json.string(m.get("location"));
			s.elapsed = Json.number(m.get("elapsed"), 0);
			s.kills = Json.integer(m.get("kills"), 0);
			s.level = Json.integer(m.get("level"), 1);
			s.xp = Json.integer(m.get("xp"), 0);
			s.pendingLevelUps = Json.integer(m.get("pendingLevelUps"), 0);
			s.health = Json.number(m.get("health"), 0);
			s.x = Json.number(m.get("x"), -1);
			s.y = Json.number(m.get("y"), -1);
			Map<String, Object> gear = Json.object(m.get("gear"));
			if (gear != null)
			{
				gear.forEach((k, v) -> s.gear.put(k, Json.string(v)));
			}
			Map<String, Object> passives = Json.object(m.get("passives"));
			if (passives != null)
			{
				passives.forEach((k, v) -> s.passives.put(k, Json.integer(v, 0)));
			}
			s.championsSlain = Json.integer(m.get("championsSlain"), 0);
			s.rerolls = m.get("rerolls") instanceof Number ? Json.integer(m.get("rerolls"), 0) : null;
			s.revives = m.get("revives") instanceof Number ? Json.integer(m.get("revives"), 0) : null;
			s.bonusCoins = Json.integer(m.get("bonusCoins"), 0);
			List<Object> pets = Json.array(m.get("runPets"));
			if (pets != null)
			{
				for (Object p : pets)
				{
					s.runPets.add(Json.string(p));
				}
			}
			return s.version == RunSave.CURRENT_VERSION ? s : null;
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	static void clearRun()
	{
		Browser.removeItem(SAVE_KEY);
	}

	// ---------------------------------------------------------------- the lasting profile

	static void storeMeta(MetaProgress meta)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("coins", meta.coins);
		m.put("perks", meta.perks == null ? new TreeMap<>() : new TreeMap<>(meta.perks));
		m.put("pets", meta.pets == null ? new ArrayList<>() : meta.pets);
		Browser.setItem(META_KEY, Json.write(m));
	}

	static MetaProgress loadMeta()
	{
		MetaProgress meta = new MetaProgress();
		Map<String, Object> m = read(META_KEY);
		if (m == null)
		{
			return meta;
		}
		meta.coins = Json.integer(m.get("coins"), 0);
		Map<String, Object> perks = Json.object(m.get("perks"));
		if (perks != null)
		{
			perks.forEach((k, v) -> meta.perks.put(k, Json.integer(v, 0)));
		}
		List<Object> pets = Json.array(m.get("pets"));
		if (pets != null)
		{
			for (Object p : pets)
			{
				String name = Json.string(p);
				if (name != null)
				{
					meta.pets.add(name);
				}
			}
		}
		return meta;
	}

	// ---------------------------------------------------------------- best times

	static Map<Location, Double> loadBestTimes()
	{
		Map<Location, Double> times = new EnumMap<>(Location.class);
		Map<String, Object> m = read(BEST_TIMES_KEY);
		if (m == null)
		{
			return times;
		}
		for (Location l : Location.values())
		{
			double t = Json.number(m.get(l.name()), 0);
			if (t > 0)
			{
				times.put(l, t);
			}
		}
		return times;
	}

	static void storeBestTimes(Map<Location, Double> times)
	{
		Map<String, Object> stored = new TreeMap<>();
		times.forEach((l, t) -> stored.put(l.name(), t));
		Browser.setItem(BEST_TIMES_KEY, Json.write(stored));
	}
}
