package com.osrssurvivors.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A champion's unique drop, revealed on three spinning reels like a slot machine. The prize is
 * rolled when the casket is opened; the reels are dressed to land on it one after another, and
 * how big the celebration is depends on its rarity. Coin pouches and XP lamps are common, boss
 * items rare, a new pet is the jackpot, and rarest of all is a megarare weapon.
 */
public final class BossDrop
{
	public enum Rarity
	{
		COMMON("Loot!"),
		UNCOMMON("Nice drop!"),
		RARE("Rare drop!"),
		JACKPOT("New pet!"),
		/** Rarer than a pet: the scythe, the twisted bow, Tumeken's shadow or the obligator. */
		MEGARARE("Megarare!");

		private final String banner;

		Rarity(String banner)
		{
			this.banner = banner;
		}

		/** The headline shown when the reels stop. */
		public String getBanner()
		{
			return banner;
		}
	}

	/** What can be won. */
	public static final class Prize
	{
		public enum Kind
		{
			COINS, LAMP, GEAR, PET
		}

		final Kind kind;
		final Gear gear;
		final Pet pet;
		final int amount;

		private Prize(Kind kind, Gear gear, Pet pet, int amount)
		{
			this.kind = kind;
			this.gear = gear;
			this.pet = pet;
			this.amount = amount;
		}

		static Prize coins(int amount)
		{
			return new Prize(Kind.COINS, null, null, amount);
		}

		static Prize lamp(int levels)
		{
			return new Prize(Kind.LAMP, null, null, levels);
		}

		static Prize gear(Gear gear)
		{
			return new Prize(Kind.GEAR, gear, null, 0);
		}

		static Prize pet(Pet pet)
		{
			return new Prize(Kind.PET, null, pet, 0);
		}

		public Kind getKind()
		{
			return kind;
		}

		/** The item for {@link Kind#GEAR}, else null. */
		public Gear getGear()
		{
			return gear;
		}

		/** The pet for {@link Kind#PET}, else null. */
		public Pet getPet()
		{
			return pet;
		}

		/** Coins for {@link Kind#COINS}, levels for {@link Kind#LAMP}. */
		public int getAmount()
		{
			return amount;
		}

		public String getTitle()
		{
			switch (kind)
			{
				case COINS:
					return amount + " coins";
				case LAMP:
					return "Lamp of " + amount + " levels";
				case GEAR:
					return gear.getTitle();
				default:
					return pet.getTitle();
			}
		}

		public String getDescription()
		{
			switch (kind)
			{
				case COINS:
					return "Kept when the run ends. Your hitpoints are restored too.";
				case LAMP:
					return "Gain " + amount + " levels at once";
				case GEAR:
					return gear.describe();
				default:
					return pet.getEffect() + ", in every run from now on";
			}
		}
	}

	/** Boss-only items, rolled for a rare drop. */
	static final List<Gear> ITEMS = Collections.unmodifiableList(Arrays.asList(Gear.INFERNAL_MAX_CAPE, Gear.AMULET_OF_RANCOUR,
		Gear.CONFLICTION_GAUNTLETS, Gear.AVERNIC_TREADS, Gear.RING_OF_THE_GODS, Gear.DIZANAS_QUIVER, Gear.ELYSIAN_SPIRIT_SHIELD,
		Gear.RONDACHE, Gear.IMBUED_ANCIENT_CAPE));
	/** Megarare weapons, at least one per style; a casket only offers those for the player's style. */
	static final List<Gear> MEGARARES = Collections.unmodifiableList(Arrays.asList(Gear.SCYTHE_OF_VITUR, Gear.OBLIGATOR,
		Gear.TWISTED_BOW, Gear.TUMEKENS_SHADOW));

	/** About one casket in 250: a megarare outclasses everything, so it should feel like one. */
	static final double MEGARARE_CHANCE = 0.004;
	static final double JACKPOT_CHANCE = 0.08;
	static final double RARE_CHANCE = 0.27;
	static final double UNCOMMON_CHANCE = 0.33;
	/** When each reel stops, in seconds after the casket opens. */
	public static final double[] STOPS = {1.3, 2.2, 3.8};
	/** Seconds until the prize can be claimed, so the reveal lands before a stray click skips it. */
	public static final double CLAIMABLE = 4.1;
	/** Symbols each reel runs through before landing. */
	static final int STRIP = 22;

	private final Rarity rarity;
	private final Prize prize;
	private final List<List<Prize>> reels = new ArrayList<>();
	private double time;

