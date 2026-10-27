package com.osrssurvivors.web;

import com.osrssurvivors.audio.SoundCues;
import com.osrssurvivors.game.Arena;
import com.osrssurvivors.game.Changelog;
import com.osrssurvivors.game.Cue;
import com.osrssurvivors.game.GameSession;
import com.osrssurvivors.game.Location;
import com.osrssurvivors.game.MetaProgress;
import com.osrssurvivors.game.Option;
import com.osrssurvivors.game.Options;
import com.osrssurvivors.game.Perk;
import com.osrssurvivors.game.Phase;
import com.osrssurvivors.game.RunSave;
import com.osrssurvivors.game.TitleScreen;
import com.osrssurvivors.game.Volume;
import com.osrssurvivors.render.Fx;
import com.osrssurvivors.render.GameRenderer;
import com.osrssurvivors.render.UiLayout;
import com.osrssurvivors.web.awt.WebImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSExceptions;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.dom.html.TextRectangle;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * The browser glue: owns the {@link GameSession},
 * advances and draws it every animation frame into the page's canvas, routes keys and clicks to
 * it, loads map locations, and keeps the saved run, best times and profile in local storage.
 * The start menu shows straight away, over a plain field.
 */
public final class WebMain
{
	/** Key that saves the run and goes back to the start menu. */
	private static final String EXIT_KEY = "Q";
	/**
	 * Window heights per pixel of the game: big screens draw the game larger rather than finer.
	 * A maximised browser on a 1080p screen (about 950 tall inside) gets 2x, so text stays readable.
	 */
	private static final int HEIGHT_PER_PIXEL_SCALE = 470;
	/** How often a run in progress is saved, in case the page closes. */
	private static final double AUTOSAVE_MS = 5000;
	private static final Color MUTED = new Color(255, 152, 31);
	private static final Color UNMUTED = new Color(0, 230, 0);

	private enum Command
	{
		TOGGLE_PAUSE, CHOOSE_1, CHOOSE_2, CHOOSE_3, RESTART, EXIT, TOGGLE_BAG, MUTE
	}

	/** A mouse button going down or up, for the bag (which is clicked, dragged and dropped). */
	private static final class Press
	{
		final Point at;
		final boolean down;
		/** Right button or shift held: throw the item away. */
		final boolean drop;

		Press(Point at, boolean down, boolean drop)
		{
			this.at = at;
			this.down = down;
			this.drop = drop;
		}
	}

	private final HTMLCanvasElement canvas;
	private final BufferedImage screen;
	private final WebAssets assets;
	private final SoundSystem sounds;
	/** Sparks, shake, notices: what the run's cues look like. */
	private final Fx fx = new Fx();
	private final boolean devTools;
	private final Map<Location, MapArea> areas = new HashMap<>();

	private GameSession session;
	private MetaProgress meta;
	private Options options;
	/** Developer tool: every location can be picked (kept in local storage). */
	private boolean allUnlocked;
	private int metaStored;
	private Phase lastPhase;
	private double lastFrame;
	private double lastSave;
	private int pixelScale = 1;

	/** A location being loaded for the next run, the saved run to continue there, and the result. */
	private Location loading;
	private RunSave loadingSave;
	private boolean loaded;
	private HTMLImageElement loadedGround;
	private Int8Array loadedData;
	private String loadError;

	private boolean up;
	private boolean down;
	private boolean left;
	private boolean right;
	private final Set<String> held = new HashSet<>();
	private final Queue<Command> commands = new ArrayDeque<>();
	private final Queue<Point> clicks = new ArrayDeque<>();
	private final Queue<Press> presses = new ArrayDeque<>();
	/** The bag slot the mouse went down on, or -1; an item is used or moved when it comes up. */
	private int pressedSlot = -1;
	/** Whether the bag shows, kept from run to run. */
	private boolean bagShown = true;
	private Point mouse;
	/** Whether the left mouse button is held, so the button under it looks pressed. */
	private boolean mouseDown;

	private WebMain(HTMLCanvasElement canvas, WebAssets assets, SoundSystem sounds, boolean devTools)
	{
		this.canvas = canvas;
		this.assets = assets;
		this.sounds = sounds;
		this.devTools = devTools;
		this.screen = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		((WebImage) (Object) screen).adopt(canvas);
	}

