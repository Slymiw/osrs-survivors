package com.osrssurvivors.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Places to fight. Each is a real spot on the game's map (rebuilt from the client's own map
 * data) with its own monsters, which join one after another as the run goes on, and a
 * difficulty that toughens them.
 */
public enum Location
{
	LUMBRIDGE("Lumbridge", "Rats, goblins and cows around the castle.", 3222, 3218, 1,
		at(0, EnemyKind.CHICKEN, 3), at(0, EnemyKind.RAT, 3), at(20, EnemyKind.GOBLIN, 3), at(60, EnemyKind.MAN, 2),
		at(90, EnemyKind.COW, 2), at(150, EnemyKind.GIANT_SPIDER, 2), at(240, EnemyKind.ZOMBIE, 2),
		at(330, EnemyKind.HILL_GIANT, 1.5), at(480, EnemyKind.MOSS_GIANT, 1), at(660, EnemyKind.LESSER_DEMON, 1),
		at(840, EnemyKind.GREATER_DEMON, 0.8)),
	DRAYNOR("Draynor Village", "Dark wizards and the manor's undead.", 3093, 3250, 2,
		at(0, EnemyKind.MAN, 3), at(0, EnemyKind.RAT, 2), at(40, EnemyKind.DARK_WIZARD, 2), at(90, EnemyKind.ZOMBIE, 2),
		at(150, EnemyKind.SKELETON, 2), at(210, EnemyKind.WOLF, 1.5), at(300, EnemyKind.GHOUL, 1.5),
		at(420, EnemyKind.FERAL_VAMPYRE, 1.2), at(600, EnemyKind.LESSER_DEMON, 1), at(780, EnemyKind.GREATER_DEMON, 0.8)),
	VARROCK("Varrock", "Guards, wizards and barbarians in the square.", 3212, 3428, 2,
		at(0, EnemyKind.MAN, 3), at(0, EnemyKind.RAT, 2), at(30, EnemyKind.GUARD, 2), at(70, EnemyKind.DARK_WIZARD, 2),
		at(120, EnemyKind.BARBARIAN, 2), at(180, EnemyKind.SKELETON, 2), at(240, EnemyKind.HILL_GIANT, 1.5),
		at(330, EnemyKind.MOSS_GIANT, 1.2), at(480, EnemyKind.LESSER_DEMON, 1), at(660, EnemyKind.GREATER_DEMON, 0.8),
		at(900, EnemyKind.BLACK_DEMON, 0.6)),
	BARBARIAN_VILLAGE("Barbarian Village", "Barbarians, druids and giants.", 3082, 3420, 2,
		at(0, EnemyKind.BARBARIAN, 3), at(0, EnemyKind.MAN, 2), at(60, EnemyKind.CHAOS_DRUID, 2),
		at(120, EnemyKind.SKELETON, 2), at(210, EnemyKind.HILL_GIANT, 1.5), at(330, EnemyKind.MOSS_GIANT, 1.2),
		at(480, EnemyKind.LESSER_DEMON, 1), at(660, EnemyKind.GREATER_DEMON, 0.8), at(900, EnemyKind.BLACK_DEMON, 0.6)),
	FALADOR("Falador", "Knights, dwarves and the cold of Ice Mountain.", 2965, 3380, 3,
		at(0, EnemyKind.MAN, 2), at(0, EnemyKind.DWARF, 2), at(40, EnemyKind.GUARD, 2), at(90, EnemyKind.WHITE_KNIGHT, 2),
		at(150, EnemyKind.BLACK_KNIGHT, 2), at(240, EnemyKind.ICE_WARRIOR, 1.5), at(330, EnemyKind.ICE_GIANT, 1.2),
		at(480, EnemyKind.LESSER_DEMON, 1), at(660, EnemyKind.GREATER_DEMON, 0.8), at(840, EnemyKind.BLACK_DEMON, 0.6)),
	AL_KHARID("Al Kharid", "Scorpions, bandits and kalphites from the desert.", 3293, 3174, 3,
		at(0, EnemyKind.SCORPION, 3), at(30, EnemyKind.BANDIT, 2), at(90, EnemyKind.KALPHITE_WORKER, 2),
		at(150, EnemyKind.SKELETON_MAGE, 1.5), at(240, EnemyKind.CROCODILE, 1.5), at(330, EnemyKind.KALPHITE_SOLDIER, 1.2),
		at(480, EnemyKind.LESSER_DEMON, 1), at(660, EnemyKind.GREATER_DEMON, 0.8), at(840, EnemyKind.BLACK_DEMON, 0.6)),
	ARDOUGNE("East Ardougne", "Archers and bandits in the market.", 2662, 3305, 3,
		at(0, EnemyKind.MAN, 3), at(30, EnemyKind.GUARD, 2), at(60, EnemyKind.ARCHER, 2), at(120, EnemyKind.BANDIT, 2),
		at(210, EnemyKind.HILL_GIANT, 1.5), at(300, EnemyKind.MOSS_GIANT, 1.2), at(450, EnemyKind.LESSER_DEMON, 1),
		at(600, EnemyKind.FIRE_GIANT, 1), at(780, EnemyKind.GREATER_DEMON, 0.8)),
	KARAMJA("Musa Point", "Monkeys, jungle spiders and jogres.", 2918, 3165, 4,
		at(0, EnemyKind.MONKEY, 3), at(30, EnemyKind.JUNGLE_SPIDER, 2), at(60, EnemyKind.SCORPION, 2),
		at(150, EnemyKind.JOGRE, 1.5), at(240, EnemyKind.MOSS_GIANT, 1.2), at(360, EnemyKind.LESSER_DEMON, 1),
		at(540, EnemyKind.GREATER_DEMON, 0.8), at(720, EnemyKind.BLACK_DEMON, 0.6)),
	CANIFIS("Canifis", "Werewolves, ghouls and worse in Morytania.", 3494, 3486, 4,
		at(0, EnemyKind.GHOUL, 2), at(0, EnemyKind.ZOMBIE, 2), at(30, EnemyKind.CRAWLING_HAND, 2),
		at(90, EnemyKind.BANSHEE, 2), at(180, EnemyKind.WEREWOLF, 1.5), at(300, EnemyKind.FERAL_VAMPYRE, 1.2),
		at(420, EnemyKind.BLOODVELD, 1), at(600, EnemyKind.ABYSSAL_DEMON, 0.8)),
	WILDERNESS("Wilderness", "Revenants, dragons and demons. Good luck.", 3100, 3560, 5,
		at(0, EnemyKind.SKELETON, 3), at(30, EnemyKind.CHAOS_DRUID, 2), at(60, EnemyKind.ZOMBIE, 2),
		at(90, EnemyKind.REVENANT_IMP, 2), at(120, EnemyKind.HILL_GIANT, 1.5), at(240, EnemyKind.GREEN_DRAGON, 1),
		at(360, EnemyKind.REVENANT_KNIGHT, 1), at(420, EnemyKind.GREATER_DEMON, 0.8), at(600, EnemyKind.BLACK_DEMON, 0.6),
		at(840, EnemyKind.CHAOS_ELEMENTAL, 0.3));

