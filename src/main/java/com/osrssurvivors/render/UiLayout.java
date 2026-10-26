package com.osrssurvivors.render;

import com.osrssurvivors.game.Boost;
import com.osrssurvivors.game.Build;
import com.osrssurvivors.game.Changelog;
import com.osrssurvivors.game.GameSession;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.GearSet;
import com.osrssurvivors.game.Location;
import com.osrssurvivors.game.Option;
import com.osrssurvivors.game.Passive;
import com.osrssurvivors.game.Perk;
import com.osrssurvivors.game.Pet;
import com.osrssurvivors.game.TitleScreen;
import com.osrssurvivors.game.Slot;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Screen layout of the menu panels and their buttons. Shared by drawing and click handling so
 * what is clicked is exactly what is drawn. Independent of fonts and artwork.
 */
public final class UiLayout
{
	public enum Action
	{
		CHOOSE_UPGRADE, RESUME, RESTART, EXIT, NEW_RUN, CONTINUE, CHOOSE_LOCATION, BACK, UPGRADES, BUY_PERK, REROLL, CLAIM,
		PETS,
		/** A pet's card on the pets page; only there to be looked at. */
		PET_CARD,
		OPTIONS,
		/** Turns the option with this ordinal on or off. */
		TOGGLE_OPTION,
		CLOSE_OPTIONS,
		/** Opens the changelog. */
		CHANGELOG,
		/** Turns the changelog to the page in the index. */
		CHANGELOG_PAGE,
		// developer tools, shown with ?dev
		DEV_END_RUN, DEV_LEVELS, DEV_CHAMPION, DEV_MAX_META, DEV_RESET_META, DEV_COINS, DEV_UNLOCK_MAPS
	}

	/** Something on the pause screen that shows a tooltip when hovered: a slot, a passive or a pet. */
	public static final class Tile
	{
		public final Rectangle bounds;
		/** The worn item, for equipment slots (null when empty). */
		public final Gear gear;
		public final Passive passive;
		public final int rank;
		public final Pet pet;

		Tile(Rectangle bounds, Gear gear, Passive passive, int rank, Pet pet)
		{
			this.bounds = bounds;
			this.gear = gear;
			this.passive = passive;
			this.rank = rank;
			this.pet = pet;
		}
	}

	/** One release on the changelog page. */
	public static final class ReleaseCard
	{
		public final Rectangle bounds;
		public final Changelog.Release release;
		/** Whether this is the newest release. */
		public final boolean latest;
		/** How many of its notes fit (all but on very short screens). */
		public final int notes;

		ReleaseCard(Rectangle bounds, Changelog.Release release, boolean latest, int notes)
		{
			this.bounds = bounds;
			this.release = release;
			this.latest = latest;
			this.notes = notes;
		}
	}

	/** A headed group on the pause screen's right side: tiles, or lines of text. */
	public static final class Section
	{
		public final String title;
		/** Top of the heading. */
		public final int y;
		public final List<Tile> tiles;
		public final List<String> lines;

		Section(String title, int y, List<Tile> tiles, List<String> lines)
		{
			this.title = title;
			this.y = y;
			this.tiles = tiles;
			this.lines = lines;
		}
	}

	public static final class Button
	{
		public final Action action;
		/**
		 * Upgrade index for {@link Action#CHOOSE_UPGRADE}, location ordinal for
		 * {@link Action#CHOOSE_LOCATION}, perk ordinal for {@link Action#BUY_PERK}; 0 otherwise.
		 */
		public final int index;
		public final Rectangle bounds;
		public final String label;

		Button(Action action, int index, Rectangle bounds, String label)
		{
			this.action = action;
			this.index = index;
			this.bounds = bounds;
			this.label = label;
		}
	}