	public static void main(String[] args)
	{
		WebAssets assets;
		try
		{
			Map<String, Object> manifest = Json.object(Json.parse(Browser.manifest()));
			ActorData actors = ActorData.decode(Browser.bytes(Browser.models()));
			assets = new WebAssets(manifest, actors);
		}
		catch (IOException | RuntimeException e)
		{
			Browser.status("Couldn't read the game's artwork: " + e.getMessage());
			return;
		}
		HTMLCanvasElement canvas = (HTMLCanvasElement) HTMLDocument.current().getElementById("game");
		new WebMain(canvas, assets, SoundSystem.load(), Browser.hasParam("dev")).start();
		Browser.status(null);
	}

	private void start()
	{
		resize();
		meta = Saves.loadMeta();
		metaStored = meta.getChanges();
		options = Saves.loadOptions();
		allUnlocked = devTools && Saves.loadAllUnlocked();
		showMenu();
		listen();
		Window.requestAnimationFrame(this::frame);
	}

	/** The start menu on the plain field, offering to continue a saved run. */
	private void showMenu()
	{
		assets.setArea(null);
		GameSession s = new GameSession(new Random(), Arena.open(GameSession.OPEN_TILES_X, GameSession.OPEN_TILES_Y));
		s.setMeta(meta);
		s.setBestTimes(Saves.loadBestTimes());
		RunSave save = Saves.loadRun();
		s.showTitle(save == null ? null : save.summary());
		s.setDevTools(devTools);
		s.setOptions(options);
		s.setAllUnlocked(allUnlocked);
		s.setChangelogNews(Changelog.hasNews(Saves.loadChangelogSeen()));
		lastFrame = 0;
		lastPhase = s.getPhase();
		session = s;
		fx.clear();
	}

	// ---------------------------------------------------------------- input

	private void listen()
	{
		Window window = Window.current();
		window.addEventListener("keydown", (KeyboardEvent e) -> key(e, true));
		window.addEventListener("keyup", (KeyboardEvent e) -> key(e, false));
		window.addEventListener("blur", (Event e) -> focusLost());
		window.addEventListener("pagehide", (Event e) -> saveIfInProgress());
		HTMLDocument.current().addEventListener("visibilitychange", (Event e) ->
		{
			if (isHidden())
			{
				saveIfInProgress();
				focusLost();
			}
		});
		canvas.addEventListener("mousedown", (MouseEvent e) ->
		{
			Point at = toGame(e);
			if (e.getButton() == MouseEvent.LEFT_BUTTON)
			{
				clicks.add(at);
				mouseDown = true;
			}
			presses.add(new Press(at, true, e.getButton() == MouseEvent.RIGHT_BUTTON || e.getShiftKey()));
			e.preventDefault();
		});
		canvas.addEventListener("mouseup", (MouseEvent e) ->
		{
			presses.add(new Press(toGame(e), false, false));
			mouseDown = false;
		});
		canvas.addEventListener("mousemove", (MouseEvent e) -> mouse = toGame(e));
		canvas.addEventListener("mouseleave", (MouseEvent e) ->
		{
			mouse = null;
			mouseDown = false;
		});
		canvas.addEventListener("contextmenu", Event::preventDefault);
	}

	@JSBody(script = "return document.visibilityState === 'hidden';")
	private static native boolean isHidden();

	private Point toGame(MouseEvent e)
	{
		TextRectangle r = canvas.getBoundingClientRect();
		double sx = canvas.getWidth() / (double) Math.max(1, r.getWidth());
		double sy = canvas.getHeight() / (double) Math.max(1, r.getHeight());
		return new Point((int) ((e.getClientX() - r.getLeft()) * sx), (int) ((e.getClientY() - r.getTop()) * sy));
	}

	private void key(KeyboardEvent e, boolean pressed)
	{
		String code = e.getCode();
		if (!pressed)
		{
			held.remove(code);
			// always release, so keys can't get stuck down
			setDirection(code, false);
			return;
		}
		if (e.isCtrlKey() || e.isAltKey() || e.isMetaKey())
		{
			return;
		}
		if (setDirection(code, true))
		{
			e.preventDefault();
			return;
		}
		Command command = commandFor(code);
		if (command != null)
		{
			if (held.add(code))
			{
				commands.add(command);
			}
			e.preventDefault();
		}
	}

	private boolean setDirection(String code, boolean pressed)
	{
		switch (code)
		{
			case "KeyW":
			case "ArrowUp":
				up = pressed;
				return true;
			case "KeyS":
			case "ArrowDown":
				down = pressed;
				return true;
			case "KeyA":
			case "ArrowLeft":
				left = pressed;
				return true;
			case "KeyD":
			case "ArrowRight":
				right = pressed;
				return true;
			default:
				return false;
		}
	}

