package com.osrssurvivors.game;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What carries over between runs: coins, the permanent upgrades bought with them and the pets
 * won from champions. Stored as JSON (names rather than ordinals, so it survives new versions);
 * unknown names are ignored.
 */
public final class MetaProgress
{
	public int coins;
	public Map<String, Integer> perks = new HashMap<>();
	public List<String> pets = new ArrayList<>();
	/** Bumped on every change, so the owner knows when to store it again; not stored. */
	private transient int changes;

	public int rank(Perk perk)
	{
		Integer r = perks == null ? null : perks.get(perk.name());
		return r == null ? 0 : Math.max(0, Math.min(perk.getMaxRank(), r));
	}

	/** Coins for the next rank of a perk, or -1 when it is at its top rank. */
	public int cost(Perk perk)
	{
		int owned = rank(perk);
		return owned >= perk.getMaxRank() ? -1 : perk.cost(owned);
	}

	public boolean canBuy(Perk perk)
	{
		int cost = cost(perk);
		return cost >= 0 && coins >= cost;
	}

	/** Buys the next rank if there are coins for it. */
	public boolean buy(Perk perk)
	{
		if (!canBuy(perk))
		{
			return false;
		}
		coins -= cost(perk);
		if (perks == null)
		{
			perks = new HashMap<>();
		}
		perks.put(perk.name(), rank(perk) + 1);
		changes++;
		return true;
	}

	public void addCoins(int amount)
	{
		if (amount > 0)
		{
			coins += amount;
			changes++;
		}
	}

	public boolean hasPet(Pet pet)
	{
		return pets != null && pets.contains(pet.name());
	}

	public Set<Pet> getPets()
	{
		Set<Pet> owned = EnumSet.noneOf(Pet.class);
		for (Pet p : Pet.values())
		{
			if (hasPet(p))
			{
				owned.add(p);
			}
		}
		return owned;
	}

	public void addPet(Pet pet)
	{
		if (!hasPet(pet))
		{
			if (pets == null)
			{
				pets = new ArrayList<>();
			}
			pets.add(pet.name());
			changes++;
		}
	}

	/** Developer tool: every upgrade at its top rank and every pet. */
	public void maxOut()
	{
		perks = new HashMap<>();
		for (Perk perk : Perk.values())
		{
			perks.put(perk.name(), perk.getMaxRank());
		}
		pets = new ArrayList<>();
		for (Pet pet : Pet.values())
		{
			pets.add(pet.name());
		}
		changes++;
	}

	/** Developer tool: back to a fresh profile. */
	public void reset()
	{
		coins = 0;
		perks = new HashMap<>();
		pets = new ArrayList<>();
		changes++;
	}

	public int getChanges()
	{
		return changes;
	}
}