	static final int PAD = 12;
	static final int GAP = 10;
	static final int TITLE_H = 38;
	static final int CARD_H = 132;
	static final int CARD_MAX_W = 200;
	static final int BUTTON_W = 130;
	static final int BUTTON_H = 30;
	/** Room for the stats line on the death screen. */
	static final int STATS_H = 26;
	/** Equipment slot squares on the pause screen, laid out like the game's equipment tab. */
	static final int SLOT = 36;
	static final int SLOT_GAP = 4;
	static final int GRID_H = 5 * SLOT + 4 * SLOT_GAP;
	// the pause screen: equipment and stats on the left, everything else in sections on the right
	static final int LOADOUT_LEFT_W = 156;
	static final int STAT_ROWS = 7;
	static final int STAT_LINE = 15;
	static final int LOADOUT_H = GRID_H + 10 + STAT_ROWS * STAT_LINE;
	static final int TILE = 34;
	static final int TILE_GAP = 6;
	static final int SECTION_HEAD = 18;
	static final int SECTION_LINE = 14;
	/** Room for the weapon's name above the sections. */
	static final int WEAPON_LINE_H = 22;
	static final int DEV_BUTTON_W = 96;
	/** One setting on the options page: its name and state, and a line describing it. */
	static final int OPTION_H = 44;
	static final int OPTIONS_W = 420;
	static final int TITLE_BUTTON_W = 220;
	/** Line under the menu title (tagline, loading status, hovered location's description). */
	static final int SUBTITLE_H = 18;
	static final int CHANGELOG_W = 560;
	static final int CHANGELOG_MAX_H = 600;
	/** A release card's heading row (picture, version, headline, date). */
	public static final int RELEASE_HEAD = 40;
	/** One note's row on a release card. */
	public static final int NOTE_H = 24;
	static final int RELEASE_PAD = 8;
	static final int LOCATION_CARD_W = 156;
	static final int LOCATION_CARD_MAX_H = 60;
	static final int LOCATION_CARD_MIN_H = 36;
	/** Coins line on the death screen. */
	static final int COINS_H = 16;
	// the boss drop's slot machine
	static final int REEL_W = 62;
	static final int REEL_H = 92;
	static final int REEL_GAP = 12;
	/** Prize name and description under the reels. */
	static final int PRIZE_H = 52;

	private UiLayout()
	{
	}

	/** The menu panel for the current phase, or null while simply playing. */
	public static Rectangle panel(Rectangle view, GameSession session)
	{
		int w;
		int h;
		if (session.isOptionsOpen())
		{
			w = OPTIONS_W;
			h = TITLE_H + SUBTITLE_H + Option.values().length * (OPTION_H + GAP) + BUTTON_H + PAD;
			w = Math.min(w, view.width);
			h = Math.min(h, view.height);
			return new Rectangle(view.x + (view.width - w) / 2, view.y + (view.height - h) / 2, w, h);
		}
		switch (session.getPhase())
		{
			case LEVEL_UP:
				int n = Math.max(1, session.getChoices().size());
				w = n * cardWidth(view, n) + (n - 1) * GAP + 2 * PAD;
				h = TITLE_H + CARD_H + PAD + (session.getRerollsLeft() > 0 ? BUTTON_H + GAP : 0);
				break;
			case BOSS_DROP:
				w = Math.max(3 * REEL_W + 2 * REEL_GAP + 2 * PAD + 60, 300);
				h = TITLE_H + REEL_H + GAP + PRIZE_H + BUTTON_H + PAD;
				break;
			case PAUSED:
				w = Math.min(640, view.width - 16);
				h = TITLE_H + Math.max(LOADOUT_H, rightHeight(session, w)) + PAD + BUTTON_H + PAD
					+ (session.isDevTools() ? BUTTON_H + GAP : 0);
				break;
			case GAME_OVER:
				w = Math.max(2 * BUTTON_W + GAP, 300) + 2 * PAD;
				h = TITLE_H + STATS_H + COINS_H + BUTTON_H + PAD;
				break;
			case TITLE:
				if (session.getTitleScreen() == TitleScreen.CHANGELOG)
				{
					w = CHANGELOG_W;
					h = Math.min(CHANGELOG_MAX_H, view.height - 16);
				}
				else if (session.getTitleScreen() != TitleScreen.MAIN)
				{
					int count = gridCount(session.getTitleScreen());
					int cols = locationColumns(view);
					int rows = gridRows(cols, count);
					w = cols * locationCardWidth(view, cols) + (cols - 1) * GAP + 2 * PAD;
					h = TITLE_H + SUBTITLE_H + rows * locationCardHeight(view, rows) + rows * GAP + BUTTON_H + PAD;
				}
				else
				{
					w = Math.max(TITLE_BUTTON_W + 40, session.isDevTools() ? 4 * DEV_BUTTON_W + 3 * GAP : 0) + 2 * PAD;
					h = TITLE_H + 20 + SUBTITLE_H + mainButtons(session) * (BUTTON_H + GAP) + PAD;
				}
				break;
			default:
				return null;
		}
		w = Math.min(w, view.width);
		h = Math.min(h, view.height);
		return new Rectangle(view.x + (view.width - w) / 2, view.y + (view.height - h) / 2, w, h);
	}