	private static Command commandFor(String code)
	{
		switch (code)
		{
			case "KeyP":
			case "Escape":
				return Command.TOGGLE_PAUSE;
			case "Digit1":
			case "Numpad1":
				return Command.CHOOSE_1;
			case "Digit2":
			case "Numpad2":
				return Command.CHOOSE_2;
			case "Digit3":
			case "Numpad3":
				return Command.CHOOSE_3;
			case "KeyR":
				return Command.RESTART;
			case "KeyQ":
				return Command.EXIT;
			case "KeyI":
			case "Tab":
				return Command.TOGGLE_BAG;
			case "KeyM":
				return Command.MUTE;
			default:
				return null;
		}
	}

	/** Pauses and forgets held keys, whose key-up events may never arrive. */
	private void focusLost()
	{
		up = false;
		down = false;
		left = false;
		right = false;
		held.clear();
		if (session != null)
		{
			session.pause();
		}
	}

	private void resize()
	{
		Window window = Window.current();
		int w = Math.max(1, window.getInnerWidth());
		int h = Math.max(1, window.getInnerHeight());
		pixelScale = Math.max(1, h / HEIGHT_PER_PIXEL_SCALE);
		int cw = Math.max(1, w / pixelScale);
		int ch = Math.max(1, h / pixelScale);
		if (cw != canvas.getWidth() || ch != canvas.getHeight())
		{
			((WebImage) (Object) screen).resize(cw, ch);
		}
	}

	// ---------------------------------------------------------------- the frame

	private void frame(double timestamp)
	{
		try
		{
			step();
		}
		catch (RuntimeException e)
		{
			log("Frame failed: " + e);
			logStack(JSExceptions.getJSException(e));
		}
		Window.requestAnimationFrame(this::frame);
	}

	@JSBody(params = "text", script = "console.error(text);")
	private static native void log(String text);

	/** The JavaScript stack of a failure inside the browser's own code, if there is one. */
	@JSBody(params = "error", script = "if (error && error.stack) { console.error(error.stack); }")
	private static native void logStack(JSObject error);

	private void step()
	{
		// the window can change size without a resize event (e.g. inside a frame)
		resize();
		GameSession s = session;
		for (Command c = commands.poll(); c != null; c = commands.poll())
		{
			switch (c)
			{
				case TOGGLE_PAUSE:
					s.togglePause();
					sounds.play(SoundCues.click());
					break;
				case CHOOSE_1:
					if (s.getPhase() == Phase.BOSS_DROP)
					{
						s.claimDrop();
					}
					else
					{
						s.chooseUpgrade(0);
					}
					break;
				case CHOOSE_2:
					s.chooseUpgrade(1);
					break;
				case CHOOSE_3:
					s.chooseUpgrade(2);
					break;
				case RESTART:
					if (s.getPhase() == Phase.GAME_OVER)
					{
						s.restart();
					}
					else if (s.getPhase() == Phase.LEVEL_UP)
					{
						s.reroll();
					}
					break;
				case EXIT:
					if (s.getPhase() != Phase.TITLE)
					{
						exitToMenu();
						return;
					}
					break;
				case TOGGLE_BAG:
					bagShown = !bagShown;
					s.setBagShown(bagShown);
					break;
				case MUTE:
					options.setMuted(!options.isMuted());
					Saves.storeOptions(options);
					fx.toast(options.isMuted() ? "Sound off" : "Sound on", "Press M to turn it " + (options.isMuted() ? "on" : "off")
						+ " again.", options.isMuted() ? MUTED : UNMUTED);
					break;
			}
		}

		Rectangle view = new Rectangle(0, 0, screen.getWidth(), screen.getHeight());
		for (Press p = presses.poll(); p != null; p = presses.poll())
		{
			press(s, view, p);
		}
		for (Point click = clicks.poll(); click != null; click = clicks.poll())
		{
			// clicks on the bag were handled as presses
			if (UiLayout.bagSlotAt(view, s, click) < 0 && click(s, view, click))
			{
				return;
			}
		}

		if (loading != null && loaded)
		{
			finishLoading(s);
			// a finished load swaps in the run's session
			s = session;
		}

		s.setVisibleArea(GameRenderer.visibleWidth(view), GameRenderer.VIEW_HEIGHT);
		s.setMoveDirection((right ? 1 : 0) - (left ? 1 : 0), (down ? 1 : 0) - (up ? 1 : 0));

		double now = Browser.now();
		double dt = lastFrame != 0 ? (now - lastFrame) / 1000 : 0;
		if (lastFrame != 0)
		{
			s.advance(dt);
		}
		lastFrame = now;
		// what happened this frame, heard and seen
		List<Cue> happened = s.takeCues();
		fx.handle(happened, s);
		sounds.frame(happened, s.getPlayer(), options, now / 1000);
		fx.update(dt, s);

		if (s.getPhase() == Phase.GAME_OVER && lastPhase != Phase.GAME_OVER)
		{
			Saves.storeBestTimes(s.getBestTimes());
			Saves.clearRun();
		}
		lastPhase = s.getPhase();
		// coins at the end of a run, pets from a boss drop
		storeMetaIfChanged();
		if (now - lastSave > AUTOSAVE_MS)
		{
			lastSave = now;
			saveIfInProgress();
		}

		assets.beginFrame();
		Graphics2D g = screen.createGraphics();
		boolean dragging = pressedSlot >= 0 && UiLayout.bagSlotAt(view, s, mouse) != pressedSlot;
		GameRenderer.render(g, view, s, assets, mouse, EXIT_KEY, dragging ? pressedSlot : -1, fx, mouseDown);
		g.dispose();
	}

