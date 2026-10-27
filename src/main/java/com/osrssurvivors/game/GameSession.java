package com.osrssurvivors.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * One run of the game in one location: owns all state and advances it in fixed time steps.
 * Plain Java with no client dependencies; the caller feeds it frame deltas, a movement direction
 * and player commands, and reads the state back for rendering. Also holds the start menu's state,
 * since the menu is drawn over the arena. Champions drop a casket whose unique drop is revealed
 * on slot-machine reels before its gear choices. A finished run pays out coins into the lasting
 * {@link MetaProgress}, if one is set. Not thread-safe; use from one thread.
 */
public final class GameSession
{
	/** Simulation step, in seconds. */
	public static final double STEP = 1.0 / 60;
	/** Frame deltas above this are clamped so a hitch (or a breakpoint) doesn't skip the game ahead. */
	public static final double MAX_FRAME = 0.25;
	/** Size of the plain field used when no real area is available. */
	public static final int OPEN_TILES_X = 75;
	public static final int OPEN_TILES_Y = 56;
	public static final int CHOICE_COUNT = 3;
	/** Damage taken is gathered into one hitsplat this often, like game ticks. */
	static final double PLAYER_SPLAT_INTERVAL = 0.6;
	static final double REDEMPTION_THRESHOLD = 0.15;
	static final double REDEMPTION_COOLDOWN = 45;
	/** Below this share of max hitpoints, food may be offered on level-up. */
	static final double FOOD_THRESHOLD = 0.6;
	/** Chance a level-up also offers a weapon for another combat style, kept in the bag to switch to. */
	static final double STYLE_SWITCH_CHANCE = 0.15;
	/** Chance each slot is also offered gear for a style whose weapon waits in the bag. */
	static final double OFF_STYLE_CHANCE = 0.3;
	/** Pieces of food in a food offer. */
	static final int FOOD_COUNT = 3;
	/** Seconds between drinks: three ticks, like eating. */
	static final double DRINK_DELAY = 3 * Gear.TICK;
	/** Seconds a bag message (such as a full bag) shows. */
	static final double NOTICE_SECONDS = 2.5;
	/** Chance a level-up leads with the next weapon tier when there is one. */
	static final double WEAPON_OFFER_CHANCE = 0.35;
	/** Chance a weapon offer comes with a different weapon of the same tier. */
	static final double SIDEGRADE_CHANCE = 0.25;
	/** Chance a mid-tier body or legs offer is a piece of Void knight robes instead. */
	static final double VOID_CHANCE = 0.2;
	/** Abilities (passives that attack by themselves) a run can hold, like weapon slots. */
	public static final int MAX_ABILITIES = 4;
	/** Seconds of warning shown before a champion arrives. */
	public static final double CHAMPION_WARNING = 10;
	static final double RETRIBUTION_THRESHOLD = 0.25;
	static final double RETRIBUTION_COOLDOWN = 30;
	static final double RETRIBUTION_RADIUS = 110;
	/** Monsters this close are frozen when the player revives. */
	static final double REVIVE_RADIUS = 200;
	// coins for a finished run
	static final double COINS_PER_KILL = 0.12;
	static final double COINS_PER_SECOND = 0.2;
	static final double COINS_PER_CHAMPION = 25;
	/**
	 * Healing from damage dealt (lifesteal, Blood Barrage) comes out of a store that refills by
	 * this share of max hitpoints a second and holds at most {@link #DRAIN_STORE} of them, so
	 * draining helps a lot without letting a crowd heal a build that stands in it for ever.
	 */
	static final double DRAIN_RATE = 0.03;
	static final double DRAIN_STORE = 0.15;

	private final Random random;
	private final Arena arena;
	private final Location location;
	private final PathField paths;
	private final Build build = new Build();
	private final Inventory bag = new Inventory();
	private final Abilities abilities = new Abilities();
	private final List<Enemy> enemies = new ArrayList<>();
	private final List<XpOrb> orbs = new ArrayList<>();
	private final List<Casket> caskets = new ArrayList<>();
	private final List<EnemyShot> shots = new ArrayList<>();
	private final List<Strike> strikes = new ArrayList<>();
	private final List<Offer> choices = new ArrayList<>();
	private final List<Effects.Hitsplat> hitsplats = new ArrayList<>();
	private final List<Effects.Corpse> corpses = new ArrayList<>();
	private final List<Effects.XpDrop> xpDrops = new ArrayList<>();
	private final List<Effects.Burst> bursts = new ArrayList<>();
	/** Things that happened since the page last looked, for sounds and screen effects. */
	private final List<Cue> cues = new ArrayList<>();
	private final Map<Location, Double> bestTimes = new EnumMap<>(Location.class);

	private Player player;
	private Weapon weapon;
	private Spawner spawner;
	private Progression progression;
	private Phase phase;
	private double elapsed;
	private int kills;
	/** Run time of the last kill, for {@link #stallMultiplier}. */
	private double lastKillTime;
	private int championsSlain;
	private double accumulator;
	private double moveX;
	private double moveY;
	private double spawnDistance;
	private boolean spawning = true;
	private double damageTaken;
	private double splatTimer;
	private double redemptionTimer;
	/** True while the offers on screen come from a casket rather than a level. */
	private boolean casketOpen;
	private int pendingCaskets;
	/** Damage taken from monster missiles and from touching monsters this run, for balance checks. */
	double missileDamageTaken;
	double contactDamageTaken;
	private String continueSummary;
	private TitleScreen titleScreen = TitleScreen.MAIN;
	private String status;
	private MetaProgress meta;
	/** Pets won this run (also kept in the profile when there is one). */
	private final Set<Pet> runPets = EnumSet.noneOf(Pet.class);
	private int rerollsLeft;
	private int revivesLeft;
	private boolean devTools;
	/** See {@link #getLevelUpTime()}. */
	private double levelUpTime = -1;
	private Options options = new Options();
	/** Whether the options page covers the start menu or the pause screen. */
	private boolean optionsOpen;
	/** Whether this player hasn't opened the changelog since the latest update. */
	private boolean changelogNews;
	/** Which page of the changelog is showing. */
	private int changelogPage;
	/** Real seconds the start menu has been showing, for its little animations. */
	private double menuTime;
	/** Developer tool: every location can be picked. */
	private boolean allUnlocked;
	private double retributionTimer;
	/** Hitpoints that draining can still heal right now (see {@link #DRAIN_RATE}). */
	private double drainStore;
	/** Seconds until the next bite of food, of a karambwan, and the next drink. */
	private double eatTimer;
	private double comboTimer;
	private double drinkTimer;
	/** A message about the bag, such as "not enough space", and the run time it disappears. */
	private String notice;
	private double noticeUntil;
	/** Whether the bag is shown on screen (a view setting, kept between runs). */
	private boolean bagShown = true;
	private int bonusCoins;
	/** Coins paid out when the run ended; -1 while it goes on. */
	private int coinsEarned = -1;
	/** The champion drop on the reels, or null. */
	private BossDrop bossDrop;
	/** Spatial grid for keeping enemies apart, reused every step. */
	private int[] cellHead = new int[0];
	private int[] cellNext = new int[0];

	/** A run on the plain field. */
	public GameSession(Random random)
	{
		this(random, Arena.open(OPEN_TILES_X, OPEN_TILES_Y));
	}

	/** A run on an arena with no map (the menu's backdrop, and tests), with Lumbridge's monsters. */
	public GameSession(Random random, Arena arena)
	{
		this(random, arena, Location.LUMBRIDGE);
	}

	public GameSession(Random random, Arena arena, Location location)
	{
		this.random = random;
		this.arena = arena;
		this.location = location;
		this.paths = arena.isOpen() ? null : new PathField(arena);
		setVisibleArea(800, 600);
		restart();
	}

	/** Starts a fresh run: no gear, bare fists, level 1. Keeps the arena and visible-area setting. */
	public void restart()
	{
		build.clear();
		bag.clear();
		abilities.clear();
		player = new Player(arena.getStartX(), arena.getStartY());
		weapon = new Weapon();
		spawner = new Spawner(random, location);
		progression = new Progression();
		enemies.clear();
		orbs.clear();
		caskets.clear();
		shots.clear();
		strikes.clear();
		choices.clear();
		hitsplats.clear();
		corpses.clear();
		xpDrops.clear();
		bursts.clear();
		cues.clear();
		phase = Phase.RUNNING;
		elapsed = 0;
		kills = 0;
		lastKillTime = 0;
		championsSlain = 0;
		accumulator = 0;
		moveX = 0;
		moveY = 0;
		damageTaken = 0;
		splatTimer = 0;
		redemptionTimer = 0;
		casketOpen = false;
		pendingCaskets = 0;
		status = null;
		build.setMeta(meta);
		runPets.clear();
		refreshPets();
		rerollsLeft = build.perk(Perk.REROLL);
		revivesLeft = build.perk(Perk.REVIVAL);
		retributionTimer = 0;
		drainStore = 0;
		eatTimer = 0;
		comboTimer = 0;
		drinkTimer = 0;
		notice = null;
		bonusCoins = 0;
		coinsEarned = -1;
		bossDrop = null;
		levelUpTime = -1;
		optionsOpen = false;
		applyBuild();
		player.health = player.maxHealth;
	}