	public static List<Button> buttons(Rectangle view, GameSession session)
	{
		Rectangle panel = panel(view, session);
		if (panel == null)
		{
			return Collections.emptyList();
		}
		List<Button> buttons = new ArrayList<>();
		int bottom = panel.y + panel.height - PAD;
		if (session.isOptionsOpen())
		{
			int y = panel.y + TITLE_H + SUBTITLE_H;
			for (Option o : Option.values())
			{
				buttons.add(new Button(Action.TOGGLE_OPTION, o.ordinal(),
					new Rectangle(panel.x + PAD, y, panel.width - 2 * PAD, OPTION_H), o.getTitle()));
				y += OPTION_H + GAP;
			}
			int bw = Math.min(BUTTON_W, panel.width - 2 * PAD);
			buttons.add(new Button(Action.CLOSE_OPTIONS, 0, new Rectangle(panel.x + (panel.width - bw) / 2, bottom - BUTTON_H, bw, BUTTON_H),
				"Back"));
			return buttons;
		}
		switch (session.getPhase())
		{
			case LEVEL_UP:
				int n = session.getChoices().size();
				int cardW = cardWidth(view, n);
				int x = panel.x + PAD;
				int cardTop = panel.y + TITLE_H;
				for (int i = 0; i < n; i++)
				{
					buttons.add(new Button(Action.CHOOSE_UPGRADE, i, new Rectangle(x, cardTop, cardW, CARD_H),
						session.getChoices().get(i).getTitle()));
					x += cardW + GAP;
				}
				if (session.getRerollsLeft() > 0)
				{
					int rw = Math.min(BUTTON_W + 30, panel.width - 2 * PAD);
					buttons.add(new Button(Action.REROLL, 0, new Rectangle(panel.x + (panel.width - rw) / 2, bottom - BUTTON_H, rw, BUTTON_H),
						"Reroll (" + session.getRerollsLeft() + ")"));
				}
				break;
			case BOSS_DROP:
				int cw = Math.min(BUTTON_W, panel.width - 2 * PAD);
				buttons.add(new Button(Action.CLAIM, 0, new Rectangle(panel.x + (panel.width - cw) / 2, bottom - BUTTON_H, cw, BUTTON_H),
					session.getBossDrop() != null && session.getBossDrop().isClaimable() ? "Claim" : "Skip"));
				break;
			case PAUSED:
				addRow(buttons, panel, bottom, BUTTON_W, new Action[]{Action.RESUME, Action.OPTIONS, Action.EXIT},
					"Resume (P)", "Options", "Save & exit");
				if (session.isDevTools())
				{
					addRow(buttons, panel, bottom - BUTTON_H - GAP, DEV_BUTTON_W,
						new Action[]{Action.DEV_END_RUN, Action.DEV_LEVELS, Action.DEV_CHAMPION}, "End run", "+5 levels", "Champion now");
				}
				break;
			case GAME_OVER:
				addPair(buttons, panel, bottom, Action.RESTART, "Play again (R)", Action.EXIT, "Main menu");
				break;
			case TITLE:
				if (session.getTitleScreen() == TitleScreen.LOCATIONS)
				{
					addGrid(buttons, view, panel, Action.CHOOSE_LOCATION, Location.values().length);
					break;
				}
				if (session.getTitleScreen() == TitleScreen.UPGRADES)
				{
					addGrid(buttons, view, panel, Action.BUY_PERK, Perk.values().length);
					break;
				}
				if (session.getTitleScreen() == TitleScreen.PETS)
				{
					addGrid(buttons, view, panel, Action.PET_CARD, Pet.values().length);
					break;
				}
				if (session.getTitleScreen() == TitleScreen.CHANGELOG)
				{
					int pages = changelogPages(panel).size();
					int page = Math.min(session.getChangelogPage(), pages - 1);
					int pw = Math.min(BUTTON_W, (panel.width - 2 * PAD - 2 * GAP) / 3);
					int by = bottom - BUTTON_H;
					buttons.add(new Button(Action.BACK, 0, new Rectangle(panel.x + (panel.width - pw) / 2, by, pw, BUTTON_H), "Back"));
					if (page > 0)
					{
						buttons.add(new Button(Action.CHANGELOG_PAGE, page - 1, new Rectangle(panel.x + PAD, by, pw, BUTTON_H), "Newer"));
					}
					if (page < pages - 1)
					{
						buttons.add(new Button(Action.CHANGELOG_PAGE, page + 1,
							new Rectangle(panel.x + panel.width - PAD - pw, by, pw, BUTTON_H), "Older"));
					}
					break;
				}
				int bw = Math.min(TITLE_BUTTON_W, panel.width - 2 * PAD);
				int bx = panel.x + (panel.width - bw) / 2;
				int y = panel.y + TITLE_H + 20 + SUBTITLE_H;
				buttons.add(new Button(Action.NEW_RUN, 0, new Rectangle(bx, y, bw, BUTTON_H), "New run..."));
				y += BUTTON_H + GAP;
				if (session.getContinueSummary() != null)
				{
					buttons.add(new Button(Action.CONTINUE, 0, new Rectangle(bx, y, bw, BUTTON_H),
						"Continue (" + session.getContinueSummary() + ")"));
					y += BUTTON_H + GAP;
				}
				if (session.getMeta() != null)
				{
					buttons.add(new Button(Action.UPGRADES, 0, new Rectangle(bx, y, bw, BUTTON_H),
						"Upgrades (" + session.getMeta().coins + " coins)"));
					y += BUTTON_H + GAP;
					buttons.add(new Button(Action.PETS, 0, new Rectangle(bx, y, bw, BUTTON_H),
						"Pets (" + session.getMeta().getPets().size() + " / " + Pet.values().length + ")"));
					y += BUTTON_H + GAP;
				}
				buttons.add(new Button(Action.OPTIONS, 0, new Rectangle(bx, y, bw, BUTTON_H), "Options"));
				y += BUTTON_H + GAP;
				buttons.add(new Button(Action.CHANGELOG, 0, new Rectangle(bx, y, bw, BUTTON_H), "What's new"));
				if (session.isDevTools() && session.getMeta() != null)
				{
					addRow(buttons, panel, y + 2 * BUTTON_H + GAP, DEV_BUTTON_W,
						new Action[]{Action.DEV_MAX_META, Action.DEV_RESET_META, Action.DEV_COINS, Action.DEV_UNLOCK_MAPS},
						"Max all", "Reset all", "+1000 coins", "Unlock maps");
				}
				break;
			default:
				break;
		}
		return buttons;
	}

