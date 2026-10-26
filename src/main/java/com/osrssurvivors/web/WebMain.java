package com.osrssurvivors.web;

import com.osrssurvivors.game.Arena;
import com.osrssurvivors.game.GameSession;
import com.osrssurvivors.game.Location;
import com.osrssurvivors.game.MetaProgress;
import com.osrssurvivors.game.Perk;
import com.osrssurvivors.game.Phase;
import com.osrssurvivors.game.RunSave;
import com.osrssurvivors.game.TitleScreen;
import com.osrssurvivors.render.GameRenderer;
import com.osrssurvivors.render.UiLayout;
import com.osrssurvivors.web.awt.WebImage;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import org.teavm.jso.JSBody;
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
	/** Window heights per pixel of the game: big screens draw the game larger rather than finer. */
	private static final int HEIGHT_PER_PIXEL_SCALE = 640;
	/** How often a run in progress is saved, in case the page closes. */
	private static final double AUTOSAVE_MS = 5000;

	private enum Command
	{
		TOGGLE_PAUSE, CHOOSE_1, CHOOSE_2, CHOOSE_3, RESTART, EXIT
	}

	private final HTMLCanvasElement canvas;
	private final BufferedImage screen;
	private final WebAssets assets;
	private final boolean devTools;
	private final Map<Location, MapArea> areas = new HashMap<>();

	private GameSession session;
	private MetaProgress meta;
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
	private Point mouse;

	private WebMain(HTMLCanvasElement canvas, WebAssets assets, boolean devTools)
	{
		this.canvas = canvas;
		this.assets = assets;
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
		new WebMain(canvas, assets, Browser.hasParam("dev")).start();
		Browser.status(null);
	}

	private void start()
	{
		resize();
		meta = Saves.loadMeta();
		metaStored = meta.getChanges();
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
		lastFrame = 0;
		lastPhase = s.getPhase();
		session = s;
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
			if (e.getButton() == 0)
			{
				clicks.add(toGame(e));
			}
			e.preventDefault();
		});
		canvas.addEventListener("mousemove", (MouseEvent e) -> mouse = toGame(e));
		canvas.addEventListener("mouseleave", (MouseEvent e) -> mouse = null);
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
		}
		Window.requestAnimationFrame(this::frame);
	}

	@JSBody(params = "text", script = "console.error(text);")
	private static native void log(String text);

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
			}
		}

		Rectangle view = new Rectangle(0, 0, screen.getWidth(), screen.getHeight());
		for (Point click = clicks.poll(); click != null; click = clicks.poll())
		{
			if (click(s, view, click))
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
		if (lastFrame != 0)
		{
			s.advance((now - lastFrame) / 1000);
		}
		lastFrame = now;

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
		GameRenderer.render(g, view, s, assets, mouse, EXIT_KEY);
		g.dispose();
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
				if (location.isUnlocked(s.getBestTimes()))
				{
					startRun(s, location, null);
				}
				else
				{
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
				break;
			case DEV_COINS:
				meta.addCoins(1000);
				storeMetaIfChanged();
				break;
			case BUY_PERK:
				if (meta.buy(Perk.values()[button.index]))
				{
					storeMetaIfChanged();
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
		lastFrame = 0;
		lastPhase = run.getPhase();
		session = run;
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