	/**
	 * The lasting profile whose upgrades and pets apply to runs and which a finished run pays its
	 * coins into; null for none. Starts a fresh run.
	 */
	public void setMeta(MetaProgress meta)
	{
		this.meta = meta;
		restart();
	}

	public MetaProgress getMeta()
	{
		return meta;
	}

	private void refreshPets()
	{
		Set<Pet> pets = EnumSet.noneOf(Pet.class);
		pets.addAll(runPets);
		if (meta != null)
		{
			pets.addAll(meta.getPets());
		}
		build.setPets(pets);
	}

	/**
	 * Shows the start menu. {@code continueSummary} describes a saved run that can be continued,
	 * or is null when there is none.
	 */
	public void showTitle(String continueSummary)
	{
		restart();
		this.continueSummary = continueSummary;
		titleScreen = TitleScreen.MAIN;
		phase = Phase.TITLE;
	}

	/** Summary of the saved run offered on the start menu, or null. */
	public String getContinueSummary()
	{
		return continueSummary;
	}

	public TitleScreen getTitleScreen()
	{
		return titleScreen;
	}

	/** Switches the start menu's page; ignored outside the menu. */
	/** The display settings; one object shared with the page, which stores it. */
	public void setOptions(Options options)
	{
		this.options = options;
	}

	public Options getOptions()
	{
		return options;
	}

	/** Shows the options page over the start menu or the pause screen. */
	public void openOptions()
	{
		if (phase == Phase.TITLE || phase == Phase.PAUSED)
		{
			optionsOpen = true;
		}
	}

	public void closeOptions()
	{
		optionsOpen = false;
	}

	public boolean isOptionsOpen()
	{
		return optionsOpen && (phase == Phase.TITLE || phase == Phase.PAUSED);
	}

	/** Whether a location can be picked: opened by best times, or by the developer tool. */
	public boolean isUnlocked(Location location)
	{
		return allUnlocked || location.isUnlocked(bestTimes);
	}

	/** Developer tool: opens every location. */
	public void setAllUnlocked(boolean allUnlocked)
	{
		this.allUnlocked = allUnlocked;
	}

	public void showTitleScreen(TitleScreen screen)
	{
		if (phase == Phase.TITLE)
		{
			titleScreen = screen;
			if (screen == TitleScreen.CHANGELOG)
			{
				changelogNews = false;
				changelogPage = 0;
			}
		}
	}

	/** Whether the main menu should point the player at the changelog. */
	public boolean hasChangelogNews()
	{
		return changelogNews;
	}

	public void setChangelogNews(boolean news)
	{
		changelogNews = news;
	}

	public int getChangelogPage()
	{
		return changelogPage;
	}

	/** Turns the changelog's page; the layout keeps it in range. */
	public void setChangelogPage(int page)
	{
		changelogPage = Math.max(0, page);
	}

	/** Real seconds the start menu has been up. */
	public double getMenuTime()
	{
		return menuTime;
	}

	/** A line shown on the start menu, e.g. while a location loads; null for none. */
	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	/** Longest survival per location, shown on the location cards. */
	public Map<Location, Double> getBestTimes()
	{
		return Collections.unmodifiableMap(bestTimes);
	}

	public void setBestTimes(Map<Location, Double> times)
	{
		bestTimes.clear();
		bestTimes.putAll(times);
	}

	/** Snapshot of the run's progress for saving. */
	public RunSave save()
	{
		RunSave s = new RunSave();
		s.location = location.name();
		s.elapsed = elapsed;
		s.kills = kills;
		s.level = progression.getLevel();
		s.xp = progression.getXp();
		s.pendingLevelUps = progression.getPendingLevelUps();
		s.health = player.health;
		s.championsSlain = championsSlain;
		s.rerolls = rerollsLeft;
		s.revives = revivesLeft;
		s.bonusCoins = bonusCoins;
		for (Pet pet : runPets)
		{
			s.runPets.add(pet.name());
		}
		s.x = player.x;
		s.y = player.y;
		for (Map.Entry<Slot, Gear> e : build.getWorn().entrySet())
		{
			s.gear.put(e.getKey().name(), e.getValue().name());
		}
		for (Map.Entry<Passive, Integer> e : build.getRanks().entrySet())
		{
			s.passives.put(e.getKey().name(), e.getValue());
		}
		for (Map.Entry<Boost, Double> e : build.getPotions().entrySet())
		{
			s.potions.put(e.getKey().name(), e.getValue());
		}
		for (int i = 0; i < Inventory.SIZE; i++)
		{
			Item item = bag.get(i);
			s.bag.add(item == null ? "" : item.key());
		}
		return s;
	}

	/**
	 * Continues a saved run: progress and build come back, the board starts empty. The player
	 * returns to the saved spot when it's still open ground. Unknown names (from a newer or older
	 * version) are skipped.
	 */
	public void load(RunSave save)
	{
		restart();
		for (String name : save.gear.values())
		{
			Gear g = enumOrNull(Gear.class, name);
			if (g != null)
			{
				build.equip(g);
			}
		}
		for (Map.Entry<String, Integer> e : save.passives.entrySet())
		{
			Passive p = enumOrNull(Passive.class, e.getKey());
			int ranks = e.getValue() == null ? 0 : e.getValue();
			for (int i = 0; p != null && i < Math.min(ranks, p.getMaxRank()); i++)
			{
				build.addRank(p);
			}
		}
		for (int i = 0; save.bag != null && i < Math.min(Inventory.SIZE, save.bag.size()); i++)
		{
			bag.set(i, Item.fromKey(save.bag.get(i)));
		}
		if (save.potions != null)
		{
			for (Map.Entry<String, Double> e : save.potions.entrySet())
			{
				Boost b = enumOrNull(Boost.class, e.getKey());
				if (b != null && e.getValue() != null)
				{
					build.restorePotion(b, e.getValue());
				}
			}
		}
		if (save.boosts != null)
		{
			// potions used to last the whole run; their doses come back as potions in the bag
			for (Map.Entry<String, Integer> e : save.boosts.entrySet())
			{
				Boost b = enumOrNull(Boost.class, e.getKey());
				for (int left = e.getValue() == null ? 0 : e.getValue(); b != null && left > 0; left -= Boost.DOSES)
				{
					bag.add(Item.potion(b, Math.min(Boost.DOSES, left)));
				}
			}
		}
		progression.restore(save.level, save.xp, save.pendingLevelUps);
		elapsed = Math.max(0, save.elapsed);
		lastKillTime = elapsed;
		kills = Math.max(0, save.kills);
		championsSlain = Math.max(0, save.championsSlain);
		rerollsLeft = save.rerolls == null ? rerollsLeft : Math.max(0, Math.min(rerollsLeft, save.rerolls));
		revivesLeft = save.revives == null ? revivesLeft : Math.max(0, Math.min(revivesLeft, save.revives));
		bonusCoins = Math.max(0, save.bonusCoins);
		if (save.runPets != null)
		{
			for (String name : save.runPets)
			{
				Pet pet = enumOrNull(Pet.class, name);
				if (pet != null)
				{
					runPets.add(pet);
				}
			}
		}
		refreshPets();
		spawner.resume(elapsed);
		applyBuild();
		player.health = save.health > 0 ? Math.min(player.maxHealth, save.health) : player.maxHealth;
		double x = save.x;
		double y = save.y;
		if (save.version < 3 && x >= 0 && y >= 0)
		{
			// the arenas grew around the same centre since
			double shift = (Location.SIZE - Location.OLD_SIZE) / 2 * Arena.TILE;
			x += shift;
			y += shift;
		}
		if (x >= 0 && y >= 0 && x < arena.getWidth() && y < arena.getHeight()
			&& arena.isWalkable(Arena.tileOf(x), Arena.tileOf(y))
			&& !arena.isSolidBox(x, y, Player.COLLISION_RADIUS))
		{
			player.x = x;
			player.y = y;
		}
		if (progression.getPendingLevelUps() > 0)
		{
			offerChoices(false);
		}
	}

