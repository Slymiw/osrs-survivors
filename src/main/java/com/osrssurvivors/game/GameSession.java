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
	/** Chance a level-up also offers switching to another combat style. */
	static final double STYLE_SWITCH_CHANCE = 0.15;
	/** From this level on the build has settled on a style, so other styles' weapons aren't offered. */
	static final int STYLE_SWITCH_MAX_LEVEL = 12;
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

	private final Random random;
	private final Arena arena;
	private final Location location;
	private final PathField paths;
	private final Build build = new Build();
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
	private final Map<Location, Double> bestTimes = new EnumMap<>(Location.class);

	private Player player;
	private Weapon weapon;
	private Spawner spawner;
	private Progression progression;
	private Phase phase;
	private double elapsed;
	private int kills;
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
	private double retributionTimer;
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
		phase = Phase.RUNNING;
		elapsed = 0;
		kills = 0;
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
		bonusCoins = 0;
		coinsEarned = -1;
		bossDrop = null;
		levelUpTime = -1;
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
	public void showTitleScreen(TitleScreen screen)
	{
		if (phase == Phase.TITLE)
		{
			titleScreen = screen;
		}
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
		progression.restore(save.level, save.xp, save.pendingLevelUps);
		elapsed = Math.max(0, save.elapsed);
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
		if (save.x >= 0 && save.y >= 0 && save.x < arena.getWidth() && save.y < arena.getHeight()
			&& arena.isWalkable(Arena.tileOf(save.x), Arena.tileOf(save.y))
			&& !arena.isSolidBox(save.x, save.y, Player.COLLISION_RADIUS))
		{
			player.x = save.x;
			player.y = save.y;
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
		if (levelUpTime >= 0 && frameSeconds > 0)
		{
			// the level-up show plays in real time over the paused game
			levelUpTime += Math.min(frameSeconds, MAX_FRAME);
		}
		if (phase == Phase.BOSS_DROP && bossDrop != null && frameSeconds > 0)
		{
			// the reels spin in real time while the game waits
			bossDrop.advance(Math.min(frameSeconds, MAX_FRAME));
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
		if (phase == Phase.RUNNING)
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
		take(choices.get(index));
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
			bossDrop.skip();
			return false;
		}
		BossDrop.Prize prize = bossDrop.getPrize();
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
				build.equip(prize.getGear());
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
		bossDrop = BossDrop.roll(random, items, pets, location.getDifficulty());
		phase = Phase.BOSS_DROP;
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
				build.equip(offer.getGear());
				break;
			case PASSIVE:
				build.addRank(offer.getPassive());
				break;
			case COINS:
				bonusCoins += offer.getCoins();
				break;
			default:
				break;
		}
		applyBuild();
		if (offer.getKind() == Offer.Kind.FOOD)
		{
			player.heal(player.maxHealth * Offer.FOOD_HEAL);
		}
		else if (offer.getPassive() == Passive.HITPOINTS)
		{
			player.heal(20);
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

		if (spawning)
		{
			spawner.update(STEP, elapsed, player, arena, spawnDistance, enemies);
		}
		moveEnemies();
		moveShots();
		if (hurtPlayer())
		{
			return;
		}

		List<Enemy> hit = weapon.update(STEP, player, enemies, strikes, hitsplats, bursts, random);
		double dealt = weapon.lastDamage;
		if (!hit.isEmpty())
		{
			Enemy first = hit.get(0);
			player.attackAge = 0;
			player.facing = Math.atan2(first.y - player.y, first.x - player.x);
		}
		dealt += abilities.update(STEP, build, player, weapon.getMaxHit(), enemies, strikes, hitsplats, bursts, random);
		double lifesteal = build.lifesteal();
		if (lifesteal > 0 && dealt > 0)
		{
			player.heal(dealt * lifesteal);
		}
		removeDeadEnemies();
		collectOrbs();
		collectCaskets();
		strikes.removeIf(s -> (s.remaining -= STEP) <= 0);
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
					e.sightCheck = 8 + i % 5;
				}
				if (!e.inSight)
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
				}
			}
			// shooters keep creeping in while they shoot, so they can't hide behind the crowd forever
			e.pursue(tx, ty, contact == Enemy.STAND_OFF_MARK ? e.radius + Player.RADIUS : contact,
				contact == Enemy.STAND_OFF_MARK ? STEP * Enemy.CREEP : STEP, arena);
		}
		separateEnemies();
	}

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
				double taken = s.damage * (1 - build.damageReduction()) * (1 - build.missileReduction());
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
		double taken = raw * (1 - build.damageReduction()) * (1 - build.contactReduction());
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
			}
			damageTaken = 0;
			splatTimer = PLAYER_SPLAT_INTERVAL;
		}
		if (build.rank(Passive.REDEMPTION) > 0 && redemptionTimer <= 0 && !player.isDead()
			&& player.health < player.maxHealth * REDEMPTION_THRESHOLD)
		{
			player.health = Math.max(player.health, player.maxHealth / 2);
			redemptionTimer = REDEMPTION_COOLDOWN;
		}
		if (build.rank(Passive.RETRIBUTION) > 0 && retributionTimer <= 0 && !player.isDead()
			&& player.health < player.maxHealth * RETRIBUTION_THRESHOLD)
		{
			double blast = weapon.getMaxHit() * 3;
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
				orbs.add(new XpOrb(e.x, e.y, e.xpValue));
				if (random.nextDouble() < build.extraGemChance())
				{
					orbs.add(new XpOrb(e.x + 6, e.y - 6, e.xpValue));
				}
				corpses.add(new Effects.Corpse(e));
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
	 * when hurt. A casket offers gear only. A ready weapon combination always leads.
	 */
	private void offerChoices(boolean casket)
	{
		List<Offer> gear = gearOffers();
		List<Offer> passives = new ArrayList<>();
		Style style = build.getStyle();
		int abilities = 0;
		for (Passive p : Passive.values())
		{
			abilities += p.isAbility() && build.rank(p) > 0 ? 1 : 0;
		}
		for (Passive p : Passive.values())
		{
			int rank = build.rank(p);
			boolean slotFree = !p.isAbility() || rank > 0 || abilities < MAX_ABILITIES;
			if (slotFree && rank < p.getMaxRank() && (p.getStyle() == null || p.getStyle() == style))
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
			gear.sort((a, b) -> Boolean.compare(b.getGear().getSlot() == Slot.WEAPON, a.getGear().getSlot() == Slot.WEAPON));
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
			if (player.health < player.maxHealth * FOOD_THRESHOLD && random.nextDouble() < 0.6)
			{
				if (choices.size() == CHOICE_COUNT)
				{
					choices.remove(CHOICE_COUNT - 1);
				}
				choices.add(Offer.food());
			}
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
		// when upgrades run out, fill up with food and a coin pouch so there's always a real choice
		if (choices.size() < CHOICE_COUNT && !choices.contains(Offer.food()))
		{
			choices.add(Offer.food());
		}
		if (choices.size() < CHOICE_COUNT)
		{
			choices.add(Offer.coins(fillerCoins()));
		}
		phase = Phase.LEVEL_UP;
	}

	/** Offers the next level's upgrades and starts the level-up show. */
	private void levelUp()
	{
		levelUpTime = 0;
		offerChoices(false);
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

	/** Every item that would be an upgrade right now, at most one per slot (two for weapons). */
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
			addPick(offers, Gear.at(Slot.WEAPON, style, weapon.getTier() + 1));
			// a combined or boss weapon outclasses everything else at its tier, its base included
			boolean special = weapon.isUnique();
			if (!special && random.nextDouble() < SIDEGRADE_CHANCE)
			{
				List<Gear> same = Gear.at(Slot.WEAPON, style, weapon.getTier());
				same.remove(weapon);
				addPick(offers, same);
			}
			if (!special && progression.getLevel() < STYLE_SWITCH_MAX_LEVEL && random.nextDouble() < STYLE_SWITCH_CHANCE)
			{
				// a sidegrade into another style at a similar tier
				Style other = Style.values()[(style.ordinal() + 1 + random.nextInt(2)) % Style.values().length];
				addPick(offers, Gear.at(Slot.WEAPON, other, weapon.getTier()));
			}
		}
		for (Slot slot : new Slot[]{Slot.HEAD, Slot.BODY, Slot.LEGS, Slot.SHIELD})
		{
			if (slot == Slot.SHIELD && weapon != null && weapon.isTwoHanded())
			{
				continue;
			}
			int top = Gear.topTier(slot, style);
			Gear current = build.get(slot);
			int tier;
			if (current == null)
			{
				tier = 1;
			}
			else if (current.getStyle() != style)
			{
				// swap into this style's armour of about the same strength (or out of Void robes)
				tier = (int) Math.max(1, Math.round(current.getPower() * top) + (current.getStyle() == null ? 1 : 0));
			}
			else
			{
				tier = current.getTier() + 1;
			}
			List<Gear> candidates = Gear.at(slot, style, Math.min(tier, top + 1));
			candidates.removeIf(g -> g.getStyle() == null && g.getTier() == 0);
			if ((slot == Slot.BODY || slot == Slot.LEGS) && current != null && current.getStyle() != null
				&& current.getPower() >= 0.3 && current.getPower() < 0.6 && random.nextDouble() < VOID_CHANCE)
			{
				candidates.clear();
				candidates.add(slot == Slot.BODY ? Gear.VOID_KNIGHT_TOP : Gear.VOID_KNIGHT_ROBE);
			}
			addPick(offers, candidates);
		}
		for (Slot slot : new Slot[]{Slot.CAPE, Slot.NECK, Slot.HANDS, Slot.FEET, Slot.RING, Slot.AMMO})
		{
			Gear current = build.get(slot);
			int tier = current == null ? 1 : current.getStyle() != null && current.getStyle() != style ? current.getTier() : current.getTier() + 1;
			List<Gear> candidates = Gear.at(slot, style, tier);
			candidates.remove(current);
			addPick(offers, candidates);
		}
		return offers;
	}

	/** Adds one of the candidates, favouring pieces of sets the player has started. */
	private void addPick(List<Offer> offers, List<Gear> candidates)
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
		for (int i = 0; i < weights.length; i++)
		{
			roll -= weights[i];
			if (roll < 0)
			{
				offers.add(Offer.gear(candidates.get(i)));
				return;
			}
		}
		offers.add(Offer.gear(candidates.get(weights.length - 1)));
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