	/** Cards in a grid (locations or upgrades, by ordinal), then a back button. */
	private static void addGrid(List<Button> buttons, Rectangle view, Rectangle panel, Action action, int count)
	{
		int cols = locationColumns(view);
		int rows = gridRows(cols, count);
		int cw = locationCardWidth(view, cols);
		int ch = locationCardHeight(view, rows);
		int y0 = panel.y + TITLE_H + SUBTITLE_H;
		for (int i = 0; i < count; i++)
		{
			int x = panel.x + PAD + (i % cols) * (cw + GAP);
			int y = y0 + (i / cols) * (ch + GAP);
			String label = action == Action.CHOOSE_LOCATION ? Location.values()[i].getTitle()
				: action == Action.PET_CARD ? Pet.values()[i].getTitle() : Perk.values()[i].getTitle();
			buttons.add(new Button(action, i, new Rectangle(x, y, cw, ch), label));
		}
		int bw = Math.min(BUTTON_W, panel.width - 2 * PAD);
		buttons.add(new Button(Action.BACK, 0, new Rectangle(panel.x + (panel.width - bw) / 2,
			panel.y + panel.height - PAD - BUTTON_H, bw, BUTTON_H), "Back"));
	}

	static int locationColumns(Rectangle view)
	{
		int available = view.width - 24 - 2 * PAD;
		return Math.max(2, Math.min(4, (available + GAP) / (LOCATION_CARD_W + GAP)));
	}

