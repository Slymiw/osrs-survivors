package com.osrssurvivors.game;

import java.util.Objects;

/**
 * One choice on the level-up screen: a piece of gear (worn at once, or put in the bag when it is
 * for another combat style), the next rank of a passive, or a filler (a potion, food, a coin
 * pouch) when there aren't enough upgrades left. Potions and food go in the bag.
 */
public final class Offer
{
	public enum Kind
	{
		GEAR, PASSIVE, FOOD, COINS, BOOST
	}

	private final Kind kind;
	private final Gear gear;
	private final Passive passive;
	private final Boost boost;
	private final Food food;
	/** Passive rank gained (1-based), coins in a pouch, or pieces of food. */
	private final int rank;
	/** Gear that goes in the bag rather than being worn. */
	private final boolean stored;

	private Offer(Kind kind, Gear gear, Passive passive, Boost boost, Food food, int rank, boolean stored)
	{
		this.kind = kind;
		this.gear = gear;
		this.passive = passive;
		this.boost = boost;
		this.food = food;
		this.rank = rank;
		this.stored = stored;
	}

	/** A four-dose potion for the bag. */
	public static Offer boost(Boost boost)
	{
		return new Offer(Kind.BOOST, null, null, boost, null, Boost.DOSES, false);
	}

	/** The potion for {@link Kind#BOOST}, else null. */
	public Boost getBoost()
	{
		return boost;
	}

	/** Gear worn as soon as it is taken. */
	public static Offer gear(Gear gear)
	{
		return new Offer(Kind.GEAR, gear, null, null, null, 0, false);
	}

	/** Gear for another combat style, put in the bag to switch to later. */
	public static Offer stored(Gear gear)
	{
		return new Offer(Kind.GEAR, gear, null, null, null, 0, true);
	}

	public static Offer passive(Passive passive, int rank)
	{
		return new Offer(Kind.PASSIVE, null, passive, null, null, rank, false);
	}

	/** {@code count} pieces of food for the bag. */
	public static Offer food(Food food, int count)
	{
		return new Offer(Kind.FOOD, null, null, null, food, count, false);
	}

	public static Offer coins(int amount)
	{
		return new Offer(Kind.COINS, null, null, null, null, amount, false);
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

	/** True for gear that goes in the bag instead of being worn. */
	public boolean isStored()
	{
		return stored;
	}

	/** The passive for {@link Kind#PASSIVE}, else null. */
	public Passive getPassive()
	{
		return passive;
	}

	/** The food for {@link Kind#FOOD}, else null. */
	public Food getFood()
	{
		return food;
	}

	/** The rank gained for {@link Kind#PASSIVE}, 1-based; pieces of food for {@link Kind#FOOD}. */
	public int getRank()
	{
		return rank;
	}

	/** Coins in the pouch for {@link Kind#COINS}. */
	public int getCoins()
	{
		return kind == Kind.COINS ? rank : 0;
	}

	/** Free bag slots taking this offer needs. */
	int bagSlots()
	{
		switch (kind)
		{
			case GEAR:
				return stored ? 1 : 0;
			case FOOD:
				return 1;
			case BOOST:
				return 1;
			default:
				return 0;
		}
	}

	public String getTitle()
	{
		switch (kind)
		{
			case GEAR:
				return gear.getTitle();
			case PASSIVE:
				return passive.title(rank);
			case COINS:
				return "Coin pouch";
			case BOOST:
				return boost.getTitle();
			default:
				return rank > 1 ? rank + " " + plural(food.getTitle()) : food.getTitle();
		}
	}

	private static String plural(String title)
	{
		return title.endsWith("fish") ? title : title + "s";
	}

	public String getDescription()
	{
		switch (kind)
		{
			case GEAR:
				return gear.describe() + (stored ? ". Goes in your bag" : "");
			case PASSIVE:
				return passive.getEffect();
			case COINS:
				return "+" + rank + " coins, kept when the run ends";
			case BOOST:
				return boost.getEffect() + " for " + (int) Boost.DOSE_SECONDS + " s a dose, " + Boost.DOSES + " doses. Drink from your bag";
			default:
				return food.getEffect() + ". Eat from your bag";
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Offer))
		{
			return false;
		}
		Offer other = (Offer) o;
		return kind == other.kind && gear == other.gear && passive == other.passive && boost == other.boost && food == other.food
			&& rank == other.rank && stored == other.stored;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(kind, gear, passive, boost, food, rank, stored);
	}

	@Override
	public String toString()
	{
		return getTitle();
	}
}
