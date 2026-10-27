package com.osrssurvivors.game;

import java.util.Objects;

/** Something in the bag: a piece of gear, a potion with doses left, or a piece of food. */
public final class Item
{
	private final Gear gear;
	private final Boost potion;
	private final Food food;
	/** Doses left, for potions. */
	private final int doses;

	private Item(Gear gear, Boost potion, Food food, int doses)
	{
		this.gear = gear;
		this.potion = potion;
		this.food = food;
		this.doses = doses;
	}

	public static Item gear(Gear gear)
	{
		return new Item(Objects.requireNonNull(gear), null, null, 0);
	}

	public static Item potion(Boost potion, int doses)
	{
		return new Item(null, Objects.requireNonNull(potion), null, Math.max(1, Math.min(Boost.DOSES, doses)));
	}

	public static Item food(Food food)
	{
		return new Item(null, null, Objects.requireNonNull(food), 0);
	}

	/** The gear, or null. */
	public Gear getGear()
	{
		return gear;
	}

	/** The potion, or null. */
	public Boost getPotion()
	{
		return potion;
	}

	/** The food, or null. */
	public Food getFood()
	{
		return food;
	}

	public int getDoses()
	{
		return doses;
	}

	/** The same potion with one dose fewer, or null when that was the last. */
	Item sipped()
	{
		return doses > 1 ? new Item(null, potion, null, doses - 1) : null;
	}

	public String getTitle()
	{
		if (gear != null)
		{
			return gear.getTitle();
		}
		if (potion != null)
		{
			return potion.getTitle() + " (" + doses + ")";
		}
		return food.getTitle();
	}

	/** What using it does, for its tooltip. */
	public String describe()
	{
		if (gear != null)
		{
			return gear.describe();
		}
		if (potion != null)
		{
			return potion.getEffect() + " for " + (int) Boost.DOSE_SECONDS + " s a dose";
		}
		return food.getEffect();
	}

	/** Save form: "GEAR:NAME", "POTION:NAME:DOSES" or "FOOD:NAME". */
	String key()
	{
		if (gear != null)
		{
			return "GEAR:" + gear.name();
		}
		if (potion != null)
		{
			return "POTION:" + potion.name() + ":" + doses;
		}
		return "FOOD:" + food.name();
	}

	/** Reads {@link #key()}; null for anything unknown. */
	static Item fromKey(String key)
	{
		if (key == null)
		{
			return null;
		}
		String[] parts = key.split(":");
		try
		{
			switch (parts[0])
			{
				case "GEAR":
					return gear(Gear.valueOf(parts[1]));
				case "POTION":
					return potion(Boost.valueOf(parts[1]), parts.length > 2 ? Integer.parseInt(parts[2]) : Boost.DOSES);
				case "FOOD":
					return food(Food.valueOf(parts[1]));
				default:
					return null;
			}
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Item))
		{
			return false;
		}
		Item other = (Item) o;
		return gear == other.gear && potion == other.potion && food == other.food && doses == other.doses;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(gear, potion, food, doses);
	}

	@Override
	public String toString()
	{
		return getTitle();
	}
}
