package com.osrssurvivors.game;

/**
 * Permanent upgrades bought between runs with the coins runs earn, like Vampire Survivors'
 * power-ups. Each rank costs more than the last. They are small on their own and add up over
 * many runs.
 */
public enum Perk
{
	MIGHT("Might", "+4% damage", 5, 150),
	ARMOUR("Armour", "+6 defence", 5, 100),
	VITALITY("Vitality", "+10 max hitpoints", 5, 100),
	RECOVERY("Recovery", "+0.15 hitpoints per second", 5, 100),
	HASTE("Haste", "Attack 3% faster", 5, 200),
	SPEED("Run energy", "+4% movement speed", 3, 120),
	MAGNET("Magnet", "+20 pickup radius", 3, 80),
	WISDOM("Wisdom", "+6% XP", 5, 150),
	GREED("Greed", "+10% coins from runs", 5, 150),
	REROLL("Reroll", "Re-roll the offers once per run, one more per rank", 3, 250),
	REVIVAL("Revival", "Once per run, come back at half hitpoints instead of dying", 1, 1000);

	private final String title;
	private final String effect;
	private final int maxRank;
	private final int baseCost;

	Perk(String title, String effect, int maxRank, int baseCost)
	{
		this.title = title;
		this.effect = effect;
		this.maxRank = maxRank;
		this.baseCost = baseCost;
	}

	public String getTitle()
	{
		return title;
	}

	/** What one rank does. */
	public String getEffect()
	{
		return effect;
	}

	public int getMaxRank()
	{
		return maxRank;
	}

	/** Coins for the next rank when {@code owned} ranks are already bought. */
	public int cost(int owned)
	{
		return baseCost * (owned + 1);
	}
}
