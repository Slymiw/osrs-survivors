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
		new Release("0.9", "2026-10-27", "Phones and tablets",
			note("Play on a phone: drag anywhere to move, let go to stand still.", null),
			note("Big buttons to tap, a pause button, and menus that fit upright or sideways.", null),
			note("Bag: tap to use, hold to see what it is, drag out to drop.", Gear.DRAGON_SCIMITAR),
			note("Full screen on the options page; or add the page to your home screen.", null),
			note("Fixed: gaps in the inventory's stone backing.", null)),
		new Release("0.8", "2026-10-26", "The real inventory",
			note("Save codes: carry your coins, upgrades, pets and run to another device.", Pet.ORION),
			note("The inventory is the game's own side panel: stone tabs, pillars and all.", Boost.SARADOMIN_BREW),
			note("Click the backpack tab (or press I) to tuck it away, as in the game.", null),
			note("This page scrolls: wheel, drag the bar or hold up and down.", null)),
		new Release("0.7", "2026-10-26", "Sound and polish",
			note("Sound: the game's own sound effects, for every swing, spell and monster death.", Gear.STAFF_OF_FIRE),
			note("Master, combat and interface volume on the options page; M mutes.", null),
			note("Hits land with sparks as the arrow or spell arrives; monsters rise in and fall.", Gear.MAGIC_SHORTBOW),
			note("Champions arrive with a gong and fall in a gold shockwave.", EnemyKind.GIANT_MOLE),
			note("A red edge when hit hard, a heartbeat when low. New option: screen shake.", null),
			note("A cleaner screen: a level badge, a smooth XP bar, notices for big moments.", null),
			note("Fixed: summoning a champion early with the dev tools could break the game.", null)),
		new Release("0.6", "2026-10-26", "Your inventory",
			note("An inventory: click to use, drag to move, shift-click to drop. I hides it.", null),
			note("Other styles' gear waits in the bag; click its weapon to switch style.", Gear.ASCENSION_CROSSBOWS),
			note("Potions go in the bag, a minute a dose. New: smelling salts and more.", Boost.SMELLING_SALTS),
			note("Food goes in the bag too, karambwans included for combo eating.", null),
			note("Megarares: Scythe of vitur, Twisted bow, Tumeken's shadow, Obligator.", Gear.TWISTED_BOW),
			note("The fire and infernal capes show their lava, and it flows.", Gear.INFERNAL_CAPE),
			note("Lamp of Knowledge gives 5% a rank; healing from hits has a limit.", Gear.SANGUINE_SCYTHE_OF_VITUR)),
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
