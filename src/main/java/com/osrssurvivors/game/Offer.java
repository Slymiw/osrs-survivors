package com.osrssurvivors.game;

import java.util.Objects;

/**
 * One choice on the level-up screen: a piece of gear, the next rank of a passive, or a filler
 * (food, a coin pouch) when there aren't enough upgrades left.
 */
public final class Offer
{
	public enum Kind
	{
		GEAR, PASSIVE, FOOD, COINS
	}

	/** Fraction of max hitpoints a food offer restores. */
	public static final double FOOD_HEAL = 0.5;

	private final Kind kind;
	private final Gear gear;
	private final Passive passive;
	/** Passive rank gained, 1-based, or coins in a pouch. */
	private final int rank;

	private Offer(Kind kind, Gear gear, Passive passive, int rank)
	{
		this.kind = kind;
		this.gear = gear;
		this.passive = passive;
		this.rank = rank;
	}

	public static Offer gear(Gear gear)
	{
		return new Offer(Kind.GEAR, gear, null, 0);
	}

	public static Offer passive(Passive passive, int rank)
	{
		return new Offer(Kind.PASSIVE, null, passive, rank);
	}

	public static Offer food()
	{
		return new Offer(Kind.FOOD, null, null, 0);
	}

	public static Offer coins(int amount)
	{
		return new Offer(Kind.COINS, null, null, amount);
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

	/** The passive for {@link Kind#PASSIVE}, else null. */
	public Passive getPassive()
	{
		return passive;
	}

	/** The rank gained for {@link Kind#PASSIVE}, 1-based. */
	public int getRank()
	{
		return rank;
	}

	/** Coins in the pouch for {@link Kind#COINS}. */
	public int getCoins()
	{
		return kind == Kind.COINS ? rank : 0;
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
			default:
				return "Shark";
		}
	}

	public String getDescription()
	{
		switch (kind)
		{
			case GEAR:
				return gear.describe();
			case PASSIVE:
				return passive.getEffect();
			case COINS:
				return "+" + rank + " coins, kept when the run ends";
			default:
				return "Heal " + Math.round(FOOD_HEAL * 100) + "% of your hitpoints";
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
		return kind == other.kind && gear == other.gear && passive == other.passive && rank == other.rank;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(kind, gear, passive, rank);
	}

	@Override
	public String toString()
	{
		return getTitle();
	}
}