	static int locationRows(int cols)
	{
		return gridRows(cols, Location.values().length);
	}

	static int gridRows(int cols, int count)
	{
		return (count + cols - 1) / cols;
	}

	private static int gridCount(TitleScreen screen)
	{
		switch (screen)
		{
			case UPGRADES:
				return Perk.values().length;
			case PETS:
				return Pet.values().length;
			default:
				return Location.values().length;
		}
	}

	/** Where the slot machine's reels sit in the boss drop panel. */
	public static Rectangle reel(Rectangle panel, int index)
	{
		int total = 3 * REEL_W + 2 * REEL_GAP;
		int x = panel.x + (panel.width - total) / 2 + index * (REEL_W + REEL_GAP);
		return new Rectangle(x, panel.y + TITLE_H, REEL_W, REEL_H);
	}

	static int locationCardWidth(Rectangle view, int cols)
	{
		int available = view.width - 24 - 2 * PAD - (cols - 1) * GAP;
		return Math.max(60, Math.min(LOCATION_CARD_W, available / cols));
	}

	static int locationCardHeight(Rectangle view, int rows)
	{
		int available = view.height - 16 - TITLE_H - SUBTITLE_H - BUTTON_H - PAD - rows * GAP;
		return Math.max(LOCATION_CARD_MIN_H, Math.min(LOCATION_CARD_MAX_H, available / rows));
	}

	/** The button under {@code point}, or null. */
	/** The releases on the changelog's current page, as cards stacked down the panel. */
	public static List<ReleaseCard> releaseCards(Rectangle view, GameSession session)
	{
		Rectangle panel = panel(view, session);
		if (panel == null || session.getTitleScreen() != TitleScreen.CHANGELOG || session.isOptionsOpen())
		{
			return Collections.emptyList();
		}
		List<Integer> pages = changelogPages(panel);
		int page = Math.min(session.getChangelogPage(), pages.size() - 1);
		int end = page + 1 < pages.size() ? pages.get(page + 1) : Changelog.RELEASES.size();
		List<ReleaseCard> cards = new ArrayList<>();
		int y = panel.y + TITLE_H + SUBTITLE_H;
		int room = changelogRoom(panel);
		for (int i = pages.get(page); i < end; i++)
		{
			Changelog.Release r = Changelog.RELEASES.get(i);
			// a release taller than the whole page keeps the notes that fit
			int notes = Math.max(0, Math.min(r.notes.size(), (room - RELEASE_HEAD - RELEASE_PAD) / NOTE_H));
			int h = RELEASE_HEAD + notes * NOTE_H + RELEASE_PAD;
			cards.add(new ReleaseCard(new Rectangle(panel.x + PAD, y, panel.width - 2 * PAD, h), r, i == 0, notes));
			y += h + GAP;
		}
		return cards;
	}

	/** Height for release cards between the page's subtitle and its buttons. */
	private static int changelogRoom(Rectangle panel)
	{
		return panel.height - TITLE_H - SUBTITLE_H - BUTTON_H - 2 * PAD;
	}

	/** First release of each changelog page: as many whole cards as fit above the buttons. */
	static List<Integer> changelogPages(Rectangle panel)
	{
		int room = changelogRoom(panel);
		List<Integer> starts = new ArrayList<>();
		int used = room;
		for (int i = 0; i < Changelog.RELEASES.size(); i++)
		{
			int h = releaseHeight(Changelog.RELEASES.get(i));
			if (used + h > room)
			{
				starts.add(i);
				used = 0;
			}
			used += h + GAP;
		}
		return starts;
	}

	private static int releaseHeight(Changelog.Release r)
	{
		return RELEASE_HEAD + r.notes.size() * NOTE_H + RELEASE_PAD;
	}

	public static Button buttonAt(Rectangle view, GameSession session, Point point)
	{
		if (point == null)
		{
			return null;
		}
		for (Button b : buttons(view, session))
		{
			if (b.bounds.contains(point))
			{
				return b;
			}
		}
		return null;
	}