	/**
	 * The bag, like the game's inventory: an item is used when the button comes up over the slot
	 * it went down on, moved when it comes up over another, and thrown away by a right-click or a
	 * shift-click.
	 */
	private void press(GameSession s, Rectangle view, Press p)
	{
		int slot = UiLayout.bagSlotAt(view, s, p.at);
		if (p.down)
		{
			pressedSlot = -1;
			if (slot >= 0 && p.drop)
			{
				s.dropItem(slot);
			}
			else if (slot >= 0)
			{
				pressedSlot = slot;
			}
			return;
		}
		int from = pressedSlot;
		pressedSlot = -1;
		if (from < 0 || slot < 0)
		{
			return;
		}
		if (slot == from)
		{
			s.useItem(slot);
		}
		else
		{
			s.moveItem(from, slot);
		}
	}

	/** Handles a click; returns true when the session was replaced and the frame should start over. */
	private boolean click(GameSession s, Rectangle view, Point click)
	{
		UiLayout.Button button = UiLayout.buttonAt(view, s, click);
		if (button == null && s.getPhase() == Phase.BOSS_DROP)
		{
			// a click anywhere stops the reels, or takes the drop
			s.claimDrop();
			return false;
		}
		// the menu waits while a location loads
		if (button == null || loading != null)
		{
			return false;
		}
		// a click moves on from a "couldn't load" line
		s.setStatus(null);
		switch (button.action)
		{
			case CHOOSE_UPGRADE:
			case REROLL:
			case CLAIM:
			case PET_CARD:
			case BUY_PERK:
			case CHOOSE_LOCATION:
				// these make their own sounds (or none)
				break;
			default:
				sounds.play(SoundCues.click());
				break;
		}
		switch (button.action)
		{
			case CHOOSE_UPGRADE:
				s.chooseUpgrade(button.index);
				break;
			case RESUME:
				s.togglePause();
				break;
			case RESTART:
				s.restart();
				break;
			case NEW_RUN:
				s.showTitleScreen(TitleScreen.LOCATIONS);
				break;
			case BACK:
				s.showTitleScreen(TitleScreen.MAIN);
				break;
			case CHOOSE_LOCATION:
				Location location = Location.values()[button.index];
				if (s.isUnlocked(location))
				{
					sounds.play(SoundCues.click());
					startRun(s, location, null);
				}
				else
				{
					sounds.play(SoundCues.error());
					s.setStatus(location.unlockHint());
				}
				break;
			case UPGRADES:
				s.showTitleScreen(TitleScreen.UPGRADES);
				break;
			case PETS:
				s.showTitleScreen(TitleScreen.PETS);
				break;
			case PET_CARD:
				break;
			case CHANGELOG:
				s.showTitleScreen(TitleScreen.CHANGELOG);
				Saves.storeChangelogSeen(Changelog.VERSION);
				break;
			case CHANGELOG_PAGE:
				s.setChangelogPage(button.index);
				break;
			case OPTIONS:
				s.openOptions();
				break;
			case TOGGLE_OPTION:
				options.toggle(Option.values()[button.index]);
				Saves.storeOptions(options);
				break;
			case VOLUME_DOWN:
			case VOLUME_UP:
			case VOLUME_BAR:
			{
				Volume v = Volume.values()[button.index];
				int level = button.action == UiLayout.Action.VOLUME_BAR ? UiLayout.volumeAt(button, click)
					: options.getVolume(v) + (button.action == UiLayout.Action.VOLUME_UP ? 1 : -1);
				options.setVolume(v, level);
				if (v == Volume.MASTER)
				{
					options.setMuted(false);
				}
				Saves.storeOptions(options);
				break;
			}
			case CLOSE_OPTIONS:
				s.closeOptions();
				break;
			case DEV_END_RUN:
				s.devEndRun();
				break;
			case DEV_LEVELS:
				s.devGrantLevels(5);
				break;
			case DEV_CHAMPION:
				s.devSummonChampion();
				break;
			case DEV_MAX_META:
				meta.maxOut();
				storeMetaIfChanged();
				break;
			case DEV_RESET_META:
				meta.reset();
				storeMetaIfChanged();
				setAllUnlocked(s, false);
				break;
			case DEV_UNLOCK_MAPS:
				setAllUnlocked(s, true);
				sounds.play(SoundCues.unlock());
				break;
			case DEV_COINS:
				meta.addCoins(1000);
				storeMetaIfChanged();
				break;
			case BUY_PERK:
				if (meta.buy(Perk.values()[button.index]))
				{
					sounds.play(SoundCues.purchase());
					storeMetaIfChanged();
				}
				else
				{
					sounds.play(SoundCues.error());
				}
				break;
			case REROLL:
				s.reroll();
				break;
			case CLAIM:
				s.claimDrop();
				break;
			case CONTINUE:
				RunSave save = Saves.loadRun();
				if (save != null)
				{
					startRun(s, save.location(), save);
				}
				break;
			case EXIT:
				exitToMenu();
				return true;
		}
		return session != s;
	}

