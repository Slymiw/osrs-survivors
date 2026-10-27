package com.osrssurvivors.game;

/** The bag: 28 slots like the game's inventory, each empty or holding one {@link Item}. */
public final class Inventory
{
	public static final int SIZE = 28;

	private final Item[] slots = new Item[SIZE];

	/** The item in a slot, or null. */
	public Item get(int slot)
	{
		return slot < 0 || slot >= SIZE ? null : slots[slot];
	}

	void set(int slot, Item item)
	{
		slots[slot] = item;
	}

	/** Puts an item in the first free slot; returns false when the bag is full. */
	boolean add(Item item)
	{
		int free = firstFree();
		if (free < 0)
		{
			return false;
		}
		slots[free] = item;
		return true;
	}

	/** The first empty slot, or -1. */
	int firstFree()
	{
		for (int i = 0; i < SIZE; i++)
		{
			if (slots[i] == null)
			{
				return i;
			}
		}
		return -1;
	}

	public int freeSlots()
	{
		int free = 0;
		for (Item i : slots)
		{
			free += i == null ? 1 : 0;
		}
		return free;
	}

	/** The slot holding a piece of gear for a slot and style, or -1. */
	int find(Slot slot, Style style)
	{
		for (int i = 0; i < SIZE; i++)
		{
			Gear g = slots[i] == null ? null : slots[i].getGear();
			if (g != null && g.getSlot() == slot && g.getStyle() == style)
			{
				return i;
			}
		}
		return -1;
	}

	/** Whether any slot holds this gear. */
	boolean contains(Gear gear)
	{
		for (Item i : slots)
		{
			if (i != null && i.getGear() == gear)
			{
				return true;
			}
		}
		return false;
	}

	void clear()
	{
		for (int i = 0; i < SIZE; i++)
		{
			slots[i] = null;
		}
	}

	/** Swaps two slots (either may be empty), for rearranging the bag. */
	void swap(int a, int b)
	{
		Item t = slots[a];
		slots[a] = slots[b];
		slots[b] = t;
	}
}
