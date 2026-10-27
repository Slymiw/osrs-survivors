package com.osrssurvivors.game;

/**
 * How rare a piece of gear is, shown on its cards and tooltips. Chain items rank by how far up
 * their slot's chain they sit; boss drops and combination weapons are legendary; the best weapons
 * of all (scythe, twisted bow, Tumeken's shadow, obligator), only from a casket, are megarare.
 */
public enum ItemRarity
{
	COMMON("Common"),
	UNCOMMON("Uncommon"),
	RARE("Rare"),
	EPIC("Epic"),
	LEGENDARY("Legendary"),
	MEGARARE("Megarare");

	private final String title;

	ItemRarity(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}

	/** The rarity of a chain item at {@code tier} in a chain whose top tier is {@code topTier}. */
	static ItemRarity ofTier(int tier, int topTier)
	{
		double place = tier / (double) Math.max(1, topTier);
		return place >= 0.9 ? EPIC : place >= 0.7 ? RARE : place >= 0.4 ? UNCOMMON : COMMON;
	}
}