	/** Buttons on the main menu page, for its height. */
	private static int mainButtons(GameSession session)
	{
		int n = 3 + (session.getContinueSummary() != null ? 1 : 0) + (session.getMeta() != null ? 2 : 0);
		return n + (session.isDevTools() && session.getMeta() != null ? 1 : 0);
	}

	/** Equal buttons side by side, centred, their bottoms at {@code bottom}. */
	private static void addRow(List<Button> buttons, Rectangle panel, int bottom, int maxWidth, Action[] actions, String... labels)
	{
		int n = actions.length;
		int w = Math.min(maxWidth, (panel.width - 2 * PAD - (n - 1) * GAP) / n);
		int x = panel.x + (panel.width - n * w - (n - 1) * GAP) / 2;
		int y = bottom - BUTTON_H;
		for (int i = 0; i < n; i++)
		{
			buttons.add(new Button(actions[i], 0, new Rectangle(x + i * (w + GAP), y, w, BUTTON_H), labels[i]));
		}
	}

	/** Left edge of the pause screen's right-hand side. */
	static int loadoutRightX(Rectangle panel)
	{
		return panel.x + PAD + LOADOUT_LEFT_W + 18;
	}

	/** Top of the stats table under the equipment grid. */
	static int statsTop(Rectangle panel)
	{
		return panel.y + TITLE_H + GRID_H + 10;
	}

	/**
	 * The pause screen's right side, top to bottom: abilities (with empty places up to the
	 * limit), prayers and skills, set bonuses and pets.
	 */
	public static List<Section> loadoutSections(Rectangle panel, GameSession session)
	{
		Build build = session.getBuild();
		int x = loadoutRightX(panel);
		int width = panel.x + panel.width - PAD - x;
		int cols = Math.max(1, (width + TILE_GAP) / (TILE + TILE_GAP));
		int y = panel.y + TITLE_H + WEAPON_LINE_H;
		List<Section> sections = new ArrayList<>();

		List<Tile> abilities = new ArrayList<>();
		List<Tile> skills = new ArrayList<>();
		for (Map.Entry<Passive, Integer> e : build.getRanks().entrySet())
		{
			(e.getKey().isAbility() ? abilities : skills).add(new Tile(null, null, e.getKey(), e.getValue(), null));
		}
		int learnt = abilities.size();
		while (abilities.size() < GameSession.MAX_ABILITIES)
		{
			// an empty place, so it's clear how many more can be learnt
			abilities.add(new Tile(null, null, null, 0, null));
		}
		y = addTiles(sections, "Abilities (" + learnt + " / " + GameSession.MAX_ABILITIES + ")", abilities, x, y, cols);
		if (!skills.isEmpty())
		{
			y = addTiles(sections, "Prayers and skills", skills, x, y, cols);
		}
		List<String> sets = new ArrayList<>();
		for (GearSet set : build.activeSets())
		{
			sets.add(set.getTitle() + ": " + set.getBonus());
		}
		if (!sets.isEmpty())
		{
			sections.add(new Section("Set bonuses", y, Collections.emptyList(), sets));
			y += SECTION_HEAD + sets.size() * SECTION_LINE + 6;
		}
		List<String> potions = new ArrayList<>();
		for (Map.Entry<Boost, Integer> e : build.getDoses().entrySet())
		{
			potions.add(e.getKey().getTitle() + " ×" + e.getValue());
		}
		if (!potions.isEmpty())
		{
			sections.add(new Section("Potions", y, Collections.emptyList(), potions));
			y += SECTION_HEAD + potions.size() * SECTION_LINE + 6;
		}
		List<Tile> pets = new ArrayList<>();
		for (Pet pet : build.getPets())
		{
			pets.add(new Tile(null, null, null, 0, pet));
		}
		if (!pets.isEmpty())
		{
			addTiles(sections, "Pets", pets, x, y, cols);
		}
		return sections;
	}

