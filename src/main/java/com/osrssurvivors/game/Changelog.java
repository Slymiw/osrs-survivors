package com.osrssurvivors.game;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * What changed in each version, newest first, for the main menu's "What's new" page. Add a
 * release at the top for every update players should hear about; {@link #VERSION} follows it, and
 * the menu highlights the page until each player has opened it once.
 */
public final class Changelog
{
	/**
	 * One line of a release, with an optional picture: a {@link Location}, {@link Gear}, {@link Pet},
	 * {@link Perk}, {@link Boost} or {@link EnemyKind}.
	 */
	public static final class Note
	{
		public final String text;
		public final Object art;

		Note(String text, Object art)
		{
			this.text = text;
			this.art = art;
		}
	}

	public static final class Release
	{
		public final String version;
		/** ISO date, e.g. 2026-10-26. */
		public final String date;
		public final String headline;
		public final List<Note> notes;

		Release(String version, String date, String headline, Note... notes)
		{
			this.version = version;
			this.date = date;
			this.headline = headline;
			this.notes = Collections.unmodifiableList(Arrays.asList(notes));
		}
	}

	public static final List<Release> RELEASES = Collections.unmodifiableList(Arrays.asList(
		new Release("0.5", "2026-10-26", "Raids and megarares",
			note("19 raid bosses join the champions: Chambers, Tombs and the Fractured Archive.", EnemyKind.TEKTON),
			note("Megarares, rarer than any pet: the Obligator, Ascension crossbows, Zorya's tome.", Gear.OBLIGATOR),
			note("13 raid pets to win, from Tektiny to Orion.", Pet.ORION),
			note("Every item has a rarity now, from Common up to Megarare.", Gear.ELYSIAN_SPIRIT_SHIELD),
			note("Out of upgrades? Potions that stack fill the offers instead.", Boost.DIVINE_SUPER_COMBAT),
			note("Overtime always ends a run, and melee holds up better late.", Gear.SANGUINE_SCYTHE_OF_VITUR)),
		new Release("0.4", "2026-10-26", "Bigger maps, smarter monsters",
			note("Every map is now 3 × 3 map squares: about four times the room.", Location.LUMBRIDGE),
			note("Monsters find their way round trees, fences and doorways.", EnemyKind.GREATER_DEMON),
			note("Al Kharid moves east into the desert.", Location.AL_KHARID),
			note("Barbarian Village is gone; the bigger maps around it cover it.", Location.VARROCK),
			note("This page: see what's new after each update.", null)),
		new Release("0.3", "2026-10-26", "Options",
			note("Turn off screen flashes, the level-up or the boss drop animation.", null),
			note("Options are on the main menu and the pause screen.", null)),
		new Release("0.2", "2026-10-26", "Level-up fanfare",
			note("Levelling up opens with rays, sparks and a bouncing banner.", Gear.DRAGON_SCIMITAR),
			note("Every run is on a real map; the open field is gone.", Location.DRAYNOR)),
		new Release("0.1", "2026-10-26", "OSRS Survivors in your browser",
			note("Open the page and play: nothing to install, no account.", null),
			note("Champions drop caskets with pets and boss-only items.", Pet.TZREK_JAD))));

	/** The game's version: the newest release. */
	public static final String VERSION = RELEASES.get(0).version;

	private Changelog()
	{
	}

	private static Note note(String text, Object art)
	{
		return new Note(text, art);
	}

	/** Whether a player who last opened the page at {@code seenVersion} (null if never) has news. */
	public static boolean hasNews(String seenVersion)
	{
		return !VERSION.equals(seenVersion);
	}
}