	/** Arena edge length, in tiles, for map locations. */
	public static final int SIZE = 96;

	/** One monster in a location's roster: what, from when, and how common. */
	static final class Entry
	{
		final double from;
		final EnemyKind kind;
		final double weight;

		Entry(double from, EnemyKind kind, double weight)
		{
			this.from = from;
			this.kind = kind;
			this.weight = weight;
		}
	}

	private final String title;
	private final String blurb;
	private final int worldX;
	private final int worldY;
	private final int difficulty;
	private final List<Entry> roster;

	Location(String title, String blurb, int worldX, int worldY, int difficulty, Entry... roster)
	{
		this.title = title;
		this.blurb = blurb;
		this.worldX = worldX;
		this.worldY = worldY;
		this.difficulty = difficulty;
		List<Entry> list = new ArrayList<>();
		Collections.addAll(list, roster);
		list.sort((a, b) -> Double.compare(a.from, b.from));
		this.roster = Collections.unmodifiableList(list);
	}

	private static Entry at(double from, EnemyKind kind, double weight)
	{
		return new Entry(from, kind, weight);
	}

	public String getTitle()
	{
		return title;
	}

	public String getBlurb()
	{
		return blurb;
	}

	/** World tile at the arena's centre. */
	public int getWorldX()
	{
		return worldX;
	}

