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
		/** Turns full screen on or off, on the options page where the browser allows it. */
		FULLSCREEN,
		/** The pause button in the corner of a touch screen's HUD. */
		PAUSE,
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
	static final int GRID_W = 3 * SLOT + 2 * SLOT_GAP;
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
	static final int TOUCH_OPTION_SHORT_H = 36;
	/** One volume row: its name, then a minus button, the level's bar and a plus button. */
	static final int VOLUME_H = 26;
	static final int VOLUME_GAP = 6;
	static final int VOLUME_BUTTON_W = 26;
	static final int VOLUME_BAR_W = 150;
	static final int OPTIONS_W = 420;
	/** The options page with its settings in three columns. */
	static final int OPTIONS_WIDE_W = 600;
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
	/** Pets are only looked at, not pressed, so their cards can be shorter to fit a small screen. */
	static final int PET_CARD_MIN_H = 26;
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
	// touch screens: fingers need bigger buttons, and there is no controls line under the bag
	static final int TOUCH_BUTTON_H = 40;
	static final int TOUCH_VOLUME_H = 36;
	static final int TOUCH_BAG_BOTTOM = 4;
	/** The HUD's card of time, kills and coins in the top right, and the touch pause button beside it. */
	public static final int HUD_CARD_W = 118;
	public static final int PAUSE_BUTTON = 40;
	/** Height of the HUD's top row (orb, card, pause button), which the open bag stays out of. */
	static final int HUD_TOP_H = 70;
	/** On a phone held upright, level-up cards go one above another once side by side they'd be narrower than this. */
	static final int STACK_CARDS_BELOW_W = 150;
	/** Narrowest a card in a grid (locations, upgrades, pets) gets before the grid takes more rows. */
	static final int GRID_CARD_MIN_W = 100;
	/** The pause screen's stats column, when a short screen puts it beside the equipment rather than under it. */
	static final int STATS_W = 140;

	private UiLayout()
	{
	}

	/** The menu panel for the current phase, or null while simply playing. */
	public static Rectangle panel(Rectangle view, GameSession session)
	{
		int w;
		int h;
		int bh = buttonH(session);
		if (session.isOptionsOpen())
		{
			int cols = optionColumns(view, session);
			w = cols >= 3 ? Math.min(view.width - 16, OPTIONS_WIDE_W) : OPTIONS_W;
			h = optionsHeight(session, optionRowHeight(view, session), cols);
			w = Math.min(w, view.width);
			h = Math.min(h, view.height);
			return new Rectangle(view.x + (view.width - w) / 2, view.y + (view.height - h) / 2, w, h);
		}
		switch (session.getPhase())
		{
			case LEVEL_UP:
				int n = Math.max(1, session.getChoices().size());
				int reroll = session.getRerollsLeft() > 0 ? bh + GAP : 0;
				if (stackCards(view, session))
				{
					w = Math.min(view.width - 16, CARD_MAX_W + 2 * PAD + 120);
					h = TITLE_H + n * CARD_H + (n - 1) * GAP + PAD + reroll;
				}
				else
				{
					w = n * cardWidth(view, n) + (n - 1) * GAP + 2 * PAD;
					h = TITLE_H + CARD_H + PAD + reroll;
				}
				break;
			case BOSS_DROP:
				w = Math.max(3 * REEL_W + 2 * REEL_GAP + 2 * PAD + 60, 300);
				h = TITLE_H + REEL_H + GAP + PRIZE_H + bh + PAD;
				break;
			case PAUSED:
				boolean beside = statsBeside(view, session);
				w = Math.min(640, view.width - 16);
				if (!bagCrowdsPause(view, session) && sidePanel(view, session) != null)
				{
					// narrower, down to what its buttons need, to stand beside the bag
					w = Math.min(w, Math.max(beside ? loadoutLeftW(true) + 2 * PAD + 18 + 4 * (TILE + TILE_GAP) : PAUSE_MIN_W,
						view.width - 16 - SIDE_W - GAP));
				}
				h = TITLE_H + Math.max(beside ? GRID_H : LOADOUT_H, rightHeight(session, w, beside)) + PAD + bh + PAD
					+ (session.isDevTools() ? bh + GAP : 0);
				break;
			case GAME_OVER:
				w = Math.max(2 * BUTTON_W + GAP, 300) + 2 * PAD;
				h = TITLE_H + STATS_H + COINS_H + bh + PAD;
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
					h = TITLE_H + SUBTITLE_H + SAVE_CODE_TEXT_H + 2 * bh + GAP + PAD;
				}
				else if (session.getTitleScreen() != TitleScreen.MAIN)
				{
					int count = gridCount(session.getTitleScreen());
					int cols = gridColumns(view, session, count);
					int rows = gridRows(cols, count);
					w = cols * locationCardWidth(view, cols) + (cols - 1) * GAP + 2 * PAD;
					h = TITLE_H + SUBTITLE_H + rows * locationCardHeight(view, session, rows) + rows * GAP + bh + PAD;
				}
				else
				{
					int cols = mainColumns(view, session);
					int rows = (mainButtons(session) + cols - 1) / cols;
					w = Math.max(cols * TITLE_BUTTON_W + (cols - 1) * GAP + 40, session.isDevTools() ? 4 * DEV_BUTTON_W + 3 * GAP : 0) + 2 * PAD;
					h = TITLE_H + 20 + SUBTITLE_H + (rows + (devRow(session) ? 1 : 0)) * (bh + GAP) + PAD;
				}
				break;
			default:
				return null;
		}
		w = Math.min(w, view.width);
		h = Math.min(h, view.height);
		int x = view.x + (view.width - w) / 2;
		int y = view.y + (view.height - h) / 2;
		// a crowded pause screen takes the middle; the bag, tucked away, keeps out of it
		Rectangle side = bagCrowdsPause(view, session) ? null : sidePanel(view, session);
		if (side != null && x + w + GAP > side.x)
		{
			if (side.x - GAP - w >= view.x)
			{
				// the pause screen steps aside for the bag
				x = side.x - GAP - w;
			}
			else
			{
				// no room beside it (a phone held upright): above it, as far as that goes
				y = Math.max(view.y, Math.min(y, side.y - GAP - h));
			}
		}
		return new Rectangle(x, y, w, h);
	}

	/** Height of a button: taller on touch screens, for fingers. */
	public static int buttonH(GameSession session)
	{
		return session.isTouch() ? TOUCH_BUTTON_H : BUTTON_H;
	}

	/** Height of a volume row, and the width of its minus and plus buttons. */
	public static int volumeH(GameSession session)
	{
		return session.isTouch() ? TOUCH_VOLUME_H : VOLUME_H;
	}

	/** On a phone held upright with narrow cards, the level-up cards go one above another. */
	public static boolean stackCards(Rectangle view, GameSession session)
	{
		int n = Math.max(1, session.getChoices().size());
		return n > 1 && view.height > view.width && cardWidth(view, n) < STACK_CARDS_BELOW_W
			&& TITLE_H + n * CARD_H + (n - 1) * GAP + PAD + buttonH(session) + GAP <= view.height;
	}

	/** Whether a short screen puts the pause screen's stats beside the equipment, rather than under it. */
	public static boolean statsBeside(Rectangle view, GameSession session)
	{
		int bh = buttonH(session);
		return TITLE_H + LOADOUT_H + 2 * PAD + bh + (session.isDevTools() ? bh + GAP : 0) > view.height;
	}

	/**
	 * Whether the open bag leaves no room beside it for the pause screen (a phone held upright):
	 * then the bag tucks away while paused, and its tab brings it out over the pause screen.
	 */
	public static boolean bagCrowdsPause(Rectangle view, GameSession session)
	{
		return session.getPhase() == Phase.PAUSED && !session.isOptionsOpen() && sidePanel(view, session, true) != null
			&& view.width - 16 - SIDE_W - GAP < PAUSE_MIN_W;
	}

	private static int loadoutLeftW(boolean beside)
	{
		return beside ? GRID_W + 14 + STATS_W : LOADOUT_LEFT_W;
	}

	/** The main menu's buttons go in two columns when one column is taller than the screen. */
	static int mainColumns(Rectangle view, GameSession session)
	{
		int bh = buttonH(session);
		int one = TITLE_H + 20 + SUBTITLE_H + (mainButtons(session) + (devRow(session) ? 1 : 0)) * (bh + GAP) + PAD;
		return one + 30 <= view.height || view.width < 2 * TITLE_BUTTON_W + GAP + 2 * PAD + 16 ? 1 : 2;
	}

	private static boolean devRow(GameSession session)
	{
		return session.isDevTools() && session.getMeta() != null;
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
		Rectangle side = sidePanel(view, session, session.isBagShown());
		if (side != null && !session.isBagShown() && bagCrowdsPause(view, session))
		{
			// tucked away on a crowded pause screen, its stones go too if they'd cover it
			Rectangle panel = panel(view, session);
			if (panel != null && panel.intersects(side))
			{
				return null;
			}
		}
		return side;
	}

	private static Rectangle sidePanel(Rectangle view, GameSession session, boolean open)
	{
		Phase phase = session.getPhase();
		int bottom = session.isTouch() ? TOUCH_BAG_BOTTOM : BAG_BOTTOM;
		// a touch screen keeps the bag however short it is, down to what the short panel needs
		int needs = session.isTouch() ? SIDE_H - SIDE_ROW_H + bottom : SIDE_H + bottom + BAG_ABOVE;
		if (session.isOptionsOpen() || phase != Phase.RUNNING && phase != Phase.PAUSED
			|| SIDE_W + 16 > view.width || needs > view.height)
		{
			return null;
		}
		boolean shortSide = shortSide(view, session);
		int h = open ? (shortSide ? SIDE_H - SIDE_ROW_H : SIDE_H) : shortSide ? SIDE_ROW_H : 2 * SIDE_ROW_H;
		return new Rectangle(view.x + view.width - 8 - SIDE_W, view.y + view.height - bottom - h, SIDE_W, h);
	}

	/**
	 * A touch screen too short for the whole side panel (a phone on its side): the bag drops its
	 * bottom row of stones and has the top row, with the inventory's tab, under the items instead.
	 */
	public static boolean shortSide(Rectangle view, GameSession session)
	{
		return session.isTouch() && SIDE_H + TOUCH_BAG_BOTTOM > view.height;
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
		int y = shortSide(view, session) ? side.y + side.height - SIDE_ROW_H : side.y;
		return new Rectangle(side.x + x, y, 33, SIDE_ROW_H);
	}

	/** Right edge of the HUD's top row: the screen's, or left of the open bag where it reaches up beside the card. */
	public static int hudRight(Rectangle view, GameSession session)
	{
		Rectangle side = sidePanel(view, session);
		return side != null && side.y < view.y + HUD_TOP_H ? side.x - 6 : view.x + view.width - 8;
	}

	/** The pause button left of the HUD's card, on touch screens while playing; null otherwise. */
	public static Rectangle pauseButton(Rectangle view, GameSession session)
	{
		if (!session.isTouch() || session.getPhase() != Phase.RUNNING)
		{
			return null;
		}
		int right = hudRight(view, session) - HUD_CARD_W - 6;
		return new Rectangle(right - PAUSE_BUTTON, view.y + 6, PAUSE_BUTTON, PAUSE_BUTTON);
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

	/** Where a bag slot is drawn, in the open bag on this screen (see {@link #shortSide}). */
	public static Rectangle bagSlot(Rectangle view, GameSession session, Rectangle bag, int index)
	{
		Rectangle r = bagSlot(bag, index);
		if (shortSide(view, session))
		{
			r.y -= SIDE_ROW_H;
		}
		return r;
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
			if (bagSlot(view, session, bag, i).contains(point))
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
			Rectangle pause = pauseButton(view, session);
			return pause == null ? Collections.emptyList() : Collections.singletonList(new Button(Action.PAUSE, 0, pause, "Pause"));
		}
		List<Button> buttons = new ArrayList<>();
		int bottom = panel.y + panel.height - PAD;
		int bh = buttonH(session);
		boolean keys = !session.isTouch();
		if (session.isOptionsOpen())
		{
			int y = panel.y + TITLE_H + optionsSubtitleH(optionColumns(view, session));
			int rowH = optionRowHeight(view, session);
			int inner = panel.width - 2 * PAD;
			int vh = volumeH(session);
			for (Volume v : Volume.values())
			{
				int right = panel.x + panel.width - PAD;
				int barW = Math.min(VOLUME_BAR_W, Math.max(40, inner / 2 - 2 * vh));
				Rectangle plus = new Rectangle(right - vh, y, vh, vh);
				Rectangle bar = new Rectangle(plus.x - 4 - barW, y, barW, vh);
				Rectangle minus = new Rectangle(bar.x - 4 - vh, y, vh, vh);
				buttons.add(new Button(Action.VOLUME_DOWN, v.ordinal(), minus, "-"));
				buttons.add(new Button(Action.VOLUME_BAR, v.ordinal(), bar, v.getTitle()));
				buttons.add(new Button(Action.VOLUME_UP, v.ordinal(), plus, "+"));
				y += vh + VOLUME_GAP;
			}
			y += GAP - VOLUME_GAP;
			int cols = optionColumns(view, session);
			int colW = (inner - (cols - 1) * GAP) / cols;
			int rows = optionRows(session);
			for (int i = 0; i < rows; i++)
			{
				int col = i % cols;
				Rectangle r = new Rectangle(panel.x + PAD + col * (colW + GAP), y, colW, rowH);
				// full screen, where the browser has it, after the stored settings
				buttons.add(i < Option.values().length ? new Button(Action.TOGGLE_OPTION, i, r, Option.values()[i].getTitle())
					: new Button(Action.FULLSCREEN, 0, r, "Full screen"));
				if (col == cols - 1 || i == rows - 1)
				{
					y += rowH + GAP;
				}
			}
			int bw = Math.min(BUTTON_W, panel.width - 2 * PAD);
			buttons.add(new Button(Action.CLOSE_OPTIONS, 0, new Rectangle(panel.x + (panel.width - bw) / 2, bottom - bh, bw, bh),
				"Back"));
			return buttons;
		}
		switch (session.getPhase())
		{
			case LEVEL_UP:
				int n = session.getChoices().size();
				boolean stacked = stackCards(view, session);
				int cardW = stacked ? panel.width - 2 * PAD : cardWidth(view, n);
				int x = panel.x + PAD;
				int cardTop = panel.y + TITLE_H;
				for (int i = 0; i < n; i++)
				{
					buttons.add(new Button(Action.CHOOSE_UPGRADE, i, new Rectangle(x, cardTop, cardW, CARD_H),
						session.getChoices().get(i).getTitle()));
					if (stacked)
					{
						cardTop += CARD_H + GAP;
					}
					else
					{
						x += cardW + GAP;
					}
				}
				if (session.getRerollsLeft() > 0)
				{
					int rw = Math.min(BUTTON_W + 30, panel.width - 2 * PAD);
					buttons.add(new Button(Action.REROLL, 0, new Rectangle(panel.x + (panel.width - rw) / 2, bottom - bh, rw, bh),
						"Reroll (" + session.getRerollsLeft() + ")"));
				}
				break;
			case BOSS_DROP:
				int cw = Math.min(BUTTON_W, panel.width - 2 * PAD);
				buttons.add(new Button(Action.CLAIM, 0, new Rectangle(panel.x + (panel.width - cw) / 2, bottom - bh, cw, bh),
					session.getBossDrop() != null && session.getBossDrop().isClaimable() ? "Claim" : "Skip"));
				break;
			case PAUSED:
				addRow(buttons, panel, bottom, bh, BUTTON_W, new Action[]{Action.RESUME, Action.OPTIONS, Action.EXIT},
					keys ? "Resume (P)" : "Resume", "Options", "Save & exit");
				if (session.isDevTools())
				{
					addRow(buttons, panel, bottom - bh - GAP, bh, DEV_BUTTON_W,
						new Action[]{Action.DEV_END_RUN, Action.DEV_LEVELS, Action.DEV_CHAMPION}, "End run", "+5 levels", "Champion now");
				}
				break;
			case GAME_OVER:
				addPair(buttons, panel, bottom, bh, Action.RESTART, keys ? "Play again (R)" : "Play again", Action.EXIT, "Main menu");
				break;
			case TITLE:
				if (session.getTitleScreen() == TitleScreen.LOCATIONS)
				{
					addGrid(buttons, view, session, panel, Action.CHOOSE_LOCATION, Location.values().length);
					break;
				}
				if (session.getTitleScreen() == TitleScreen.UPGRADES)
				{
					addGrid(buttons, view, session, panel, Action.BUY_PERK, Perk.values().length);
					break;
				}
				if (session.getTitleScreen() == TitleScreen.PETS)
				{
					addGrid(buttons, view, session, panel, Action.PET_CARD, Pet.values().length);
					break;
				}
				if (session.getTitleScreen() == TitleScreen.SAVE_CODE)
				{
					addRow(buttons, panel, bottom - bh - GAP, bh, TITLE_BUTTON_W - 40, new Action[]{Action.COPY_SAVE_CODE, Action.LOAD_SAVE_CODE},
						"Copy my code", "Load a code");
					int pw = Math.min(BUTTON_W, panel.width - 2 * PAD);
					buttons.add(new Button(Action.BACK, 0, new Rectangle(panel.x + (panel.width - pw) / 2, bottom - bh, pw, bh),
						"Back"));
					break;
				}
				if (session.getTitleScreen() == TitleScreen.CHANGELOG)
				{
					int pw = Math.min(BUTTON_W, panel.width - 2 * PAD);
					buttons.add(new Button(Action.BACK, 0, new Rectangle(panel.x + (panel.width - pw) / 2, bottom - bh, pw, bh),
						"Back"));
					break;
				}
				addMainButtons(buttons, view, session, panel);
				break;
			default:
				break;
		}
		return buttons;
	}

	/** The main menu: one column of buttons, or two on a short screen, then the developer row. */
	private static void addMainButtons(List<Button> buttons, Rectangle view, GameSession session, Rectangle panel)
	{
		List<Action> actions = new ArrayList<>();
		List<String> labels = new ArrayList<>();
		actions.add(Action.NEW_RUN);
		labels.add("New run...");
		if (session.getContinueSummary() != null)
		{
			actions.add(Action.CONTINUE);
			labels.add("Continue (" + session.getContinueSummary() + ")");
		}
		if (session.getMeta() != null)
		{
			actions.add(Action.UPGRADES);
			labels.add("Upgrades (" + session.getMeta().coins + " coins)");
			actions.add(Action.PETS);
			labels.add("Pets (" + session.getMeta().getPets().size() + " / " + Pet.values().length + ")");
		}
		actions.add(Action.OPTIONS);
		labels.add("Options");
		if (session.getMeta() != null)
		{
			actions.add(Action.SAVE_CODE);
			labels.add("Save code (other devices)");
		}
		actions.add(Action.CHANGELOG);
		labels.add("What's new");

		int bh = buttonH(session);
		int cols = mainColumns(view, session);
		int rows = (actions.size() + cols - 1) / cols;
		int bw = Math.min(TITLE_BUTTON_W, (panel.width - 2 * PAD - (cols - 1) * GAP) / cols);
		int x0 = panel.x + (panel.width - cols * bw - (cols - 1) * GAP) / 2;
		int y0 = panel.y + TITLE_H + 20 + SUBTITLE_H;
		for (int i = 0; i < actions.size(); i++)
		{
			// down the first column, then the second
			int col = i / rows;
			int row = i % rows;
			buttons.add(new Button(actions.get(i), 0, new Rectangle(x0 + col * (bw + GAP), y0 + row * (bh + GAP), bw, bh), labels.get(i)));
		}
		if (devRow(session))
		{
			addRow(buttons, panel, y0 + rows * (bh + GAP) + bh, bh, DEV_BUTTON_W,
				new Action[]{Action.DEV_MAX_META, Action.DEV_RESET_META, Action.DEV_COINS, Action.DEV_UNLOCK_MAPS},
				"Max all", "Reset all", "+1000 coins", "Unlock maps");
		}
	}

	/** Cards in a grid (locations or upgrades, by ordinal), then a back button. */
	private static void addGrid(List<Button> buttons, Rectangle view, GameSession session, Rectangle panel, Action action, int count)
	{
		int cols = gridColumns(view, session, count);
		int rows = gridRows(cols, count);
		int cw = locationCardWidth(view, cols);
		int ch = locationCardHeight(view, session, rows);
		int y0 = panel.y + TITLE_H + SUBTITLE_H;
		for (int i = 0; i < count; i++)
		{
			int x = panel.x + PAD + (i % cols) * (cw + GAP);
			int y = y0 + (i / cols) * (ch + GAP);
			String label = action == Action.CHOOSE_LOCATION ? Location.values()[i].getTitle()
				: action == Action.PET_CARD ? Pet.values()[i].getTitle() : Perk.values()[i].getTitle();
			buttons.add(new Button(action, i, new Rectangle(x, y, cw, ch), label));
		}
		int bh = buttonH(session);
		int bw = Math.min(BUTTON_W, panel.width - 2 * PAD);
		buttons.add(new Button(Action.BACK, 0, new Rectangle(panel.x + (panel.width - bw) / 2,
			panel.y + panel.height - PAD - bh, bw, bh), "Back"));
	}

	static int locationColumns(Rectangle view)
	{
		int available = view.width - 24 - 2 * PAD;
		return Math.max(2, Math.min(4, (available + GAP) / (LOCATION_CARD_W + GAP)));
	}

	/** Columns for a grid of {@code count} cards: as for locations, or more (narrower cards) when the rows wouldn't fit. */
	static int gridColumns(Rectangle view, GameSession session, int count)
	{
		int cols = locationColumns(view);
		int most = Math.min(count, (view.width - 24 - 2 * PAD + GAP) / (GRID_CARD_MIN_W + GAP));
		int fixed = TITLE_H + SUBTITLE_H + buttonH(session) + PAD + 16;
		while (cols < most && fixed + gridRows(cols, count) * (cardMinHeight(session) + GAP) > view.height)
		{
			cols++;
		}
		return cols;
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

	private static int cardMinHeight(GameSession session)
	{
		return session.getTitleScreen() == TitleScreen.PETS ? PET_CARD_MIN_H : LOCATION_CARD_MIN_H;
	}

	static int locationCardHeight(Rectangle view, GameSession session, int rows)
	{
		int available = view.height - 16 - TITLE_H - SUBTITLE_H - buttonH(session) - PAD - rows * GAP;
		return Math.max(cardMinHeight(session), Math.min(LOCATION_CARD_MAX_H, available / rows));
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
		int h = panel.height - TITLE_H - SUBTITLE_H - buttonH(session) - 2 * PAD;
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
	private static int optionsHeight(GameSession session, int rowH, int cols)
	{
		int rows = (optionRows(session) + cols - 1) / cols;
		return TITLE_H + optionsSubtitleH(cols) + Volume.values().length * (volumeH(session) + VOLUME_GAP) + GAP - VOLUME_GAP
			+ rows * (rowH + GAP) + buttonH(session) + PAD;
	}

	/** The options page's setting rows: the stored options, and full screen where the browser has it. */
	static int optionRows(GameSession session)
	{
		return Option.values().length + (session.isFullscreenAvailable() ? 1 : 0);
	}

	/** Setting rows keep their descriptions when the window has room for them. */
	static int optionRowHeight(Rectangle view, GameSession session)
	{
		return optionsHeight(session, OPTION_H, 1) <= view.height ? OPTION_H : optionShortH(session);
	}

	/** A setting without its description: taller on a touch screen, for fingers. */
	private static int optionShortH(GameSession session)
	{
		return session.isTouch() ? TOUCH_OPTION_SHORT_H : OPTION_SHORT_H;
	}

	/**
	 * Settings go side by side in two columns when even short rows don't fit one above another,
	 * and in three (on a wider page, without its subtitle) when two columns don't fit either.
	 */
	static int optionColumns(Rectangle view, GameSession session)
	{
		int shortH = optionShortH(session);
		return optionsHeight(session, shortH, 1) <= view.height ? 1 : optionsHeight(session, shortH, 2) <= view.height ? 2 : 3;
	}

	/** The options page's subtitle, left out to make room for three columns. */
	public static int optionsSubtitleH(int cols)
	{
		return cols >= 3 ? 0 : SUBTITLE_H;
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

	/** Buttons on the main menu page, less the developer row, for its height. */
	private static int mainButtons(GameSession session)
	{
		return 3 + (session.getContinueSummary() != null ? 1 : 0) + (session.getMeta() != null ? 3 : 0);
	}

	/** Equal buttons side by side, centred, their bottoms at {@code bottom}. */
	private static void addRow(List<Button> buttons, Rectangle panel, int bottom, int bh, int maxWidth, Action[] actions, String... labels)
	{
		int n = actions.length;
		int w = Math.min(maxWidth, (panel.width - 2 * PAD - (n - 1) * GAP) / n);
		int x = panel.x + (panel.width - n * w - (n - 1) * GAP) / 2;
		int y = bottom - bh;
		for (int i = 0; i < n; i++)
		{
			buttons.add(new Button(actions[i], 0, new Rectangle(x + i * (w + GAP), y, w, bh), labels[i]));
		}
	}

	/** Left edge of the pause screen's right-hand side. */
	static int loadoutRightX(Rectangle panel, boolean beside)
	{
		return panel.x + PAD + loadoutLeftW(beside) + 18;
	}

	/** Top of the stats table: under the equipment grid, or beside it (see {@link #statsBeside}). */
	static int statsTop(Rectangle panel, boolean beside)
	{
		return beside ? panel.y + TITLE_H - 4 : panel.y + TITLE_H + GRID_H + 10;
	}

	/** Left edge of the stats table. */
	static int statsLeft(Rectangle panel, boolean beside)
	{
		return panel.x + PAD + (beside ? GRID_W + 14 : 0);
	}

	static int statsWidth(boolean beside)
	{
		return beside ? STATS_W : LOADOUT_LEFT_W;
	}

	/**
	 * The pause screen's right side, top to bottom: abilities (with empty places up to the
	 * limit), prayers and skills, set bonuses and pets.
	 */
	public static List<Section> loadoutSections(Rectangle panel, GameSession session, boolean beside)
	{
		Build build = session.getBuild();
		int x = loadoutRightX(panel, beside);
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
	private static int rightHeight(GameSession session, int width, boolean beside)
	{
		// lay it out in a panel at the top of the screen and see where it ends
		Rectangle probe = new Rectangle(0, 0, width, 2000);
		int bottom = TITLE_H + WEAPON_LINE_H;
		for (Section s : loadoutSections(probe, session, beside))
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
	public static List<Tile> loadoutTiles(Rectangle panel, GameSession session, boolean beside)
	{
		List<Tile> tiles = new ArrayList<>();
		for (Map.Entry<Slot, Rectangle> e : equipmentSlots(panel, beside).entrySet())
		{
			tiles.add(new Tile(e.getValue(), session.getBuild().get(e.getKey()), null, 0, null));
		}
		for (Section s : loadoutSections(panel, session, beside))
		{
			tiles.addAll(s.tiles);
		}
		return tiles;
	}

	/** Where each equipment slot is drawn on the pause screen. */
	static Map<Slot, Rectangle> equipmentSlots(Rectangle panel, boolean beside)
	{
		Map<Slot, Rectangle> slots = new EnumMap<>(Slot.class);
		int x0 = panel.x + PAD + (beside ? 0 : (LOADOUT_LEFT_W - GRID_W) / 2);
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

	private static void addPair(List<Button> buttons, Rectangle panel, int bottom, int bh, Action first, String firstLabel,
		Action second, String secondLabel)
	{
		int w = Math.min(BUTTON_W, (panel.width - 2 * PAD - GAP) / 2);
		int x = panel.x + (panel.width - 2 * w - GAP) / 2;
		int y = bottom - bh;
		buttons.add(new Button(first, 0, new Rectangle(x, y, w, bh), firstLabel));
		buttons.add(new Button(second, 0, new Rectangle(x + w + GAP, y, w, bh), secondLabel));
	}

	static int cardWidth(Rectangle view, int count)
	{
		int n = Math.max(1, count);
		int available = view.width - 24 - 2 * PAD - GAP * (n - 1);
		return Math.max(40, Math.min(CARD_MAX_W, available / n));
	}
}