	// ---------------------------------------------------------------- runs and locations

	/**
	 * Starts a run in a location, continuing {@code save} if not null. A location loads its data
	 * the first time, with a loading line on the menu.
	 */
	private void startRun(GameSession menu, Location location, RunSave save)
	{
		MapArea area = areas.get(location);
		if (area != null)
		{
			assets.setArea(area);
			begin(menu, new GameSession(new Random(), area.arena, location), save);
			return;
		}
		loading = location;
		loadingSave = save;
		loaded = false;
		menu.setStatus("Loading " + location.getTitle() + "...");
		Browser.loadLocation(location.name(), (ground, data, error) ->
		{
			loadedGround = ground;
			loadedData = data;
			loadError = error;
			loaded = true;
		});
	}

	private void finishLoading(GameSession menu)
	{
		Location location = loading;
		RunSave save = loadingSave;
		loading = null;
		loadingSave = null;
		loaded = false;
		MapArea area = null;
		String problem = loadError;
		if (problem == null)
		{
			try
			{
				BufferedImage ground = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
				((WebImage) (Object) ground).load(loadedGround);
				area = MapArea.decode(Browser.bytes(loadedData), ground);
			}
			catch (IOException | RuntimeException e)
			{
				problem = e.getMessage();
			}
		}
		loadedGround = null;
		loadedData = null;
		loadError = null;
		if (area == null)
		{
			menu.setStatus("Couldn't load " + location.getTitle() + (problem == null ? "." : ": " + problem));
			return;
		}
		// the arenas are big: keep only the latest one loaded
		areas.clear();
		areas.put(location, area);
		assets.setArea(area);
		begin(menu, new GameSession(new Random(), area.arena, location), save);
	}

	/** Replaces the menu's session with a run's, keeping the best times. */
	private void begin(GameSession menu, GameSession run, RunSave save)
	{
		run.setMeta(meta);
		run.setBestTimes(menu.getBestTimes());
		if (save != null)
		{
			run.load(save);
		}
		run.setDevTools(devTools);
		run.setOptions(options);
		run.setAllUnlocked(allUnlocked);
		run.setBagShown(bagShown);
		lastFrame = 0;
		lastPhase = run.getPhase();
		session = run;
		fx.clear();
	}

	private void setAllUnlocked(GameSession s, boolean unlocked)
	{
		allUnlocked = unlocked;
		s.setAllUnlocked(unlocked);
		Saves.storeAllUnlocked(unlocked);
	}

	/** Saves an unfinished run and goes back to the start menu. */
	private void exitToMenu()
	{
		saveIfInProgress();
		if (session.getPhase() == Phase.GAME_OVER)
		{
			Saves.clearRun();
		}
		storeMetaIfChanged();
		showMenu();
	}

	private void saveIfInProgress()
	{
		GameSession s = session;
		if (s == null)
		{
			return;
		}
		switch (s.getPhase())
		{
			case RUNNING:
			case PAUSED:
			case LEVEL_UP:
				if (s.getElapsed() > 0)
				{
					Saves.storeRun(s.save());
				}
				break;
			default:
				break;
		}
	}

	/** Stores the profile if it changed since it was last stored. */
	private void storeMetaIfChanged()
	{
		if (meta.getChanges() != metaStored)
		{
			metaStored = meta.getChanges();
			Saves.storeMeta(meta);
		}
	}
}