	public int getWorldY()
	{
		return worldY;
	}

	/** 1 (easiest) to 5. */
	public int getDifficulty()
	{
		return difficulty;
	}

	/** Survival time at the difficulty below that opens up the next difficulty. */
	public static final double UNLOCK_SECONDS = 300;

	/**
	 * Whether this location can be picked, given the best times so far. The easiest places are
	 * always open; each harder place opens once you have survived
	 * {@link #UNLOCK_SECONDS} in a place one difficulty below.
	 */
	public boolean isUnlocked(Map<Location, Double> bestTimes)
	{
		if (difficulty <= 1)
		{
			return true;
		}
		for (Map.Entry<Location, Double> e : bestTimes.entrySet())
		{
			Location l = e.getKey();
			if (l.difficulty == difficulty - 1 && e.getValue() != null && e.getValue() >= UNLOCK_SECONDS)
			{
				return true;
			}
		}
		return false;
	}

	/** What it takes to open this location, for its card. */
	public String unlockHint()
	{
		int s = (int) UNLOCK_SECONDS;
		return String.format("Survive %d:%02d at difficulty %d to unlock.", s / 60, s % 60, difficulty - 1);
	}

	/** Seconds over which a location's difficulty comes in, so every run opens gently. */
	static final double DIFFICULTY_RAMP = 240;

	/** Monster health multiplier for this location at a point in the run. */
	double healthMultiplier(double elapsed)
	{
		return 1 + 0.12 * (difficulty - 1) * ramp(elapsed);
	}

	/** Monster damage multiplier for this location at a point in the run. */
	double damageMultiplier(double elapsed)
	{
		return 1 + 0.08 * (difficulty - 1) * ramp(elapsed);
	}

	private static double ramp(double elapsed)
	{
		return Math.min(1, elapsed / DIFFICULTY_RAMP);
	}

	/** The monsters in this location's roster, in order of arrival. */
	public List<EnemyKind> getKinds()
	{
		List<EnemyKind> kinds = new ArrayList<>();
		for (Entry e : roster)
		{
			kinds.add(e.kind);
		}
		return kinds;
	}

	/**
	 * Picks a monster for a spawn. Everything that has arrived can appear; the three newest
	 * arrivals are the most common, the oldest fade to a trickle.
	 */
	EnemyKind choose(double elapsed, Random random)
	{
		int unlocked = 0;
		while (unlocked < roster.size() && roster.get(unlocked).from <= elapsed)
		{
			unlocked++;
		}
		unlocked = Math.max(1, unlocked);
		double[] weights = new double[unlocked];
		double total = 0;
		for (int i = 0; i < unlocked; i++)
		{
			int age = unlocked - 1 - i;
			weights[i] = roster.get(i).weight * (age < 3 ? 1 : 0.35);
			total += weights[i];
		}
		double roll = random.nextDouble() * total;
		for (int i = 0; i < unlocked; i++)
		{
			roll -= weights[i];
			if (roll < 0)
			{
				return roster.get(i).kind;
			}
		}
		return roster.get(unlocked - 1).kind;
	}

	/** The strongest monster that has arrived by now; champions are made from it. */
	EnemyKind newest(double elapsed)
	{
		EnemyKind kind = roster.get(0).kind;
		for (Entry e : roster)
		{
			if (e.from <= elapsed)
			{
				kind = e.kind;
			}
		}
		return kind;
	}
}
