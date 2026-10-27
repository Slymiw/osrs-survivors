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
import com.osrssurvivors.render.TouchControls;
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
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.events.WheelEvent;
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
	/** Game pixels the changelog scrolls for a wheel "line", and each frame an up or down key is held. */
	private static final int WHEEL_LINE = 16;
	private static final int CHANGELOG_KEY_STEP = 8;
	/** How often a run in progress is saved, in case the page closes. */
	private static final double AUTOSAVE_MS = 5000;
	private static final Color MUTED = new Color(255, 152, 31);
	private static final Color UNMUTED = new Color(0, 230, 0);
	/** Window widths per pixel of the game, so a tablet held upright isn't drawn too large to fit. */
	private static final int WIDTH_PER_PIXEL_SCALE = 380;
	/** Game pixels a finger can wander and still tap, and how long it rests on an item to show what it is. */
	private static final int TAP_SLOP = 10;
	private static final double LONG_PRESS_MS = 380;
	/** A flicked changelog slows by this share a second; a finger resting this long before lifting doesn't flick it. */
	private static final double FLING_DECAY = 4.5;
	private static final double FLING_REST_MS = 90;

	private enum Command
	{
		TOGGLE_PAUSE, CHOOSE_1, CHOOSE_2, CHOOSE_3, RESTART, EXIT, TOGGLE_BAG, MUTE,
		/** The browser's or phone's back button. */
		BACK
	}

	/** A pointer event; the page treats a pointer as a finger unless it's a mouse. */
	interface PointerEvent extends MouseEvent
	{
		@JSProperty
		int getPointerId();

		@JSProperty
		String getPointerType();
	}

	private enum FingerMove
	{
		DOWN, MOVE, UP, CANCEL
	}

	/** A finger going down, moving or coming up, handled at the start of the next frame. */
	private static final class Finger
	{
		final int id;
		final FingerMove move;
		final Point at;

		Finger(int id, FingerMove move, Point at)
		{
			this.id = id;
			this.move = move;
			this.at = at;
		}
	}

	/** What the pressing finger went down on. */
	private enum Hold
	{
		NONE,
		/** A button, pressed when the finger comes up on it. */
		BUTTON,
		/** A bag slot: tapped to use, held to look at, dragged to move or out of the bag to drop. */
		SLOT,
		/** The changelog's list, dragged and flicked. */
		SCROLL,
		/** A volume bar, slid along. */
		VOLUME,
		/** The pause screen, where a tap shows what's under it until the next one. */
		TILE
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
	/** Mouse wheel movement not yet applied, in game pixels. */
	private double wheel;
	/** Where on the changelog's scrollbar thumb the mouse holds it, or -1 when not dragging it. */
	private int thumbGrab = -1;

	/** Laid out for fingers: whichever was pressed last, a finger or the mouse, decides. */
	private boolean touch;
	private final TouchControls touchUi = new TouchControls();
	private final Queue<Finger> fingers = new ArrayDeque<>();
	/** Where each finger came down, for the full screen button (which must be pressed inside the browser's event). */
	private final Map<Integer, Point> fingerDowns = new HashMap<>();
	/** The finger steering the player, and the one pressing the interface, or -1. */
	private int stickFinger = -1;
	private int pressFinger = -1;
	private Hold hold = Hold.NONE;
	private UiLayout.Button heldButton;
	private Point pressStart;
	private double pressTime;
	private boolean pressMoved;
	/** The changelog under a finger: where it was last, when, and how fast the list is gliding (game pixels a second). */
	private int scrollLastY;
	private double scrollLastTime;
	private double flingSpeed;
	private double flingLast;
	/** While paused on a screen with no room beside the bag, the bag is tucked away (see {@link UiLayout#bagCrowdsPause}). */
	private boolean bagTucked;

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
		touch = Browser.coarsePointer() || Browser.hasParam("touch");
		resize();
		// a phone held upright has little room: the bag starts tucked away, a tap on its tab away
		bagShown = !touch || screen.getWidth() >= screen.getHeight();
		meta = Saves.loadMeta();
		metaStored = meta.getChanges();
		options = Saves.loadOptions();
		allUnlocked = devTools && Saves.loadAllUnlocked();
		showMenu();
		listen();
		Browser.trapBack(this::back);
		Window.requestAnimationFrame(this::frame);
	}

	/** The back button: asks the next frame to pause or close a page; returns whether there was one to. */
	private boolean back()
	{
		GameSession s = session;
		boolean used = s.getPhase() != Phase.TITLE || s.isOptionsOpen() || s.getTitleScreen() != TitleScreen.MAIN;
		if (used)
		{
			commands.add(Command.BACK);
		}
		return used;
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
		s.setTouch(touch);
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
		// pointer events, for the mouse and for fingers (handled apart: see touch())
		canvas.addEventListener("pointerdown", (PointerEvent e) ->
		{
			// no mouse events after a touch, and no focus scrolling
			e.preventDefault();
			Point at = toGame(e);
			if (isFinger(e))
			{
				setTouch(true);
				fingerDowns.put(e.getPointerId(), at);
				fingers.add(new Finger(e.getPointerId(), FingerMove.DOWN, at));
				return;
			}
			setTouch(false);
			if (e.getButton() == MouseEvent.LEFT_BUTTON)
			{
				clicks.add(at);
				mouseDown = true;
				fullscreenNow(at, at);
			}
			presses.add(new Press(at, true, e.getButton() == MouseEvent.RIGHT_BUTTON || e.getShiftKey()));
		});
		canvas.addEventListener("pointerup", (PointerEvent e) ->
		{
			Point at = toGame(e);
			if (isFinger(e))
			{
				fullscreenNow(fingerDowns.remove(e.getPointerId()), at);
				fingers.add(new Finger(e.getPointerId(), FingerMove.UP, at));
				return;
			}
			presses.add(new Press(at, false, false));
			mouseDown = false;
		});
		canvas.addEventListener("pointercancel", (PointerEvent e) ->
		{
			if (isFinger(e))
			{
				fingerDowns.remove(e.getPointerId());
				fingers.add(new Finger(e.getPointerId(), FingerMove.CANCEL, toGame(e)));
				return;
			}
			mouseDown = false;
		});
		canvas.addEventListener("pointermove", (PointerEvent e) ->
		{
			Point at = toGame(e);
			if (isFinger(e))
			{
				fingers.add(new Finger(e.getPointerId(), FingerMove.MOVE, at));
				return;
			}
			mouse = at;
		});
		canvas.addEventListener("pointerleave", (PointerEvent e) ->
		{
			if (!isFinger(e))
			{
				mouse = null;
				mouseDown = false;
			}
		});
		canvas.addEventListener("contextmenu", Event::preventDefault);
		canvas.addEventListener("wheel", (WheelEvent e) ->
		{
			double lines = e.getDeltaMode() == WheelEvent.DOM_DELTA_LINE ? WHEEL_LINE
				: e.getDeltaMode() == WheelEvent.DOM_DELTA_PAGE ? screen.getHeight() : 1;
			TextRectangle r = canvas.getBoundingClientRect();
			wheel += e.getDeltaY() * lines * (r.getHeight() > 0 && e.getDeltaMode() == WheelEvent.DOM_DELTA_PIXEL
				? (double) screen.getHeight() / r.getHeight() : 1);
			e.preventDefault();
		});
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

	private static boolean isFinger(PointerEvent e)
	{
		return !"mouse".equals(e.getPointerType());
	}

	/** Lays the screen out for fingers or for the mouse, as whichever was just pressed. */
	private void setTouch(boolean on)
	{
		if (touch == on)
		{
			return;
		}
		touch = on;
		session.setTouch(on);
		if (!on)
		{
			stickFinger = -1;
			pressFinger = -1;
			hold = Hold.NONE;
			touchUi.release();
		}
		mouse = null;
		mouseDown = false;
	}

	/**
	 * Browsers only go full screen while handling a press, so the full screen button is pressed
	 * here, in the event, when the press went down and came up on it; the frame only plays the click.
	 */
	private void fullscreenNow(Point down, Point up)
	{
		GameSession s = session;
		if (down == null || s == null)
		{
			return;
		}
		Rectangle view = new Rectangle(0, 0, screen.getWidth(), screen.getHeight());
		UiLayout.Button from = UiLayout.buttonAt(view, s, down);
		UiLayout.Button to = UiLayout.buttonAt(view, s, up);
		if (from != null && to != null && from.action == UiLayout.Action.FULLSCREEN && to.action == UiLayout.Action.FULLSCREEN)
		{
			Browser.toggleFullscreen();
		}
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
		if (e.isCtrlKey() || e.isAltKey() || e.isMetaKey() || Browser.typing())
		{
			// keys typed into the bug report are its own
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
		// the canvas keeps clear of a phone's notch and home bar (index.html), so measure it
		TextRectangle box = canvas.getBoundingClientRect();
		Window window = Window.current();
		int w = Math.max(1, box.getWidth() > 0 ? (int) Math.round(box.getWidth()) : window.getInnerWidth());
		int h = Math.max(1, box.getHeight() > 0 ? (int) Math.round(box.getHeight()) : window.getInnerHeight());
		pixelScale = Math.max(1, Math.min(h / HEIGHT_PER_PIXEL_SCALE, w / WIDTH_PER_PIXEL_SCALE));
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
					toggleBag(s);
					break;
				case MUTE:
					options.setMuted(!options.isMuted());
					Saves.storeOptions(options);
					fx.toast(options.isMuted() ? "Sound off" : "Sound on", "Press M to turn it " + (options.isMuted() ? "on" : "off")
						+ " again.", options.isMuted() ? MUTED : UNMUTED);
					break;
				case BACK:
					if (s.isOptionsOpen())
					{
						s.closeOptions();
					}
					else if (s.getPhase() == Phase.RUNNING)
					{
						s.togglePause();
					}
					else if (s.getPhase() == Phase.TITLE && loading == null)
					{
						s.showTitleScreen(TitleScreen.MAIN);
					}
					sounds.play(SoundCues.click());
					break;
			}
		}

		Rectangle view = new Rectangle(0, 0, screen.getWidth(), screen.getHeight());
		s.setTouch(touch);
		s.setFullscreen(Browser.fullscreenAvailable(), Browser.fullscreen());
		for (Press p = presses.poll(); p != null; p = presses.poll())
		{
			press(s, view, p);
		}
		for (Finger f = fingers.poll(); f != null; f = fingers.poll())
		{
			if (finger(s, view, f))
			{
				return;
			}
		}
		holdStill(s);
		fling(s, view);
		scrollChangelog(s, view);
		for (Point click = clicks.poll(); click != null; click = clicks.poll())
		{
			// clicks on the bag and its tab stones were handled as presses
			Rectangle side = UiLayout.sidePanel(view, s);
			if ((side == null || !side.contains(click)) && click(s, view, click))
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

		s.setVisibleArea(GameRenderer.visibleWidth(view, touch), GameRenderer.visibleHeight(view, touch));
		int keysX = (right ? 1 : 0) - (left ? 1 : 0);
		int keysY = (down ? 1 : 0) - (up ? 1 : 0);
		if (keysX != 0 || keysY != 0)
		{
			s.setMoveDirection(keysX, keysY);
		}
		else
		{
			// a finger on open ground steers; let go and the player stands still
			double[] stick = touchUi.direction();
			s.setMoveDirection(stick[0], stick[1]);
		}
		// a phone held upright has no room for the bag beside the pause screen: it tucks away
		// while paused (its tab still brings it out) and comes back as it was
		boolean crowded = UiLayout.bagCrowdsPause(view, s);
		if (crowded != bagTucked)
		{
			bagTucked = crowded;
			s.setBagShown(!crowded && bagShown);
		}
		if (s.getPhase() != Phase.TITLE || s.isOptionsOpen() || s.getTitleScreen() != TitleScreen.MAIN)
		{
			// the back button pauses or closes this
			Browser.armBack();
		}

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
		if (touch && s.getPhase() != lastPhase && pressFinger < 0)
		{
			// a tooltip a tap left up goes with the screen it was on
			mouse = null;
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
		GameRenderer.render(g, view, s, assets, mouse, EXIT_KEY, dragging ? pressedSlot : -1, fx, mouseDown, touch ? touchUi : null);
		g.dispose();
	}

	/**
	 * The bag, like the game's inventory: an item is used when the button comes up over the slot
	 * it went down on, moved when it comes up over another, and thrown away by a right-click or a
	 * shift-click.
	 */
	private void press(GameSession s, Rectangle view, Press p)
	{
		Rectangle tab = UiLayout.inventoryTab(view, s);
		if (p.down && tab != null && tab.contains(p.at))
		{
			// the inventory's tab opens and closes the bag, as in the game
			pressedSlot = -1;
			toggleBag(s);
			return;
		}
		if (!p.down)
		{
			thumbGrab = -1;
		}
		else if (pressChangelogScrollbar(s, view, p.at))
		{
			return;
		}
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

	/**
	 * The changelog's scrollbar: a press on the thumb takes hold of it, one on the track above or
	 * below it moves a viewport's height that way. Returns whether the press was on the scrollbar.
	 */
	private boolean pressChangelogScrollbar(GameSession s, Rectangle view, Point at)
	{
		Rectangle track = UiLayout.changelogTrack(view, s);
		Rectangle thumb = UiLayout.changelogThumb(view, s);
		if (track == null || thumb == null || !track.contains(at))
		{
			return false;
		}
		if (thumb.contains(at))
		{
			thumbGrab = at.y - thumb.y;
		}
		else
		{
			int page = UiLayout.changelogViewport(view, s).height - CHANGELOG_KEY_STEP;
			s.setChangelogScroll(UiLayout.changelogScroll(view, s) + (at.y < thumb.y ? -page : page));
		}
		return true;
	}

	/** Moves the changelog's list by the mouse wheel, a held scrollbar thumb, or the up and down keys. */
	private void scrollChangelog(GameSession s, Rectangle view)
	{
		double moved = wheel;
		wheel = 0;
		Rectangle track = UiLayout.changelogTrack(view, s);
		if (track == null || !mouseDown)
		{
			// the button came up outside the page
			thumbGrab = -1;
		}
		if (track == null)
		{
			return;
		}
		int max = UiLayout.maxChangelogScroll(view, s);
		double scroll = UiLayout.changelogScroll(view, s) + moved;
		if (thumbGrab >= 0 && mouse != null)
		{
			Rectangle thumb = UiLayout.changelogThumb(view, s);
			int room = track.height - thumb.height;
			scroll = room > 0 ? (double) (mouse.y - thumbGrab - track.y) * max / room : 0;
		}
		if (up != down)
		{
			scroll += up ? -CHANGELOG_KEY_STEP : CHANGELOG_KEY_STEP;
		}
		s.setChangelogScroll(Math.max(0, Math.min(max, scroll)));
	}

	private void toggleBag(GameSession s)
	{
		if (bagTucked)
		{
			// out over the pause screen, and back; as it was before pausing once play goes on
			s.setBagShown(!s.isBagShown());
			return;
		}
		bagShown = !bagShown;
		s.setBagShown(bagShown);
	}

	// ---------------------------------------------------------------- fingers

	/**
	 * A finger on a touch screen. One on open ground while playing steers the player like a
	 * stick that appears where it came down; one on the interface presses it: buttons go when the
	 * finger comes up on them, bag items are tapped, held or dragged, the changelog is dragged and
	 * flicked, volume bars slid. A second finger can press the interface while the first steers.
	 * Returns true when the session was replaced and the frame should start over.
	 */
	private boolean finger(GameSession s, Rectangle view, Finger f)
	{
		switch (f.move)
		{
			case DOWN:
				if (pressFinger >= 0 || f.id == stickFinger)
				{
					return false;
				}
				boolean open = !onInterface(s, view, f.at);
				if (open && stickFinger < 0 && s.getPhase() == Phase.RUNNING)
				{
					stickFinger = f.id;
					touchUi.stickOrigin = f.at;
					touchUi.stickAt = f.at;
					return false;
				}
				if (open && stickFinger >= 0)
				{
					// a second finger on open ground does nothing
					return false;
				}
				pressFinger = f.id;
				return beginPress(s, view, f.at);
			case MOVE:
				if (f.id == stickFinger)
				{
					touchUi.drag(f.at);
				}
				else if (f.id == pressFinger)
				{
					movePress(s, view, f.at);
				}
				return false;
			default:
				if (f.id == stickFinger)
				{
					stickFinger = -1;
					touchUi.release();
				}
				else if (f.id == pressFinger)
				{
					pressFinger = -1;
					return endPress(s, view, f.at, f.move == FingerMove.CANCEL);
				}
				return false;
		}
	}

	/** Whether a point is on the bag or a button (else it's open ground, where a finger steers). */
	private static boolean onInterface(GameSession s, Rectangle view, Point at)
	{
		Rectangle side = UiLayout.sidePanel(view, s);
		return side != null && side.contains(at) || UiLayout.buttonAt(view, s, at) != null;
	}

	private boolean beginPress(GameSession s, Rectangle view, Point at)
	{
		mouse = at;
		mouseDown = true;
		pressStart = at;
		pressTime = Browser.now();
		pressMoved = false;
		heldButton = null;
		hold = Hold.NONE;
		touchUi.inspecting = false;
		touchUi.dropping = false;
		Rectangle tab = UiLayout.inventoryTab(view, s);
		if (tab != null && tab.contains(at))
		{
			toggleBag(s);
			return false;
		}
		if (UiLayout.bagSlotAt(view, s, at) >= 0)
		{
			hold = Hold.SLOT;
			press(s, view, new Press(at, true, false));
			return false;
		}
		Rectangle side = UiLayout.sidePanel(view, s);
		if (side != null && side.contains(at))
		{
			return false;
		}
		Rectangle port = UiLayout.changelogViewport(view, s);
		if (port != null && port.contains(at))
		{
			hold = Hold.SCROLL;
			scrollLastY = at.y;
			scrollLastTime = pressTime;
			flingSpeed = 0;
			return false;
		}
		UiLayout.Button button = UiLayout.buttonAt(view, s, at);
		if (button != null && button.action == UiLayout.Action.VOLUME_BAR)
		{
			hold = Hold.VOLUME;
			heldButton = button;
			return click(s, view, at);
		}
		if (button != null)
		{
			hold = Hold.BUTTON;
			heldButton = button;
			return false;
		}
		if (s.getPhase() == Phase.BOSS_DROP)
		{
			// a tap anywhere stops the reels, or takes the drop
			return click(s, view, at);
		}
		if (s.getPhase() == Phase.PAUSED)
		{
			hold = Hold.TILE;
		}
		return false;
	}

	private void movePress(GameSession s, Rectangle view, Point at)
	{
		mouse = at;
		if (!pressMoved && Math.hypot(at.x - pressStart.x, at.y - pressStart.y) > TAP_SLOP)
		{
			pressMoved = true;
			touchUi.inspecting = false;
		}
		switch (hold)
		{
			case SCROLL:
			{
				double now = Browser.now();
				int dy = scrollLastY - at.y;
				wheel += dy;
				double seconds = (now - scrollLastTime) / 1000;
				if (seconds > 0.004)
				{
					// smoothed, so one jumpy event doesn't fling the list
					flingSpeed = 0.6 * flingSpeed + 0.4 * dy / seconds;
				}
				scrollLastY = at.y;
				scrollLastTime = now;
				break;
			}
			case VOLUME:
			{
				// slid along the bar, the level follows the finger however far it strays up or down
				Volume v = Volume.values()[heldButton.index];
				options.setVolume(v, UiLayout.volumeAt(heldButton, at));
				if (v == Volume.MASTER)
				{
					options.setMuted(false);
				}
				break;
			}
			case SLOT:
			{
				Rectangle side = UiLayout.sidePanel(view, s);
				touchUi.dropping = pressedSlot >= 0 && s.getInventory().get(pressedSlot) != null && (side == null || !side.contains(at));
				break;
			}
			default:
				break;
		}
	}

	private boolean endPress(GameSession s, Rectangle view, Point at, boolean cancelled)
	{
		mouseDown = false;
		Hold was = hold;
		hold = Hold.NONE;
		UiLayout.Button pressed = heldButton;
		heldButton = null;
		boolean replaced = false;
		switch (was)
		{
			case BUTTON:
			{
				// only when the finger comes up on the button it went down on
				UiLayout.Button under = UiLayout.buttonAt(view, s, at);
				if (!cancelled && under != null && pressed != null && under.action == pressed.action && under.index == pressed.index)
				{
					replaced = click(s, view, at);
				}
				break;
			}
			case SLOT:
				if (cancelled || touchUi.inspecting && !pressMoved)
				{
					// held to look at it, or the touch was taken away: nothing happens to it
					pressedSlot = -1;
				}
				else if (touchUi.dropping)
				{
					s.dropItem(pressedSlot);
					pressedSlot = -1;
					Browser.vibrate(15);
				}
				else
				{
					press(s, view, new Press(at, false, false));
				}
				break;
			case SCROLL:
				if (cancelled || Browser.now() - scrollLastTime > FLING_REST_MS)
				{
					flingSpeed = 0;
				}
				break;
			case VOLUME:
				Saves.storeOptions(options);
				break;
			case TILE:
				// what was tapped keeps its tooltip until the next tap
				return false;
			default:
				break;
		}
		touchUi.inspecting = false;
		touchUi.dropping = false;
		mouse = null;
		return replaced;
	}

	/** A finger resting on a bag item shows what it is; letting go then leaves the item be. */
	private void holdStill(GameSession s)
	{
		if (hold == Hold.SLOT && !pressMoved && !touchUi.inspecting && pressedSlot >= 0
			&& s.getInventory().get(pressedSlot) != null && Browser.now() - pressTime > LONG_PRESS_MS)
		{
			touchUi.inspecting = true;
			Browser.vibrate(10);
		}
	}

	/** A flicked changelog glides on, slowing. */
	private void fling(GameSession s, Rectangle view)
	{
		double now = Browser.now();
		double seconds = flingLast == 0 ? 0 : Math.min(0.1, (now - flingLast) / 1000);
		flingLast = now;
		if (UiLayout.changelogTrack(view, s) == null)
		{
			flingSpeed = 0;
		}
		if (hold == Hold.SCROLL || flingSpeed == 0)
		{
			return;
		}
		wheel += flingSpeed * seconds;
		flingSpeed *= Math.exp(-FLING_DECAY * seconds);
		if (Math.abs(flingSpeed) < 15)
		{
			flingSpeed = 0;
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
			case PAUSE:
				s.togglePause();
				break;
			case FULLSCREEN:
				// already done, in the browser's event (see fullscreenNow)
				break;
			case REPORT_BUG:
				reportBug(s, view);
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
			case SAVE_CODE:
				s.showTitleScreen(TitleScreen.SAVE_CODE);
				break;
			case COPY_SAVE_CODE:
				copySaveCode(s);
				break;
			case LOAD_SAVE_CODE:
				loadSaveCode(s);
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

	// ---------------------------------------------------------------- bug reports

	/**
	 * Opens the bug report form with what the game knows: the version, where the player is, how
	 * the screen is laid out, and (if they keep it ticked) their save code, so the run can be loaded.
	 */
	private void reportBug(GameSession s, Rectangle view)
	{
		// the save code should hold the run as it is now
		saveIfInProgress();
		storeMetaIfChanged();
		StringBuilder d = new StringBuilder();
		d.append("Version ").append(Changelog.VERSION).append('\n');
		d.append("Screen: ").append(s.getPhase());
		if (s.getPhase() == Phase.TITLE)
		{
			d.append(" / ").append(s.getTitleScreen());
		}
		d.append(s.isOptionsOpen() ? " (options open)" : "").append('\n');
		if (s.getPhase() != Phase.TITLE && s.getLocation() != null)
		{
			int seconds = (int) s.getElapsed();
			d.append("Run: ").append(s.getLocation().getTitle()).append(", ").append(seconds / 60).append(':')
				.append(seconds % 60 < 10 ? "0" : "").append(seconds % 60).append(", level ").append(s.getProgression().getLevel())
				.append(", ").append(s.getKills()).append(" kills, ")
				.append(s.getWeapon().getGear() == null ? "fists" : s.getWeapon().getGear().getTitle()).append('\n');
		}
		d.append("Layout: ").append(touch ? "touch" : "mouse").append(", ").append(view.width).append('x').append(view.height)
			.append(" game pixels at ").append(pixelScale).append("x\n");
		Browser.openBugReport(d.toString(), SaveCode.encode(Saves.exportContents()));
		// the form takes the button coming up
		mouseDown = false;
	}

	// ---------------------------------------------------------------- save codes

	/** Copies this browser's progress as a save code, or shows it to copy by hand when the clipboard can't be used. */
	private void copySaveCode(GameSession s)
	{
		storeMetaIfChanged();
		String code = SaveCode.encode(Saves.exportContents());
		if (Browser.copyText(code))
		{
			s.setStatus("Copied! On your other device, choose Load a code and paste it.");
		}
		else
		{
			Browser.showText(touch ? "Copy this save code, then load it on your other device:"
				: "Copy this save code (Ctrl+C), then load it on your other device:", code);
			s.setStatus("On your other device, choose Load a code and paste it.");
		}
		// the dialog swallowed the button coming up
		mouseDown = false;
	}

	/** Asks for a save code and, once the player agrees, replaces this browser's progress with it. */
	private void loadSaveCode(GameSession s)
	{
		String pasted = Browser.ask("Paste a save code from another device:");
		mouseDown = false;
		if (pasted == null)
		{
			return;
		}
		SaveCode.Contents contents;
		try
		{
			contents = SaveCode.decode(pasted);
		}
		catch (IllegalArgumentException e)
		{
			s.setStatus(e.getMessage());
			sounds.play(SoundCues.error());
			return;
		}
		if (!Browser.confirm("Load " + contents.describe() + "?\n\nThis replaces the progress in this browser."))
		{
			return;
		}
		Saves.importContents(contents);
		meta = Saves.loadMeta();
		metaStored = meta.getChanges();
		showMenu();
		session.setStatus("Progress loaded from your save code.");
		sounds.play(SoundCues.unlock());
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
		run.setTouch(touch);
		bagTucked = false;
		touchUi.steered = save != null;
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