	/** Height the right side needs, for sizing the panel. */
	private static int rightHeight(GameSession session, int width)
	{
		// lay it out in a panel at the top of the screen and see where it ends
		Rectangle probe = new Rectangle(0, 0, width, 2000);
		int bottom = TITLE_H + WEAPON_LINE_H;
		for (Section s : loadoutSections(probe, session))
		{
			int end = s.y + SECTION_HEAD + s.lines.size() * SECTION_LINE;
			for (Tile t : s.tiles)
			{
				end = Math.max(end, t.bounds.y + t.bounds.height);
			}
			bottom = Math.max(bottom, end);
		}
		return bottom - TITLE_H;
	}

	private static int addTiles(List<Section> sections, String title, List<Tile> tiles, int x, int y, int cols)
	{
		List<Tile> placed = new ArrayList<>();
		for (int i = 0; i < tiles.size(); i++)
		{
			Tile t = tiles.get(i);
			Rectangle r = new Rectangle(x + (i % cols) * (TILE + TILE_GAP), y + SECTION_HEAD + (i / cols) * (TILE + TILE_GAP), TILE, TILE);
			placed.add(new Tile(r, t.gear, t.passive, t.rank, t.pet));
		}
		sections.add(new Section(title, y, placed, Collections.emptyList()));
		int rows = (tiles.size() + cols - 1) / cols;
		return y + SECTION_HEAD + rows * (TILE + TILE_GAP) + 4;
	}

	/** Every hoverable thing on the pause screen: equipment slots, then the sections' tiles. */
	public static List<Tile> loadoutTiles(Rectangle panel, GameSession session)
	{
		List<Tile> tiles = new ArrayList<>();
		for (Map.Entry<Slot, Rectangle> e : equipmentSlots(panel).entrySet())
		{
			tiles.add(new Tile(e.getValue(), session.getBuild().get(e.getKey()), null, 0, null));
		}
		for (Section s : loadoutSections(panel, session))
		{
			tiles.addAll(s.tiles);
		}
		return tiles;
	}

	/** Where each equipment slot is drawn on the pause screen. */
	static Map<Slot, Rectangle> equipmentSlots(Rectangle panel)
	{
		Map<Slot, Rectangle> slots = new EnumMap<>(Slot.class);
		int x0 = panel.x + PAD + (LOADOUT_LEFT_W - (3 * SLOT + 2 * SLOT_GAP)) / 2;
		int y0 = panel.y + TITLE_H;
		put(slots, Slot.HEAD, x0, y0, 1, 0);
		put(slots, Slot.CAPE, x0, y0, 0, 1);
		put(slots, Slot.NECK, x0, y0, 1, 1);
		put(slots, Slot.AMMO, x0, y0, 2, 1);
		put(slots, Slot.WEAPON, x0, y0, 0, 2);
		put(slots, Slot.BODY, x0, y0, 1, 2);
		put(slots, Slot.SHIELD, x0, y0, 2, 2);
		put(slots, Slot.LEGS, x0, y0, 1, 3);
		put(slots, Slot.HANDS, x0, y0, 0, 4);
		put(slots, Slot.FEET, x0, y0, 1, 4);
		put(slots, Slot.RING, x0, y0, 2, 4);
		return slots;
	}

	private static void put(Map<Slot, Rectangle> slots, Slot slot, int x0, int y0, int col, int row)
	{
		slots.put(slot, new Rectangle(x0 + col * (SLOT + SLOT_GAP), y0 + row * (SLOT + SLOT_GAP), SLOT, SLOT));
	}

	private static void addPair(List<Button> buttons, Rectangle panel, int bottom, Action first, String firstLabel,
		Action second, String secondLabel)
	{
		int w = Math.min(BUTTON_W, (panel.width - 2 * PAD - GAP) / 2);
		int x = panel.x + (panel.width - 2 * w - GAP) / 2;
		int y = bottom - BUTTON_H;
		buttons.add(new Button(first, 0, new Rectangle(x, y, w, BUTTON_H), firstLabel));
		buttons.add(new Button(second, 0, new Rectangle(x + w + GAP, y, w, BUTTON_H), secondLabel));
	}

	static int cardWidth(Rectangle view, int count)
	{
		int n = Math.max(1, count);
		int available = view.width - 24 - 2 * PAD - GAP * (n - 1);
		return Math.max(40, Math.min(CARD_MAX_W, available / n));
	}
}
