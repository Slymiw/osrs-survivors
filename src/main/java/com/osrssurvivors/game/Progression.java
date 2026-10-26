package com.osrssurvivors.game;

/** XP and level tracking. Level-ups are queued so several at once are each offered an upgrade. */
public final class Progression
{
	private int level = 1;
	private int xp;
	private int pendingLevelUps;

	/** XP needed to go from {@code level} to {@code level + 1}: quick at first, steeper later. */
	public static int xpToNext(int level)
	{
		int n = level - 1;
		return (int) Math.round(4 + 2.5 * n + 0.03 * n * n);
	}

	public int getLevel()
	{
		return level;
	}

	/** XP gathered towards the next level. */
	public int getXp()
	{
		return xp;
	}

	public int getXpToNext()
	{
		return xpToNext(level);
	}

	public int getPendingLevelUps()
	{
		return pendingLevelUps;
	}

	/** Returns the number of levels gained. */
	int addXp(int amount)
	{
		xp += amount;
		int gained = 0;
		while (xp >= xpToNext(level))
		{
			xp -= xpToNext(level);
			level++;
			gained++;
		}
		pendingLevelUps += gained;
		return gained;
	}

	/** Gains whole levels at once (an XP lamp), each offering an upgrade. */
	void grantLevels(int levels)
	{
		level += levels;
		pendingLevelUps += levels;
	}

	/** Restores saved progress. */
	void restore(int level, int xp, int pendingLevelUps)
	{
		this.level = Math.max(1, level);
		this.xp = Math.max(0, xp);
		this.pendingLevelUps = Math.max(0, pendingLevelUps);
	}

	void consumeLevelUp()
	{
		pendingLevelUps--;
	}
}