	BossDrop(Rarity rarity, Prize prize, List<Prize> symbols, Random random)
	{
		this.rarity = rarity;
		this.prize = prize;
		for (int r = 0; r < STOPS.length; r++)
		{
			List<Prize> strip = new ArrayList<>();
			for (int i = 0; i < STRIP - 1; i++)
			{
				strip.add(symbols.get(random.nextInt(symbols.size())));
			}
			strip.add(prize);
			// the symbol after the prize peeks in below the payline as the reel settles
			strip.add(symbols.get(random.nextInt(symbols.size())));
			reels.add(Collections.unmodifiableList(strip));
		}
	}

	/**
	 * Rolls a drop. {@code megarares} are the megarare weapons on offer (the player's style, not
	 * held yet), {@code items} the boss items the player could still use, {@code pets} the pets
	 * not yet won. A roll for something unavailable falls to the next rarity down.
	 */
	static BossDrop roll(Random random, List<Gear> megarares, List<Gear> items, List<Pet> pets, int difficulty)
	{
		double m = random.nextDouble();
		Rarity rarity;
		Prize prize;
		// the megarare has its own roll, so the other odds stay as they were
		if (m < MEGARARE_CHANCE && !megarares.isEmpty())
		{
			return new BossDrop(Rarity.MEGARARE, Prize.gear(megarares.get(random.nextInt(megarares.size()))), symbols(null), random);
		}
		double r = random.nextDouble();
		if (r < JACKPOT_CHANCE && !pets.isEmpty())
		{
			rarity = Rarity.JACKPOT;
			prize = Prize.pet(pets.get(random.nextInt(pets.size())));
		}
		else if (r < JACKPOT_CHANCE + RARE_CHANCE && !items.isEmpty())
		{
			rarity = Rarity.RARE;
			prize = Prize.gear(items.get(random.nextInt(items.size())));
		}
		else if (r < JACKPOT_CHANCE + RARE_CHANCE + UNCOMMON_CHANCE)
		{
			rarity = Rarity.UNCOMMON;
			prize = Prize.lamp(2);
		}
		else
		{
			rarity = Rarity.COMMON;
			prize = Prize.coins(40 + 30 * difficulty);
		}
		return new BossDrop(rarity, prize, symbols(prize), random);
	}

	/** The reels show everything that could have been won, pets and megarares included. */
	private static List<Prize> symbols(Prize prize)
	{
		List<Prize> symbols = new ArrayList<>();
		symbols.add(Prize.coins(prize != null && prize.kind == Prize.Kind.COINS ? prize.amount : 100));
		symbols.add(Prize.lamp(2));
		for (Gear g : ITEMS)
		{
			symbols.add(Prize.gear(g));
		}
		for (Gear g : MEGARARES)
		{
			symbols.add(Prize.gear(g));
		}
		for (Pet p : Pet.values())
		{
			symbols.add(Prize.pet(p));
		}
		return symbols;
	}

	void advance(double dt)
	{
		time += dt;
	}

	/** Jumps to the end of the spin. */
	void skip()
	{
		time = Math.max(time, STOPS[STOPS.length - 1]);
	}

	public Rarity getRarity()
	{
		return rarity;
	}

	public Prize getPrize()
	{
		return prize;
	}

	/** Seconds since the casket opened. */
	public double getTime()
	{
		return time;
	}

	public boolean isSpinning()
	{
		return time < STOPS[STOPS.length - 1];
	}

	public boolean isClaimable()
	{
		return time >= CLAIMABLE;
	}

	/** The symbols on a reel, top to bottom; the prize is second from last. */
	public List<Prize> reel(int index)
	{
		return reels.get(index);
	}

	public int reelCount()
	{
		return reels.size();
	}

	/**
	 * How far a reel has turned, in symbols: 0 at the start, {@code STRIP - 1} (the prize on the
	 * payline) once stopped. Fast at first, then easing in with a small overshoot.
	 */
	public double reelPosition(int index)
	{
		double stop = STOPS[index];
		double t = Math.min(1, time / stop);
		// ease-out-back: overshoots a little and settles, like a reel clicking into place
		double c = 1.4;
		double u = t - 1;
		double eased = 1 + (c + 1) * u * u * u + c * u * u;
		return (STRIP - 1) * eased;
	}

	/** True once a reel has stopped on the prize. */
	public boolean isStopped(int index)
	{
		return time >= STOPS[index];
	}
}
