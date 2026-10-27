package com.osrssurvivors.render;

import com.osrssurvivors.game.Boost;
import com.osrssurvivors.game.Build;
import com.osrssurvivors.game.Changelog;
import com.osrssurvivors.game.GameSession;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.GearSet;
import com.osrssurvivors.game.Inventory;
import com.osrssurvivors.game.Location;
import com.osrssurvivors.game.Option;
import com.osrssurvivors.game.Volume;
import com.osrssurvivors.game.Passive;
import com.osrssurvivors.game.Perk;
import com.osrssurvivors.game.Pet;
import com.osrssurvivors.game.Phase;
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
		/** Turns the volume with this ordinal down or up a step. */
		VOLUME_DOWN,
		VOLUME_UP,
		/** The volume with this ordinal's bar: clicked, it sets the level where it was clicked ({@link #volumeAt}). */
		VOLUME_BAR,
		CLOSE_OPTIONS,
		/** Opens the changelog. */
		CHANGELOG,
		/** Opens the save code page; on it, copies this device's code or loads one from another device. */
		SAVE_CODE,
		COPY_SAVE_CODE,
		LOAD_SAVE_CODE,
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

		ReleaseCard(Rectangle bounds, Changelog.Release release, boolean latest)
		{
			this.bounds = bounds;
			this.release = release;
			this.latest = latest;
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
	/** The pause screen's narrowest, still holding its row of three buttons. */
	static final int PAUSE_MIN_W = 3 * BUTTON_W + 2 * GAP + 2 * PAD;
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
	/** A setting without its description, when the window is too short for them. */
	static final int OPTION_SHORT_H = 28;
	/** One volume row: its name, then a minus button, the level's bar and a plus button. */
	static final int VOLUME_H = 26;
	static final int VOLUME_GAP = 6;
	static final int VOLUME_BUTTON_W = 26;
	static final int VOLUME_BAR_W = 150;
	static final int OPTIONS_W = 420;
	static final int TITLE_BUTTON_W = 220;
	/** Line under the menu title (tagline, loading status, hovered location's description). */
	static final int SUBTITLE_H = 18;
	static final int CHANGELOG_W = 560;
	static final int SAVE_CODE_W = 440;
	/** Room for the save code page's explanation above its buttons. */
	public static final int SAVE_CODE_TEXT_H = 92;
	static final int CHANGELOG_MAX_H = 600;
	/** A release card's heading row (picture, version, headline, date). */
	public static final int RELEASE_HEAD = 40;
	/** One note's row on a release card. */
	public static final int NOTE_H = 24;
	static final int RELEASE_PAD = 8;
	/** The changelog's scrollbar: its width and the gap between it and the cards. */
	static final int SCROLLBAR_W = 8;
	static final int SCROLLBAR_GAP = 6;
	static final int SCROLL_THUMB_MIN = 24;
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
	// the bag is the game's side panel (resizable classic): a row of seven tab stones above and
	// below, stone pillars either side, and the inventory's four columns of seven on the stone
	// backing between, item pictures 36 x 32, 42 apart across and 36 down, as in the game
	public static final int SIDE_W = 241;
	public static final int SIDE_H = 335;
	public static final int SIDE_ROW_H = 37;
	public static final int SIDE_PILLAR_W = 26;
	public static final int BAG_COLS = 4;
	static final int BAG_SLOT_W = 36;
	static final int BAG_SLOT_H = 32;
	static final int BAG_GAP_X = 6;
	static final int BAG_GAP_Y = 4;
	/** The first slot's corner on the stone backing. */
	static final int BAG_LEFT = 16;
	static final int BAG_TOP = 6;
	/** Room left under the bag for the controls line. */
	static final int BAG_BOTTOM = 18;
	/** Room the bag leaves above it for the rest of the screen; smaller windows hide it. */
	static final int BAG_ABOVE = 100;
	/** Where the inventory's tab sits in the top row of stones. */
	static final int INVENTORY_TAB = 3;

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
			h = optionsHeight(optionRowHeight(view), optionColumns(view));
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
				if (sidePanel(view, session) != null)
				{
					// narrower, down to what its buttons need, to stand beside the bag
					w = Math.min(w, Math.max(PAUSE_MIN_W, view.width - 16 - SIDE_W - GAP));
				}
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
				else if (session.getTitleScreen() == TitleScreen.SAVE_CODE)
				{
					w = SAVE_CODE_W;
					h = TITLE_H + SUBTITLE_H + SAVE_CODE_TEXT_H + 2 * BUTTON_H + GAP + PAD;
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
		int x = view.x + (view.width - w) / 2;
		Rectangle side = sidePanel(view, session);
		if (side != null && x + w + GAP > side.x)
		{
			// the pause screen steps aside for the bag
			x = Math.max(view.x, side.x - GAP - w);
		}
		return new Rectangle(x, view.y + (view.height - h) / 2, w, h);
	}

	/** The bag panel, shown while playing and paused; null when hidden. */
	public static Rectangle bag(Rectangle view, GameSession session)
	{
		Rectangle side = sidePanel(view, session);
		return side != null && session.isBagShown() ? side : null;
	}

	/**
	 * The side panel in the bottom right while playing and paused: the open bag, or with the bag
	 * hidden just its two rows of tab stones, as the game collapses it. Null in small windows.
	 */
	public static Rectangle sidePanel(Rectangle view, GameSession session)
	{
		Phase phase = session.getPhase();
		if (session.isOptionsOpen() || phase != Phase.RUNNING && phase != Phase.PAUSED
			|| SIDE_W + 16 > view.width || SIDE_H + BAG_BOTTOM + BAG_ABOVE > view.height)
		{
			return null;
		}
		int h = session.isBagShown() ? SIDE_H : 2 * SIDE_ROW_H;
		return new Rectangle(view.x + view.width - 8 - SIDE_W, view.y + view.height - BAG_BOTTOM - h, SIDE_W, h);
	}

	/** The inventory's tab stone, which opens and closes the bag; null when the side panel is hidden. */
	public static Rectangle inventoryTab(Rectangle view, GameSession session)
	{
		Rectangle side = sidePanel(view, session);
		if (side == null)
		{
			return null;
		}
		int x = sideTabX(INVENTORY_TAB);
		return new Rectangle(side.x + x, side.y, 33, SIDE_ROW_H);
	}

	/** Left edge of a tab's 33-pixel icon box in a row of stones (the corner stones are wider). */
	public static int sideTabX(int tab)
	{
		return tab == 0 ? 3 : tab == 6 ? 205 : 39 + 33 * (tab - 1);
	}

	/** Where a bag slot is drawn. */
	public static Rectangle bagSlot(Rectangle bag, int index)
	{
		int col = index % BAG_COLS;
		int row = index / BAG_COLS;
		return new Rectangle(bag.x + SIDE_PILLAR_W + BAG_LEFT + col * (BAG_SLOT_W + BAG_GAP_X),
			bag.y + SIDE_ROW_H + BAG_TOP + row * (BAG_SLOT_H + BAG_GAP_Y), BAG_SLOT_W, BAG_SLOT_H);
	}

	/** The bag slot under a point, or -1. Bag slots are not {@link #buttons}: they are clicked, dragged and dropped. */
	public static int bagSlotAt(Rectangle view, GameSession session, Point point)
	{
		Rectangle bag = bag(view, session);
		if (bag == null || point == null)
		{
			return -1;
		}
		for (int i = 0; i < Inventory.SIZE; i++)
		{
			if (bagSlot(bag, i).contains(point))
			{
				return i;
			}
		}
		return -1;
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
			int rowH = optionRowHeight(view);
			int inner = panel.width - 2 * PAD;
			for (Volume v : Volume.values())
			{
				int right = panel.x + panel.width - PAD;
				int barW = Math.min(VOLUME_BAR_W, Math.max(40, inner / 2 - 2 * VOLUME_BUTTON_W));
				Rectangle plus = new Rectangle(right - VOLUME_BUTTON_W, y, VOLUME_BUTTON_W, VOLUME_H);
				Rectangle bar = new Rectangle(plus.x - 4 - barW, y, barW, VOLUME_H);
				Rectangle minus = new Rectangle(bar.x - 4 - VOLUME_BUTTON_W, y, VOLUME_BUTTON_W, VOLUME_H);
				buttons.add(new Button(Action.VOLUME_DOWN, v.ordinal(), minus, "-"));
				buttons.add(new Button(Action.VOLUME_BAR, v.ordinal(), bar, v.getTitle()));
				buttons.add(new Button(Action.VOLUME_UP, v.ordinal(), plus, "+"));
				y += VOLUME_H + VOLUME_GAP;
			}
			y += GAP - VOLUME_GAP;
			int cols = optionColumns(view);
			int colW = (inner - (cols - 1) * GAP) / cols;
			for (Option o : Option.values())
			{
				int col = o.ordinal() % cols;
				buttons.add(new Button(Action.TOGGLE_OPTION, o.ordinal(),
					new Rectangle(panel.x + PAD + col * (colW + GAP), y, colW, rowH), o.getTitle()));
				if (col == cols - 1 || o.ordinal() == Option.values().length - 1)
				{
					y += rowH + GAP;
				}
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
				if (session.getTitleScreen() == TitleScreen.SAVE_CODE)
				{
					addRow(buttons, panel, bottom - BUTTON_H - GAP, TITLE_BUTTON_W - 40, new Action[]{Action.COPY_SAVE_CODE, Action.LOAD_SAVE_CODE},
						"Copy my code", "Load a code");
					int pw = Math.min(BUTTON_W, panel.width - 2 * PAD);
					buttons.add(new Button(Action.BACK, 0, new Rectangle(panel.x + (panel.width - pw) / 2, bottom - BUTTON_H, pw, BUTTON_H),
						"Back"));
					break;
				}
				if (session.getTitleScreen() == TitleScreen.CHANGELOG)
				{
					int pw = Math.min(BUTTON_W, panel.width - 2 * PAD);
					buttons.add(new Button(Action.BACK, 0, new Rectangle(panel.x + (panel.width - pw) / 2, bottom - BUTTON_H, pw, BUTTON_H),
						"Back"));
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
				if (session.getMeta() != null)
				{
					buttons.add(new Button(Action.SAVE_CODE, 0, new Rectangle(bx, y, bw, BUTTON_H), "Save code (other devices)"));
					y += BUTTON_H + GAP;
				}
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
	/**
	 * Every release as a card, one list stacked down the changelog and moved up by its scroll;
	 * cards wholly outside {@link #changelogViewport} are left out, the rest are clipped to it.
	 */
	public static List<ReleaseCard> releaseCards(Rectangle view, GameSession session)
	{
		Rectangle port = changelogViewport(view, session);
		if (port == null)
		{
			return Collections.emptyList();
		}
		List<ReleaseCard> cards = new ArrayList<>();
		int y = port.y - changelogScroll(view, session);
		for (int i = 0; i < Changelog.RELEASES.size(); i++)
		{
			Changelog.Release r = Changelog.RELEASES.get(i);
			int h = releaseHeight(r);
			if (y + h > port.y && y < port.y + port.height)
			{
				cards.add(new ReleaseCard(new Rectangle(port.x, y, port.width, h), r, i == 0));
			}
			y += h + GAP;
		}
		return cards;
	}

	/** The changelog's scrolling area: between the subtitle and the Back button, beside the scrollbar. */
	public static Rectangle changelogViewport(Rectangle view, GameSession session)
	{
		Rectangle panel = panel(view, session);
		if (panel == null || session.getPhase() != Phase.TITLE || session.getTitleScreen() != TitleScreen.CHANGELOG
			|| session.isOptionsOpen())
		{
			return null;
		}
		int h = panel.height - TITLE_H - SUBTITLE_H - BUTTON_H - 2 * PAD;
		return new Rectangle(panel.x + PAD, panel.y + TITLE_H + SUBTITLE_H, panel.width - 2 * PAD - SCROLLBAR_W - SCROLLBAR_GAP,
			Math.max(0, h));
	}

	/** The scrollbar's track beside the cards; null when everything fits or off the changelog. */
	public static Rectangle changelogTrack(Rectangle view, GameSession session)
	{
		Rectangle port = changelogViewport(view, session);
		if (port == null || maxChangelogScroll(view, session) <= 0)
		{
			return null;
		}
		return new Rectangle(port.x + port.width + SCROLLBAR_GAP, port.y, SCROLLBAR_W, port.height);
	}

	/** The scrollbar's thumb: as tall as the share of the list showing, placed by the scroll. */
	public static Rectangle changelogThumb(Rectangle view, GameSession session)
	{
		Rectangle track = changelogTrack(view, session);
		if (track == null)
		{
			return null;
		}
		int content = changelogHeight();
		int h = Math.max(SCROLL_THUMB_MIN, Math.min(track.height, track.height * track.height / content));
		int max = maxChangelogScroll(view, session);
		int y = track.y + (int) Math.round((track.height - h) * (double) changelogScroll(view, session) / max);
		return new Rectangle(track.x, y, track.width, h);
	}

	/** How far the list can scroll: its height less the viewport's, never below 0. */
	public static int maxChangelogScroll(Rectangle view, GameSession session)
	{
		Rectangle port = changelogViewport(view, session);
		return port == null ? 0 : Math.max(0, changelogHeight() - port.height);
	}

	/** The session's scroll, kept within the list (the window may have grown since it was set). */
	public static int changelogScroll(Rectangle view, GameSession session)
	{
		return Math.max(0, Math.min(maxChangelogScroll(view, session), (int) Math.round(session.getChangelogScroll())));
	}

	/** Height of every release card stacked with gaps between. */
	static int changelogHeight()
	{
		int h = 0;
		for (Changelog.Release r : Changelog.RELEASES)
		{
			h += releaseHeight(r) + GAP;
		}
		return Math.max(0, h - GAP);
	}

	private static int releaseHeight(Changelog.Release r)
	{
		return RELEASE_HEAD + r.notes.size() * NOTE_H + RELEASE_PAD;
	}

	/** Height of the options page with setting rows {@code rowH} high in {@code cols} columns. */
	private static int optionsHeight(int rowH, int cols)
	{
		int rows = (Option.values().length + cols - 1) / cols;
		return TITLE_H + SUBTITLE_H + Volume.values().length * (VOLUME_H + VOLUME_GAP) + GAP - VOLUME_GAP
			+ rows * (rowH + GAP) + BUTTON_H + PAD;
	}

	/** Setting rows keep their descriptions when the window has room for them. */
	static int optionRowHeight(Rectangle view)
	{
		return optionsHeight(OPTION_H, 1) <= view.height ? OPTION_H : OPTION_SHORT_H;
	}

	/** Settings go side by side in two columns when even short rows don't fit one above another. */
	static int optionColumns(Rectangle view)
	{
		return optionsHeight(OPTION_SHORT_H, 1) <= view.height ? 1 : 2;
	}

	/** The volume level a click on a volume bar asks for: 0 at its left end, {@link Volume#MAX} at its right. */
	public static int volumeAt(Button bar, Point click)
	{
		double f = (click.x - bar.bounds.x) / (double) Math.max(1, bar.bounds.width);
		return Math.max(0, Math.min(Volume.MAX, (int) Math.round(f * Volume.MAX)));
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
		int n = 3 + (session.getContinueSummary() != null ? 1 : 0) + (session.getMeta() != null ? 3 : 0);
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
		for (Map.Entry<Boost, Double> e : build.getPotions().entrySet())
		{
			int left = (int) Math.ceil(e.getValue());
			potions.add(e.getKey().getTitle() + ": " + e.getKey().getEffect().toLowerCase() + " (" + left + " s)");
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
