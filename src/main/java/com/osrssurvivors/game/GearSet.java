package com.osrssurvivors.game;

/** Armour sets with a bonus for wearing the whole set, like the game's Barrows and Void sets. */
public enum GearSet
{
	DHAROK("Dharok's", 3, "More damage the lower your hitpoints"),
	GUTHAN("Guthan's", 3, "Melee hits heal you for 15% of their damage"),
	VERAC("Verac's", 3, "Melee attacks cleave 2 more enemies"),
	TORAG("Torag's", 3, "+40 defence and faster healing"),
	KARIL("Karil's", 3, "A quarter of attacks fire twice"),
	AHRIM("Ahrim's", 3, "+20% magic damage and bigger splashes"),
	VOID("Void knight", 4, "+20% damage with any style"),
	INQUISITOR("Inquisitor's", 3, "+15% melee damage, attacks 10% faster"),
	OBSIDIAN("Obsidian", 3, "+10% melee damage and +20 defence; +20% more with the Tzhaar-ket-om"),
	CRYSTAL("Crystal", 3, "+10% ranged damage; +30% more with the Bow of faerdhinen"),
	VIRTUS("Virtus", 3, "+12% magic damage, and spells freeze monsters briefly");

	private final String title;
	private final int pieces;
	private final String bonus;

	GearSet(String title, int pieces, String bonus)
	{
		this.title = title;
		this.pieces = pieces;
		this.bonus = bonus;
	}

	public String getTitle()
	{
		return title;
	}

	/** Armour pieces needed for the bonus. */
	public int getPieces()
	{
		return pieces;
	}

	public String getBonus()
	{
		return bonus;
	}
}