	private static <E extends Enum<E>> E enumOrNull(Class<E> type, String name)
	{
		try
		{
			return name == null ? null : Enum.valueOf(type, name);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	/** Recomputes everything derived from the build. */
	private void applyBuild()
	{
		build.setLevel(progression.getLevel());
		weapon.configure(build);
		player.maxHealth = build.maxHealth();
		player.health = Math.min(player.health, player.maxHealth);
		player.speed = build.speed();
		player.magnetRadius = build.magnetRadius();
	}

	/**
	 * The size of the world area the player can see, in world units. Enemies spawn just beyond it
	 * so they walk in from off-screen.
	 */
	public void setVisibleArea(double width, double height)
	{
		spawnDistance = Math.hypot(width, height) / 2 + Spawner.OFFSCREEN_MARGIN;
	}

	/** Desired movement; any non-zero vector moves at full speed in its direction. */
	public void setMoveDirection(double dx, double dy)
	{
		double len = Math.hypot(dx, dy);
		if (len == 0)
		{
			moveX = 0;
			moveY = 0;
		}
		else
		{
			moveX = dx / len;
			moveY = dy / len;
		}
	}

	/** Advances the simulation by a frame's worth of real time. Does nothing unless running. */
	public void advance(double frameSeconds)
	{
		if (phase == Phase.TITLE && frameSeconds > 0)
		{
			menuTime += Math.min(frameSeconds, MAX_FRAME);
		}
		if (levelUpTime >= 0 && frameSeconds > 0)
		{
			// the level-up show plays in real time over the paused game
			levelUpTime += Math.min(frameSeconds, MAX_FRAME);
		}
		if (phase == Phase.BOSS_DROP && bossDrop != null && frameSeconds > 0)
		{
			// the reels spin in real time while the game waits
			BossDrop drop = bossDrop;
			boolean wasSpinning = drop.isSpinning();
			boolean[] stopped = reelsStopped(drop);
			drop.advance(Math.min(frameSeconds, MAX_FRAME));
			cueReels(drop, stopped, wasSpinning);
			return;
		}
		if (phase != Phase.RUNNING || !(frameSeconds > 0))
		{
			return;
		}
		accumulator += Math.min(frameSeconds, MAX_FRAME);
		while (accumulator >= STEP && phase == Phase.RUNNING)
		{
			step();
			accumulator -= STEP;
		}
		if (phase != Phase.RUNNING)
		{
			// don't carry leftover time into the next running period
			accumulator = 0;
		}
	}

	public void togglePause()
	{
		if (isOptionsOpen())
		{
			// Esc or P leaves the options page first
			optionsOpen = false;
		}
		else if (phase == Phase.RUNNING)
		{
			phase = Phase.PAUSED;
		}
		else if (phase == Phase.PAUSED)
		{
			phase = Phase.RUNNING;
		}
	}

	/** Whether the developer buttons show (a config option). */
	public void setDevTools(boolean devTools)
	{
		this.devTools = devTools;
	}

	public boolean isDevTools()
	{
		return devTools;
	}

	/** Developer tool: ends the run now, as if the player had died. */
	public void devEndRun()
	{
		if (phase == Phase.RUNNING || phase == Phase.PAUSED || phase == Phase.LEVEL_UP)
		{
			player.health = 0;
			endRun();
		}
	}

	/** Developer tool: gains levels and offers an upgrade for each, starting now. */
	public void devGrantLevels(int levels)
	{
		if (phase == Phase.RUNNING || phase == Phase.PAUSED)
		{
			progression.grantLevels(levels);
			applyBuild();
			levelUp();
		}
	}

	/** Developer tool: the next champion arrives now. */
	public void devSummonChampion()
	{
		if ((phase == Phase.RUNNING || phase == Phase.PAUSED) && getChampion() == null)
		{
			spawner.summonNow(elapsed);
		}
	}

	/** Pauses if currently running; used when the client loses focus. */
	public void pause()
	{
		if (phase == Phase.RUNNING)
		{
			phase = Phase.PAUSED;
		}
	}

	/** Takes the offer at {@code index}. Returns false if there is no such choice right now. */
	public boolean chooseUpgrade(int index)
	{
		if (phase != Phase.LEVEL_UP || index < 0 || index >= choices.size())
		{
			return false;
		}
		Offer chosen = choices.get(index);
		cue(Cue.of(Cue.Type.CHOOSE).withGear(chosen.getGear()).withPassive(chosen.getPassive()));
		take(chosen);
		if (casketOpen)
		{
			casketOpen = false;
		}
		else
		{
			progression.consumeLevelUp();
		}
		choices.clear();
		nextReward();
		return true;
	}

	/**
	 * Re-rolls the offers on screen, if the run has a reroll left (from the Reroll upgrade).
	 * Returns false when it can't.
	 */
	public boolean reroll()
	{
		if (phase != Phase.LEVEL_UP || rerollsLeft <= 0)
		{
			return false;
		}
		rerollsLeft--;
		offerChoices(casketOpen);
		cue(Cue.of(Cue.Type.REROLL));
		return true;
	}

	public int getRerollsLeft()
	{
		return rerollsLeft;
	}

	public int getRevivesLeft()
	{
		return revivesLeft;
	}

	/** The champion drop being revealed, or null. */
	public BossDrop getBossDrop()
	{
		return bossDrop;
	}

	/**
	 * Takes the revealed drop and goes on to the casket's gear choices. While the reels still
	 * spin, the first call only stops them. Returns true when the drop was taken.
	 */
	public boolean claimDrop()
	{
		if (phase != Phase.BOSS_DROP || bossDrop == null)
		{
			return false;
		}
		if (!bossDrop.isClaimable())
		{
			boolean wasSpinning = bossDrop.isSpinning();
			boolean[] stopped = reelsStopped(bossDrop);
			bossDrop.skip();
			cueReels(bossDrop, stopped, wasSpinning);
			return false;
		}
		BossDrop.Prize prize = bossDrop.getPrize();
		cue(Cue.of(Cue.Type.DROP_CLAIMED).withValue(bossDrop.getRarity().ordinal()).withGear(prize.getGear()));
		bossDrop = null;
		switch (prize.getKind())
		{
			case COINS:
				bonusCoins += prize.getAmount();
				player.health = player.maxHealth;
				break;
			case LAMP:
				progression.grantLevels(prize.getAmount());
				break;
			case GEAR:
				wear(prize.getGear());
				break;
			default:
				runPets.add(prize.getPet());
				if (meta != null)
				{
					meta.addPet(prize.getPet());
				}
				refreshPets();
				break;
		}
		applyBuild();
		offerChoices(true);
		return true;
	}

	/** Opens a champion's casket: its unique drop goes on the reels. */
	private void openCasket()
	{
		List<Gear> items = new ArrayList<>();
		Gear weapon = build.get(Slot.WEAPON);
		for (Gear g : BossDrop.ITEMS)
		{
			boolean blocked = g.getSlot() == Slot.SHIELD && weapon != null && weapon.isTwoHanded();
			if (build.get(g.getSlot()) != g && !blocked)
			{
				items.add(g);
			}
		}
		List<Pet> pets = new ArrayList<>();
		for (Pet p : Pet.values())
		{
			if (!build.getPets().contains(p))
			{
				pets.add(p);
			}
		}
		List<Gear> megarares = new ArrayList<>();
		for (Gear g : BossDrop.MEGARARES)
		{
			if (g.getStyle() == build.getStyle() && weapon != g)
			{
				megarares.add(g);
			}
		}
		bossDrop = BossDrop.roll(random, megarares, items, pets, location.getDifficulty());
		phase = Phase.BOSS_DROP;
		cue(Cue.of(Cue.Type.REELS_START));
	}

	/** Coins paid when the run ended, or -1 while it goes on. */
	public int getCoinsEarned()
	{
		return coinsEarned;
	}

	/** Coins the run would pay if it ended now. */
	public int coinsSoFar()
	{
		double coins = kills * COINS_PER_KILL + elapsed * COINS_PER_SECOND + championsSlain * COINS_PER_CHAMPION;
		coins *= (1 + 0.25 * (location.getDifficulty() - 1)) * build.coinMultiplier();
		return (int) Math.round(coins) + bonusCoins;
	}

	/** Shows the next waiting reward (casket first), or resumes play. */
	private void nextReward()
	{
		if (pendingCaskets > 0)
		{
			pendingCaskets--;
			openCasket();
		}
		else if (progression.getPendingLevelUps() > 0)
		{
			levelUp();
		}
		else
		{
			phase = Phase.RUNNING;
		}
	}

	/** True while the offers come from a champion's casket. */
	public boolean isCasketOpen()
	{
		return casketOpen;
	}

	private void take(Offer offer)
	{
		switch (offer.getKind())
		{
			case GEAR:
				if (offer.isStored())
				{
					store(offer.getGear());
				}
				else
				{
					wear(offer.getGear());
				}
				break;
			case PASSIVE:
				build.addRank(offer.getPassive());
				break;
			case COINS:
				bonusCoins += offer.getCoins();
				break;
			case BOOST:
				bag.add(Item.potion(offer.getBoost(), Boost.DOSES));
				break;
			default:
				for (int i = 0; i < offer.getRank(); i++)
				{
					bag.add(Item.food(offer.getFood()));
				}
				break;
		}
		applyBuild();
		if (offer.getPassive() == Passive.HITPOINTS)
		{
			player.heal(20);
		}
	}

	// ---------------------------------------------------------------- the bag

	public Inventory getInventory()
	{
		return bag;
	}

	public boolean isBagShown()
	{
		return bagShown;
	}

	/** Shows or hides the bag. */
	public void setBagShown(boolean shown)
	{
		bagShown = shown;
	}

	/** Seconds until food (or, for karambwans, the next karambwan) can be eaten again. */
	public double getEatDelay(Food food)
	{
		return food.isCombo() ? comboTimer : eatTimer;
	}

	/** Seconds until the next potion dose can be drunk. */
	public double getDrinkDelay()
	{
		return drinkTimer;
	}

	/** A message about the bag to show now (e.g. a full bag), or null. */
	public String getNotice()
	{
		return notice != null && elapsed < noticeUntil ? notice : null;
	}

	private void notice(String text)
	{
		notice = text;
		noticeUntil = elapsed + NOTICE_SECONDS;
	}

	/**
	 * Uses what is in a bag slot, like clicking it in the game: gear is worn (what it replaces goes
	 * in its place), a potion is drunk, food is eaten. A weapon of another style also puts on that
	 * style's gear from the bag, so one click switches style. Works while playing or paused.
	 * Returns false when nothing happened.
	 */
	public boolean useItem(int slot)
	{
		Item item = bag.get(slot);
		if (item == null || phase != Phase.RUNNING && phase != Phase.PAUSED)
		{
			return false;
		}
		if (item.getGear() != null)
		{
			return equipFromBag(slot);
		}
		if (item.getPotion() != null)
		{
			if (drinkTimer > 0)
			{
				cue(Cue.of(Cue.Type.REFUSED));
				return false;
			}
			Boost potion = item.getPotion();
			build.drink(potion);
			bag.set(slot, item.sipped());
			drinkTimer = DRINK_DELAY;
			applyBuild();
			if (potion == Boost.SARADOMIN_BREW)
			{
				player.heal(player.maxHealth * Boost.HITPOINTS);
			}
			cue(Cue.at(Cue.Type.DRINK, player.x, player.y));
			return true;
		}
		Food food = item.getFood();
		if (food.isCombo() ? comboTimer > 0 : eatTimer > 0)
		{
			cue(Cue.of(Cue.Type.REFUSED));
			return false;
		}
		if (food.isCombo())
		{
			comboTimer = Food.EAT_DELAY;
		}
		else
		{
			eatTimer = Food.EAT_DELAY;
		}
		bag.set(slot, null);
		player.heal(player.maxHealth * food.getHeal());
		cue(Cue.at(Cue.Type.EAT, player.x, player.y));
		return true;
	}

	/** Throws away what is in a bag slot. */
	public boolean dropItem(int slot)
	{
		if (bag.get(slot) == null || phase == Phase.TITLE || phase == Phase.GAME_OVER)
		{
			return false;
		}
		bag.set(slot, null);
		cue(Cue.of(Cue.Type.ITEM_DROPPED));
		return true;
	}

	/** Moves an item to another bag slot, swapping with whatever is there. */
	public boolean moveItem(int from, int to)
	{
		if (from == to || bag.get(from) == null || to < 0 || to >= Inventory.SIZE || phase == Phase.TITLE || phase == Phase.GAME_OVER)
		{
			return false;
		}
		bag.swap(from, to);
		return true;
	}

	private boolean equipFromBag(int slot)
	{
		Gear gear = bag.get(slot).getGear();
		// what comes off goes where it was, and in free slots
		if (build.wouldTakeOff(gear) > 1 + bag.freeSlots())
		{
			notice("You don't have enough inventory space to do that.");
			cue(Cue.of(Cue.Type.REFUSED));
			return false;
		}
		bag.set(slot, null);
		cue(Cue.of(Cue.Type.EQUIP).withGear(gear));
		putOn(gear, slot);
		if (gear.getSlot() == Slot.WEAPON)
		{
			dress(gear);
		}
		applyBuild();
		return true;
	}

	/** Wears gear; what comes off goes in bag slot {@code slot} if it's free, else in any free slot. */
	private void putOn(Gear gear, int slot)
	{
		for (Gear off : build.equip(gear))
		{
			if (bag.get(slot) == null)
			{
				bag.set(slot, Item.gear(off));
			}
			else
			{
				bag.add(Item.gear(off));
			}
		}
	}

	/**
	 * After a weapon goes on: wears the bag's gear made for its style in place of pieces for other
	 * styles (or none), and in place of any-style pieces that are weaker. One click on a weapon
	 * in the bag is a whole switch, as players set up in the game.
	 */
	private void dress(Gear weapon)
	{
		Style style = weapon.getStyle();
		for (Slot slot : Slot.values())
		{
			if (slot == Slot.WEAPON || slot == Slot.SHIELD && weapon.isTwoHanded())
			{
				continue;
			}
			int i = bag.find(slot, style);
			Gear worn = build.get(slot);
			if (i < 0 || worn != null && (worn.getStyle() == style || worn.getStyle() == null && worn.getPower() > bag.get(i).getGear().getPower()))
			{
				continue;
			}
			Gear piece = bag.get(i).getGear();
			bag.set(i, null);
			putOn(piece, i);
		}
	}

	/**
	 * Wears a piece of gear from an offer or a drop. What it replaces goes in the bag when it is
	 * still worth keeping: a piece for another style (or any style), a boss item, or a shield
	 * taken off by a two-handed weapon. Weaker pieces of the same kind in the bag are thrown away.
	 */
	private void wear(Gear gear)
	{
		discardWeaker(gear);
		for (Gear old : build.equip(gear))
		{
			if (old.getStyle() != gear.getStyle() || old.isUnique() || old.getSlot() != gear.getSlot())
			{
				bag.add(Item.gear(old));
			}
		}
		if (gear.getSlot() == Slot.WEAPON)
		{
			dress(gear);
		}
	}

	/** Puts gear for another style in the bag, in place of a weaker piece of the same kind. */
	private void store(Gear gear)
	{
		discardWeaker(gear);
		bag.add(Item.gear(gear));
	}

	/** Throws away bag pieces for the same slot and style that are no better than {@code gear}. */
	private void discardWeaker(Gear gear)
	{
		for (int i = 0; i < Inventory.SIZE; i++)
		{
			Gear g = bag.get(i) == null ? null : bag.get(i).getGear();
			if (g != null && g != gear && g.getSlot() == gear.getSlot() && g.getStyle() == gear.getStyle() && !g.isUnique()
				&& g.getTier() <= gear.getTier())
			{
				bag.set(i, null);
			}
		}
	}

	/** Whether the bag has room for what an offer puts in it. */
	private boolean fits(Offer offer)
	{
		int needed = offer.bagSlots();
		if (needed == 0 || bag.freeSlots() >= needed)
		{
			return true;
		}
		Gear g = offer.getGear();
		return g != null && bag.find(g.getSlot(), g.getStyle()) >= 0;
	}

	/** Styles the player can fight in: the held weapon's, and those of weapons in the bag. */
	public Set<Style> carriedStyles()
	{
		Set<Style> styles = EnumSet.of(build.getStyle());
		for (int i = 0; i < Inventory.SIZE; i++)
		{
			Gear g = bag.get(i) == null ? null : bag.get(i).getGear();
			if (g != null && g.getSlot() == Slot.WEAPON)
			{
				styles.add(g.getStyle());
			}
		}
		return styles;
	}

	/** The strongest piece owned, worn or in the bag, for a slot and style; null for none. */
	private Gear owned(Slot slot, Style style)
	{
		Gear best = null;
		Gear worn = build.get(slot);
		if (worn != null && worn.getStyle() == style)
		{
			best = worn;
		}
		for (int i = 0; i < Inventory.SIZE; i++)
		{
			Gear g = bag.get(i) == null ? null : bag.get(i).getGear();
			if (g != null && g.getSlot() == slot && g.getStyle() == style && (best == null || g.getTier() > best.getTier()))
			{
				best = g;
			}
		}
		return best;
	}

	/** Heals from damage dealt, as far as the drain store allows. */
	private void drain(double wanted)
	{
		double healed = Math.min(Math.max(0, wanted), drainStore);
		if (healed > 0)
		{
			drainStore -= healed;
			player.heal(healed);
		}
	}

	/** Walking straight into the edge of a tree or wall end eases the player round it instead of stopping dead. */
	private void slipRoundCorners(double r)
	{
		int dirX = (int) Math.signum(moveX);
		int dirY = (int) Math.signum(moveY);
		if ((dirX == 0) == (dirY == 0))
		{
			return;
		}
		double slip = arena.cornerSlip(player.x, player.y, dirX, dirY, r);
		if (slip == 0)
		{
			return;
		}
		double d = Math.copySign(Math.min(Math.abs(slip), player.speed * STEP), slip);
		if (dirX != 0)
		{
			player.y = arena.moveY(player.x, player.y, d, r);
		}
		else
		{
			player.x = arena.moveX(player.x, player.y, d, r);
		}
	}

	private void step()
	{
		elapsed += STEP;
		player.attackAge += STEP;

		double r = Player.COLLISION_RADIUS;
		player.x = arena.moveX(player.x, player.y, moveX * player.speed * STEP, r);
		player.y = arena.moveY(player.x, player.y, moveY * player.speed * STEP, r);
		slipRoundCorners(r);
		player.moving = moveX != 0 || moveY != 0;
		if (player.moving && player.attackAge > Player.ATTACK_ANIMATION)
		{
			player.facing = Math.atan2(moveY, moveX);
		}
		player.heal(build.regenPerSecond() * STEP);
		redemptionTimer = Math.max(0, redemptionTimer - STEP);
		retributionTimer = Math.max(0, retributionTimer - STEP);
		eatTimer = Math.max(0, eatTimer - STEP);
		comboTimer = Math.max(0, comboTimer - STEP);
		drinkTimer = Math.max(0, drinkTimer - STEP);
		if (build.tickPotions(STEP))
		{
			applyBuild();
		}

		boolean hadChampion = getChampion() != null;
		if (spawning)
		{
			spawner.update(STEP, elapsed, player, arena, spawnDistance, enemies);
		}
		Enemy champion = getChampion();
		if (!hadChampion && champion != null)
		{
			cue(Cue.at(Cue.Type.CHAMPION_ARRIVES, champion.x, champion.y).withKind(champion.kind));
		}
		else if (champion == null && spawner.untilChampion(elapsed - STEP) > CHAMPION_WARNING
			&& spawner.untilChampion(elapsed) <= CHAMPION_WARNING)
		{
			cue(Cue.of(Cue.Type.CHAMPION_WARNING));
		}
		moveEnemies();
		moveShots();
		if (hurtPlayer())
		{
			return;
		}

		int splatsBefore = hitsplats.size();
		int burstsBefore = bursts.size();
		List<Enemy> hit = weapon.update(STEP, player, enemies, strikes, hitsplats, bursts, random);
		cueHits(splatsBefore, weapon.getGear(), null);
		double dealt = weapon.lastDamage;
		drainStore = Math.min(player.maxHealth * DRAIN_STORE, drainStore + player.maxHealth * DRAIN_RATE * STEP);
		if (!hit.isEmpty())
		{
			Enemy first = hit.get(0);
			player.attackAge = 0;
			player.facing = Math.atan2(first.y - player.y, first.x - player.x);
			cue(Cue.at(Cue.Type.ATTACK, player.x, player.y).withGear(weapon.getGear()));
		}
		int abilitySplats = hitsplats.size();
		int abilityStrikes = strikes.size();
		int abilityBursts = bursts.size();
		abilities.update(STEP, build, player, weapon.getAttackHit(), enemies, strikes, hitsplats, bursts, random);
		cueAbilities(abilityStrikes, abilityBursts);
		cueHits(abilitySplats, null, Passive.CANNON);
		// lifesteal comes from weapon hits only; Blood Barrage heals by itself
		drain(dealt * build.lifesteal() + abilities.drained);
		removeDeadEnemies();
		collectOrbs();
		collectCaskets();
		strikes.removeIf(s -> s.tick(STEP));
		hitsplats.removeIf(h -> (h.remaining -= STEP) <= 0);
		corpses.removeIf(c -> (c.remaining -= STEP) <= 0);
		xpDrops.removeIf(d -> (d.remaining -= STEP) <= 0);
		bursts.removeIf(b -> (b.remaining -= STEP) <= 0);
	}

	private void moveEnemies()
	{
		if (paths != null)
		{
			paths.update(Arena.tileOf(player.x), Arena.tileOf(player.y));
		}
		double damageScale = Spawner.damageScale(elapsed);
		for (int i = 0; i < enemies.size(); i++)
		{
			Enemy e = enemies.get(i);
			double tx = player.x;
			double ty = player.y;
			double contact = e.radius + Player.RADIUS;
			if (paths != null)
			{
				// line-of-sight checks are staggered: each enemy looks every few steps
				if (--e.sightCheck <= 0)
				{
					e.inSight = arena.clearLine(e.x, e.y, player.x, player.y);
					e.clearWalk = e.inSight && arena.clearPath(e.x, e.y, player.x, player.y, contact, e.collisionRadius());
					if (e.wanted > STUCK_MIN_WALK && Math.hypot(e.x - e.checkX, e.y - e.checkY) < e.wanted * STUCK_SHARE)
					{
						// barely moved while trying to walk: caught on something, so follow the paths for a while
						e.detour = DETOUR_SECONDS;
					}
					e.checkX = e.x;
					e.checkY = e.y;
					e.wanted = 0;
					e.sightCheck = 8 + i % 5;
				}
				e.detour = Math.max(0, e.detour - STEP);
				if (!e.clearWalk || e.detour > 0)
				{
					double[] next = paths.nextStep(Arena.tileOf(e.x), Arena.tileOf(e.y));
					if (next != null)
					{
						tx = next[0];
						ty = next[1];
						contact = 0;
					}
				}
			}
			if (e.kind.isRanged() && e.inSight)
			{
				double dist = Math.hypot(player.x - e.x, player.y - e.y);
				double range = e.kind.attackRange;
				if (dist <= range * Enemy.STAND_OFF)
				{
					// close enough to shoot
					contact = Enemy.STAND_OFF_MARK;
				}
				e.attackTimer -= STEP;
				if (dist <= range && e.attackTimer <= 0 && !e.isFrozen())
				{
					e.attackTimer = e.kind.attackCooldown;
					shots.add(new EnemyShot(e.kind.attack, e.x, e.y - 8, player.x, player.y, e.missileDamage * damageScale, range));
					cue(Cue.at(Cue.Type.ENEMY_SHOT, e.x, e.y).withKind(e.kind).withShot(e.kind.attack));
				}
			}
			// shooters keep creeping in while they shoot, so they can't hide behind the crowd forever
			e.pursue(tx, ty, contact == Enemy.STAND_OFF_MARK ? e.radius + Player.RADIUS : contact,
				contact == Enemy.STAND_OFF_MARK ? STEP * Enemy.CREEP : STEP, arena);
		}
		separateEnemies();
	}

	/** A monster that covered less than this share of the way it tried to walk is stuck. */
	static final double STUCK_SHARE = 0.25;
	/** Walking less than this (world units) between checks never counts as stuck. */
	static final double STUCK_MIN_WALK = 4;
	/** Seconds a stuck monster follows the paths before trying the straight line again. */
	static final double DETOUR_SECONDS = 1.5;

	/** Flies monster missiles; they hurt the player on contact and stop at walls. */
	private void moveShots()
	{
		double reach = Player.RADIUS + 4;
		for (Iterator<EnemyShot> it = shots.iterator(); it.hasNext(); )
		{
			EnemyShot s = it.next();
			s.x += s.vx * STEP;
			s.y += s.vy * STEP;
			s.remaining -= STEP;
			double dx = player.x - s.x;
			double dy = player.y - (s.y + 8);
			if (dx * dx + dy * dy <= reach * reach)
			{
				double taken = s.damage * (1 - build.damageReduction()) * (1 - build.missileReduction()) * stallMultiplier();
				missileDamageTaken += taken;
				player.damage(taken);
				damageTaken += taken;
				it.remove();
			}
			else if (s.remaining <= 0 || !arena.isOpen() && arena.isSolidAt(s.x, s.y + 8))
			{
				it.remove();
			}
		}
	}

	/** Size of the grid cells used to find neighbouring enemies; at least the largest enemy diameter. */
	static final double CROWD_CELL = 48;

	/**
	 * Pushes overlapping enemies apart so a crowd surrounds the player instead of piling into one
	 * spot. This also limits how many can touch the player at once.
	 */
	private void separateEnemies()
	{
		int n = enemies.size();
		if (n < 2)
		{
			return;
		}
		int cols = (int) Math.ceil(arena.getWidth() / CROWD_CELL) + 1;
		int rows = (int) Math.ceil(arena.getHeight() / CROWD_CELL) + 1;
		if (cellHead.length != cols * rows)
		{
			cellHead = new int[cols * rows];
		}
		if (cellNext.length < n)
		{
			cellNext = new int[Math.max(n, cellNext.length * 2)];
		}
		java.util.Arrays.fill(cellHead, -1);
		for (int i = 0; i < n; i++)
		{
			Enemy e = enemies.get(i);
			int c = cellOf(e, cols, rows);
			cellNext[i] = cellHead[c];
			cellHead[c] = i;
		}
		for (int i = 0; i < n; i++)
		{
			Enemy a = enemies.get(i);
			int cx = Math.min(cols - 1, Math.max(0, (int) (a.x / CROWD_CELL)));
			int cy = Math.min(rows - 1, Math.max(0, (int) (a.y / CROWD_CELL)));
			for (int gy = Math.max(0, cy - 1); gy <= Math.min(rows - 1, cy + 1); gy++)
			{
				for (int gx = Math.max(0, cx - 1); gx <= Math.min(cols - 1, cx + 1); gx++)
				{
					for (int j = cellHead[gy * cols + gx]; j != -1; j = cellNext[j])
					{
						if (j > i)
						{
							push(a, enemies.get(j));
						}
					}
				}
			}
		}
	}

	private static int cellOf(Enemy e, int cols, int rows)
	{
		int cx = Math.min(cols - 1, Math.max(0, (int) (e.x / CROWD_CELL)));
		int cy = Math.min(rows - 1, Math.max(0, (int) (e.y / CROWD_CELL)));
		return cy * cols + cx;
	}

	private void push(Enemy a, Enemy b)
	{
		double dx = b.x - a.x;
		double dy = b.y - a.y;
		double min = (a.radius + b.radius) * 0.85;
		double d2 = dx * dx + dy * dy;
		if (d2 >= min * min)
		{
			return;
		}
		double d = Math.sqrt(d2);
		if (d < 1e-6)
		{
			// exactly on top of each other: split along an arbitrary axis
			dx = 1;
			dy = 0;
			d = 1;
		}
		double overlap = (min - Math.min(d, min)) / 2;
		double px = dx / d * overlap;
		double py = dy / d * overlap;
		// frozen monsters and champions hold their ground; the other one gives way
		double wa = a.frozen > 0 || a.champion ? 0 : 1;
		double wb = b.frozen > 0 || b.champion ? 0 : 1;
		if (wa + wb == 0)
		{
			return;
		}
		double sa = 2 * wa / (wa + wb);
		double sb = 2 * wb / (wa + wb);
		if (arena.isOpen())
		{
			a.x -= px * sa;
			a.y -= py * sa;
			b.x += px * sb;
			b.y += py * sb;
		}
		else
		{
			a.x = arena.moveX(a.x, a.y, -px * sa, a.collisionRadius());
			a.y = arena.moveY(a.x, a.y, -py * sa, a.collisionRadius());
			b.x = arena.moveX(b.x, b.y, px * sb, b.collisionRadius());
			b.y = arena.moveY(b.x, b.y, py * sb, b.collisionRadius());
		}
	}

	/** In overtime, seconds without a kill before monsters start hitting harder for it. */
	static final double STALL_GRACE = 20;
	/** Seconds over which stalling multiplies monster damage by e. */
	static final double STALL_GROWTH = 20;

	/**
	 * Monster damage multiplier for a stalled run: in overtime, once the player has gone
	 * {@link #STALL_GRACE} seconds without a kill, damage keeps growing until something gives. A
	 * build that can't kill anything any more and can't be killed either would otherwise stand
	 * off forever.
	 */
	double stallMultiplier()
	{
		if (elapsed <= Spawner.OVERTIME_START)
		{
			return 1;
		}
		double stalled = elapsed - Math.max(lastKillTime, Spawner.OVERTIME_START) - STALL_GRACE;
		return stalled <= 0 ? 1 : Math.exp(stalled / STALL_GROWTH);
	}

	/** Applies contact damage (and Vengeance) for this step. Returns true if the player died. */
	private boolean hurtPlayer()
	{
		double raw = 0;
		double thorns = build.thornsDps() * STEP;
		for (Enemy e : enemies)
		{
			if (e.touches(player))
			{
				raw += e.contactDps * STEP;
				if (thorns > 0)
				{
					e.damage(thorns);
				}
			}
		}
		double taken = raw * (1 - build.damageReduction()) * (1 - build.contactReduction()) * stallMultiplier();
		if (taken > 0)
		{
			player.damage(taken);
			damageTaken += taken;
			contactDamageTaken += taken;
		}
		splatTimer -= STEP;
		if (splatTimer <= 0)
		{
			if (damageTaken >= 0.5)
			{
				hitsplats.add(new Effects.Hitsplat(null, (int) Math.round(damageTaken), false));
				cue(Cue.at(Cue.Type.PLAYER_HURT, player.x, player.y).withValue((int) Math.round(damageTaken)));
			}
			damageTaken = 0;
			splatTimer = PLAYER_SPLAT_INTERVAL;
		}
		if (build.rank(Passive.REDEMPTION) > 0 && redemptionTimer <= 0 && !player.isDead()
			&& player.health < player.maxHealth * REDEMPTION_THRESHOLD)
		{
			player.health = Math.max(player.health, player.maxHealth / 2);
			redemptionTimer = REDEMPTION_COOLDOWN;
			cue(Cue.at(Cue.Type.PRAYER_PROC, player.x, player.y).withPassive(Passive.REDEMPTION));
		}
		if (build.rank(Passive.RETRIBUTION) > 0 && retributionTimer <= 0 && !player.isDead()
			&& player.health < player.maxHealth * RETRIBUTION_THRESHOLD)
		{
			double blast = weapon.getAttackHit() * 3;
			for (Enemy e : enemies)
			{
				if (!e.isDead() && Math.hypot(e.x - player.x, e.y - player.y) <= RETRIBUTION_RADIUS + e.radius)
				{
					e.damage(blast);
					hitsplats.add(new Effects.Hitsplat(e, (int) Math.round(blast), true));
				}
			}
			bursts.add(new Effects.Burst(Effects.Burst.Type.EXPLOSION, player.x, player.y, RETRIBUTION_RADIUS));
			retributionTimer = RETRIBUTION_COOLDOWN;
			cue(Cue.at(Cue.Type.PRAYER_PROC, player.x, player.y).withPassive(Passive.RETRIBUTION));
		}
		if (player.isDead() && revivesLeft > 0)
		{
			revivesLeft--;
			player.health = player.maxHealth / 2;
			for (Enemy e : enemies)
			{
				if (Math.hypot(e.x - player.x, e.y - player.y) <= REVIVE_RADIUS)
				{
					e.frozen = Math.max(e.frozen, e.champion ? 1 : 3);
				}
			}
			bursts.add(new Effects.Burst(Effects.Burst.Type.GOD, player.x, player.y, REVIVE_RADIUS));
			cue(Cue.at(Cue.Type.REVIVE, player.x, player.y));
		}
		if (player.isDead())
		{
			endRun();
			return true;
		}
		return false;
	}

	/** The run is over: records the time and pays out its coins. */
	private void endRun()
	{
		phase = Phase.GAME_OVER;
		cue(Cue.at(Cue.Type.PLAYER_DEATH, player.x, player.y));
		Double best = bestTimes.get(location);
		if (best == null || elapsed > best)
		{
			bestTimes.put(location, elapsed);
		}
		coinsEarned = coinsSoFar();
		if (meta != null)
		{
			meta.addCoins(coinsEarned);
		}
	}

	private void removeDeadEnemies()
	{
		double heal = build.healPerKill();
		for (Iterator<Enemy> it = enemies.iterator(); it.hasNext(); )
		{
			Enemy e = it.next();
			if (e.isDead())
			{
				it.remove();
				kills++;
				lastKillTime = elapsed;
				orbs.add(new XpOrb(e.x, e.y, e.xpValue));
				if (random.nextDouble() < build.extraGemChance())
				{
					orbs.add(new XpOrb(e.x + 6, e.y - 6, e.xpValue));
				}
				corpses.add(new Effects.Corpse(e));
				cue(Cue.at(Cue.Type.KILL, e.x, e.y).withKind(e.kind).withValue(e.champion ? 1 : 0).after(e.hitDelay));
				if (e.champion)
				{
					championsSlain++;
					caskets.add(new Casket(e.x, e.y));
				}
				if (heal > 0)
				{
					player.heal(heal);
				}
			}
		}
	}

	private void collectOrbs()
	{
		double pickup = Player.RADIUS + XpOrb.RADIUS;
		double pull = XpOrb.PULL_SPEED * STEP;
		int gained = 0;
		for (Iterator<XpOrb> it = orbs.iterator(); it.hasNext(); )
		{
			XpOrb orb = it.next();
			double dx = player.x - orb.x;
			double dy = player.y - orb.y;
			double dist = Math.hypot(dx, dy);
			if (dist <= player.magnetRadius && dist > pickup)
			{
				double travel = Math.min(pull, dist);
				orb.x += dx / dist * travel;
				orb.y += dy / dist * travel;
				dist -= travel;
			}
			if (dist <= pickup)
			{
				it.remove();
				gained += orb.value;
			}
		}
		if (gained > 0)
		{
			int xp = Math.max(1, (int) Math.round(gained * build.xpMultiplier()));
			xpDrops.add(new Effects.XpDrop(xp));
			cue(Cue.at(Cue.Type.XP, player.x, player.y).withValue(xp));
			if (progression.addXp(xp) > 0)
			{
				// every level makes attacks a little faster straight away
				applyBuild();
				levelUp();
			}
		}
	}

	private void collectCaskets()
	{
		double pickup = Player.RADIUS + Casket.RADIUS;
		boolean opened = false;
		for (Iterator<Casket> it = caskets.iterator(); it.hasNext(); )
		{
			Casket c = it.next();
			double dx = player.x - c.x;
			double dy = player.y - c.y;
			double dist = Math.hypot(dx, dy);
			if (dist <= player.magnetRadius && dist > pickup)
			{
				double travel = Math.min(XpOrb.PULL_SPEED * STEP, dist);
				c.x += dx / dist * travel;
				c.y += dy / dist * travel;
				dist -= travel;
			}
			if (dist <= pickup)
			{
				it.remove();
				pendingCaskets++;
				opened = true;
				cue(Cue.at(Cue.Type.CASKET_PICKUP, c.x, c.y));
			}
		}
		if (opened && phase == Phase.RUNNING)
		{
			pendingCaskets--;
			openCasket();
		}
	}

	/**
	 * Picks three offers. A level-up gives, when possible, one piece of gear, one passive and one
	 * more of either; bare-handed players are always offered a weapon, and food may be offered
	 * when hurt. A casket offers gear only. A ready weapon combination always leads. Nothing is
	 * offered that the bag has no room for.
	 */
	private void offerChoices(boolean casket)
	{
		List<Offer> gear = gearOffers();
		gear.removeIf(o -> !fits(o));
		List<Offer> passives = new ArrayList<>();
		Set<Style> styles = carriedStyles();
		int abilities = 0;
		for (Passive p : Passive.values())
		{
			abilities += p.isAbility() && build.rank(p) > 0 ? 1 : 0;
		}
		for (Passive p : Passive.values())
		{
			int rank = build.rank(p);
			boolean slotFree = !p.isAbility() || rank > 0 || abilities < MAX_ABILITIES;
			if (slotFree && rank < p.getMaxRank() && (p.getStyle() == null || styles.contains(p.getStyle())))
			{
				passives.add(Offer.passive(p, rank + 1));
			}
		}
		Collections.shuffle(gear, random);
		Collections.shuffle(passives, random);
		// a weapon first when bare-handed, so fists are only the opening act; after that the next
		// weapon tier (the biggest upgrade) leads some of the time
		if (build.get(Slot.WEAPON) == null || random.nextDouble() < (casket ? 0.6 : WEAPON_OFFER_CHANCE))
		{
			gear.sort((a, b) -> Integer.compare(weaponRank(a), weaponRank(b)));
		}

		choices.clear();
		if (casket)
		{
			while (choices.size() < CHOICE_COUNT && !gear.isEmpty())
			{
				choices.add(gear.remove(0));
			}
			while (choices.size() < CHOICE_COUNT && !passives.isEmpty())
			{
				choices.add(passives.remove(0));
			}
			casketOpen = true;
		}
		else
		{
			if (!gear.isEmpty())
			{
				choices.add(gear.remove(0));
			}
			if (!passives.isEmpty())
			{
				choices.add(passives.remove(0));
			}
			List<Offer> rest = new ArrayList<>(gear);
			rest.addAll(passives);
			Collections.shuffle(rest, random);
			while (choices.size() < CHOICE_COUNT && !rest.isEmpty())
			{
				choices.add(rest.remove(0));
			}
			Offer food = Offer.food(Food.SHARK, FOOD_COUNT);
			if (player.health < player.maxHealth * FOOD_THRESHOLD && random.nextDouble() < 0.6 && fits(food))
			{
				if (choices.size() == CHOICE_COUNT)
				{
					choices.remove(CHOICE_COUNT - 1);
				}
				choices.add(food);
			}
		}
		// one piece for the bag a screen, so a style that's done still leaves room for potions and food
		boolean stored = false;
		for (Iterator<Offer> it = choices.iterator(); it.hasNext(); )
		{
			Offer o = it.next();
			if (o.isStored() && stored && !casket)
			{
				it.remove();
			}
			stored |= o.isStored();
		}
		Combination combination = Combination.ready(build);
		if (combination != null)
		{
			choices.remove(Offer.gear(combination.getResult()));
			choices.add(0, Offer.gear(combination.getResult()));
			while (choices.size() > CHOICE_COUNT)
			{
				choices.remove(choices.size() - 1);
			}
		}
		// when upgrades run out, fill up from potions, food and a coin pouch, so the last levels
		// of a long run still give a real (and varied) choice
		if (choices.size() < CHOICE_COUNT)
		{
			List<Offer> fillers = new ArrayList<>();
			for (Boost b : Boost.values())
			{
				if (b.getStyle() == null || styles.contains(b.getStyle()))
				{
					fillers.add(Offer.boost(b));
				}
			}
			fillers.add(Offer.food(randomFood(), FOOD_COUNT));
			fillers.removeIf(o -> !fits(o) || choices.contains(o));
			Collections.shuffle(fillers, random);
			// the pouch always has a place, so a full bag still leaves something to take
			fillers.add(random.nextInt(Math.min(CHOICE_COUNT - choices.size(), fillers.size() + 1)), Offer.coins(fillerCoins()));
			while (choices.size() < CHOICE_COUNT && !fillers.isEmpty())
			{
				choices.add(fillers.remove(0));
			}
		}
		phase = Phase.LEVEL_UP;
	}

	/** Sort key putting the held style's weapons first, then other weapons, then the rest. */
	private static int weaponRank(Offer o)
	{
		return o.getGear().getSlot() != Slot.WEAPON ? 2 : o.isStored() ? 1 : 0;
	}

	/** Food for a filler offer: sharks most often, anglerfish least. */
	private Food randomFood()
	{
		double roll = random.nextDouble();
		return roll < 0.4 ? Food.SHARK : roll < 0.65 ? Food.MANTA_RAY : roll < 0.85 ? Food.COOKED_KARAMBWAN : Food.ANGLERFISH;
	}

	/** Offers the next level's upgrades and starts the level-up show. */
	private void levelUp()
	{
		levelUpTime = 0;
		offerChoices(false);
		cue(Cue.at(Cue.Type.LEVEL_UP, player.x, player.y).withValue(progression.getLevel()));
	}

	/**
	 * Real seconds since the latest level-up screen opened (for its animation), or -1 if there
	 * hasn't been one this run.
	 */
	public double getLevelUpTime()
	{
		return levelUpTime;
	}

	/** Coins in a filler pouch: more the longer the run has gone. */
	int fillerCoins()
	{
		return 10 + 3 * (int) (elapsed / 60);
	}

	/**
	 * Every item that would be an upgrade right now, at most one per slot (two for weapons) for
	 * the held weapon's style. Now and then a weapon for another style is offered for the bag, and
	 * once one is there, upgrades for that style come too (also for the bag), so a run can switch
	 * between styles and still has more to find late on.
	 */
	private List<Offer> gearOffers()
	{
		List<Offer> offers = new ArrayList<>();
		Style style = build.getStyle();
		Gear weapon = build.get(Slot.WEAPON);
		if (weapon == null)
		{
			for (Style s : Style.values())
			{
				offers.add(Offer.gear(Gear.first(Slot.WEAPON, s)));
			}
		}
		else
		{
			addPick(offers, Gear.at(Slot.WEAPON, style, weapon.getTier() + 1), false);
			// a combined or boss weapon outclasses everything else at its tier, its base included
			if (!weapon.isUnique() && random.nextDouble() < SIDEGRADE_CHANCE)
			{
				List<Gear> same = Gear.at(Slot.WEAPON, style, weapon.getTier());
				same.remove(weapon);
				addPick(offers, same, false);
			}
			if (random.nextDouble() < STYLE_SWITCH_CHANCE)
			{
				// a weapon for another style, a little weaker, or the next one up from the bag's
				Style other = Style.values()[(style.ordinal() + 1 + random.nextInt(2)) % Style.values().length];
				Gear have = owned(Slot.WEAPON, other);
				addPick(offers, Gear.at(Slot.WEAPON, other, have == null ? Math.max(1, weapon.getTier() - 1) : have.getTier() + 1), true);
			}
		}
		addArmour(offers, style, false, 1);
		List<Style> others = new ArrayList<>(carriedStyles());
		others.remove(style);
		if (!others.isEmpty())
		{
			Style other = others.get(random.nextInt(others.size()));
			Gear have = owned(Slot.WEAPON, other);
			if (have != null && random.nextDouble() < OFF_STYLE_CHANCE)
			{
				addPick(offers, Gear.at(Slot.WEAPON, other, have.getTier() + 1), true);
			}
			addArmour(offers, other, true, OFF_STYLE_CHANCE);
		}
		return offers;
	}

	/**
	 * Armour, shields and accessories for one style, each slot offered with {@code chance}: worn
	 * at once for the held weapon's style, or {@code stored} in the bag for another.
	 */
	private void addArmour(List<Offer> offers, Style style, boolean stored, double chance)
	{
		Gear weapon = stored ? owned(Slot.WEAPON, style) : build.get(Slot.WEAPON);
		for (Slot slot : new Slot[]{Slot.HEAD, Slot.BODY, Slot.LEGS, Slot.SHIELD})
		{
			if (slot == Slot.SHIELD && weapon != null && weapon.isTwoHanded() || chance < 1 && random.nextDouble() >= chance)
			{
				continue;
			}
			int top = Gear.topTier(slot, style);
			Gear mine = owned(slot, style);
			Gear current = build.get(slot);
			int tier;
			if (mine != null)
			{
				tier = mine.getTier() + 1;
			}
			else if (current == null)
			{
				tier = 1;
			}
			else
			{
				// swap into this style's armour of about the same strength (or out of Void robes)
				tier = (int) Math.max(1, Math.round(current.getPower() * top) + (current.getStyle() == null ? 1 : 0));
			}
			List<Gear> candidates = Gear.at(slot, style, Math.min(tier, top + 1));
			candidates.removeIf(g -> g.getStyle() == null && (stored || g.getTier() == 0));
			if (!stored && (slot == Slot.BODY || slot == Slot.LEGS) && current != null && current.getStyle() != null
				&& current.getPower() >= 0.3 && current.getPower() < 0.6 && random.nextDouble() < VOID_CHANCE)
			{
				candidates.clear();
				candidates.add(slot == Slot.BODY ? Gear.VOID_KNIGHT_TOP : Gear.VOID_KNIGHT_ROBE);
			}
			addPick(offers, candidates, stored);
		}
		for (Slot slot : new Slot[]{Slot.CAPE, Slot.NECK, Slot.HANDS, Slot.FEET, Slot.RING, Slot.AMMO})
		{
			if (chance < 1 && random.nextDouble() >= chance)
			{
				continue;
			}
			Gear mine = owned(slot, style);
			Gear current = build.get(slot);
			int tier;
			if (mine != null)
			{
				tier = mine.getTier() + 1;
			}
			else if (current == null)
			{
				tier = 1;
			}
			else if (stored || current.getStyle() == style)
			{
				// for the bag: this style's piece above an any-style one, or level with another style's
				tier = current.getTier() + (current.getStyle() == null || current.getStyle() == style ? 1 : 0);
			}
			else
			{
				// another style's piece: this style's of the same tier; an any-style piece: the next one up
				tier = current.getStyle() != null ? current.getTier() : current.getTier() + 1;
			}
			List<Gear> candidates = Gear.at(slot, style, tier);
			candidates.remove(current);
			if (stored)
			{
				// the bag only needs what is made for the style; any-style pieces are worn already
				candidates.removeIf(g -> g.getStyle() != style);
			}
			addPick(offers, candidates, stored);
		}
	}

	/** Adds one of the candidates, favouring pieces of sets the player has started. */
	private void addPick(List<Offer> offers, List<Gear> candidates, boolean stored)
	{
		if (candidates.isEmpty())
		{
			return;
		}
		double total = 0;
		double[] weights = new double[candidates.size()];
		for (int i = 0; i < weights.length; i++)
		{
			GearSet set = candidates.get(i).getSet();
			weights[i] = set != null && wearsPieceOf(set) ? 4 : 1;
			total += weights[i];
		}
		double roll = random.nextDouble() * total;
		Gear pick = candidates.get(weights.length - 1);
		for (int i = 0; i < weights.length; i++)
		{
			roll -= weights[i];
			if (roll < 0)
			{
				pick = candidates.get(i);
				break;
			}
		}
		offers.add(stored ? Offer.stored(pick) : Offer.gear(pick));
	}

	private boolean wearsPieceOf(GearSet set)
	{
		for (Gear g : build.getWorn().values())
		{
			if (g.getSet() == set)
			{
				return true;
			}
		}
		return false;
	}

	public Phase getPhase()
	{
		return phase;
	}

	public Arena getArena()
	{
		return arena;
	}

	public Location getLocation()
	{
		return location;
	}

	public Player getPlayer()
	{
		return player;
	}

	public Weapon getWeapon()
	{
		return weapon;
	}

	public Build getBuild()
	{
		return build;
	}

	public Abilities getAbilities()
	{
		return abilities;
	}

	public Progression getProgression()
	{
		return progression;
	}

	public List<Enemy> getEnemies()
	{
		return Collections.unmodifiableList(enemies);
	}

	/** The champion currently alive, or null. */
	public Enemy getChampion()
	{
		for (Enemy e : enemies)
		{
			if (e.champion)
			{
				return e;
			}
		}
		return null;
	}

	/** Seconds until the next champion arrives. */
	public double getChampionCountdown()
	{
		return spawner.untilChampion(elapsed);
	}

	public int getChampionsSlain()
	{
		return championsSlain;
	}

	public List<XpOrb> getOrbs()
	{
		return Collections.unmodifiableList(orbs);
	}

	public List<Casket> getCaskets()
	{
		return Collections.unmodifiableList(caskets);
	}

	public List<EnemyShot> getShots()
	{
		return Collections.unmodifiableList(shots);
	}

	// ---------------------------------------------------------------- cues

	/** Most cues kept between takes, so a page that stops looking can't make the list grow for ever. */
	static final int MAX_CUES = 512;

	private void cue(Cue c)
	{
		if (cues.size() >= MAX_CUES)
		{
			cues.remove(0);
		}
		cues.add(c);
	}

	/**
	 * What happened since the last call, oldest first: hits, kills, pickups, level-ups and so on,
	 * for sounds and screen effects. Empties the list.
	 */
	public List<Cue> takeCues()
	{
		List<Cue> taken = new ArrayList<>(cues);
		cues.clear();
		return taken;
	}

	/** An impact cue for each hitsplat added since {@code from}, timed to when it lands. */
	private void cueHits(int from, Gear gear, Passive ability)
	{
		for (int i = from; i < hitsplats.size(); i++)
		{
			Effects.Hitsplat h = hitsplats.get(i);
			Enemy e = h.target;
			if (e == null)
			{
				continue;
			}
			Cue c = Cue.at(Cue.Type.IMPACT, e.x, e.y).withKind(e.kind).withValue(h.damage).after(h.delay);
			cue(ability != null ? c.withPassive(ability) : c.withGear(gear));
		}
	}

	/** Ability cues for the abilities' new missiles and area spells. */
	private void cueAbilities(int strikesFrom, int burstsFrom)
	{
		for (int i = strikesFrom; i < strikes.size(); i++)
		{
			Strike s = strikes.get(i);
			Passive p = s.source == Strike.Source.CANNON ? Passive.CANNON : s.source == Strike.Source.THRALL ? Passive.THRALLS
				: s.source == Strike.Source.CHINCHOMPA ? Passive.CHINCHOMPAS : null;
			if (p != null)
			{
				cue(Cue.at(Cue.Type.ABILITY, s.fromX, s.fromY).withPassive(p));
			}
			if (p == Passive.CHINCHOMPAS)
			{
				// the blast, as the chinchompa lands
				cue(Cue.at(Cue.Type.ABILITY, s.toX, s.toY).withPassive(p).withValue(1).after(s.flight));
			}
		}
		for (int i = burstsFrom; i < bursts.size(); i++)
		{
			Effects.Burst b = bursts.get(i);
			Passive p;
			switch (b.type)
			{
				case ICE:
					p = Passive.ICE_BARRAGE;
					break;
				case BLOOD:
					p = Passive.BLOOD_BARRAGE;
					break;
				case GOD:
					p = Passive.GOD_SPELLS;
					break;
				case DRAGONFIRE:
					p = Passive.DRAGONFIRE;
					break;
				default:
					// chinchompa blasts are heard as their missiles' impacts
					p = null;
					break;
			}
			if (p != null)
			{
				// value 1: an area going off, where it lands
				cue(Cue.at(Cue.Type.ABILITY, b.x, b.y).withPassive(p).withValue(1));
			}
		}
	}

	private static boolean[] reelsStopped(BossDrop drop)
	{
		boolean[] stopped = new boolean[drop.reelCount()];
		for (int i = 0; i < stopped.length; i++)
		{
			stopped[i] = drop.isStopped(i);
		}
		return stopped;
	}

	/** Cues for reels that stopped since {@code before}, and for the prize once all have. */
	private void cueReels(BossDrop drop, boolean[] before, boolean wasSpinning)
	{
		for (int i = 0; i < before.length; i++)
		{
			if (!before[i] && drop.isStopped(i))
			{
				cue(Cue.of(Cue.Type.REEL_STOP).withValue(i));
			}
		}
		if (wasSpinning && !drop.isSpinning())
		{
			cue(Cue.of(Cue.Type.DROP_REVEALED).withValue(drop.getRarity().ordinal()));
		}
	}

	public List<Strike> getStrikes()
	{
		return Collections.unmodifiableList(strikes);
	}

	public List<Effects.Hitsplat> getHitsplats()
	{
		return Collections.unmodifiableList(hitsplats);
	}

	public List<Effects.Corpse> getCorpses()
	{
		return Collections.unmodifiableList(corpses);
	}

	public List<Effects.XpDrop> getXpDrops()
	{
		return Collections.unmodifiableList(xpDrops);
	}

	public List<Effects.Burst> getBursts()
	{
		return Collections.unmodifiableList(bursts);
	}

	/** The offers on the level-up screen; empty unless in {@link Phase#LEVEL_UP}. */
	public List<Offer> getChoices()
	{
		return Collections.unmodifiableList(choices);
	}

	/** Seconds of running (unpaused) play. */
	public double getElapsed()
	{
		return elapsed;
	}

	public int getKills()
	{
		return kills;
	}

	// test hooks: place entities directly

	Enemy spawnEnemyAt(double x, double y, double health)
	{
		Enemy e = new Enemy(x, y, health, 70, 10, 1);
		enemies.add(e);
		return e;
	}

	/** Adds a monster of a kind as the spawner would make it now. */
	Enemy spawnKindAt(EnemyKind kind, double x, double y, boolean champion)
	{
		Enemy e = Spawner.createEnemy(kind, x, y, elapsed, 0, location, champion);
		enemies.add(e);
		return e;
	}

	XpOrb dropOrbAt(double x, double y, int value)
	{
		XpOrb orb = new XpOrb(x, y, value);
		orbs.add(orb);
		return orb;
	}

	Casket dropCasketAt(double x, double y)
	{
		Casket c = new Casket(x, y);
		caskets.add(c);
		return c;
	}

	/** Stops natural spawning, for tests that need a controlled board. */
	void disableSpawning()
	{
		spawning = false;
	}

	/** Gives the player an item or passive rank directly, for tests. */
	void grant(Offer offer)
	{
		take(offer);
	}
}
