package com.osrssurvivors.game;

/**
 * Ranked upgrades that aren't equipment: prayers, skills, potions, and abilities that fight on
 * their own (cannon, thralls, spells, chinchompas). Each rank can have its own name (the prayer
 * and spell chains), and the effect grows per rank. Style prayers and potions are only offered
 * for the style of the weapon you hold.
 */
public enum Passive
{
	MELEE_PRAYER(Style.MELEE, false, "+5% melee damage", "Burst of Strength", "Superhuman Strength", "Ultimate Strength", "Chivalry", "Piety"),
	RANGED_PRAYER(Style.RANGED, false, "+5% ranged damage", "Sharp Eye", "Hawk Eye", "Eagle Eye", "Deadeye", "Rigour"),
	MAGIC_PRAYER(Style.MAGIC, false, "+5% magic damage", "Mystic Will", "Mystic Lore", "Mystic Might", "Mystic Vigour", "Augury"),
	SUPER_COMBAT(Style.MELEE, false, "+4% melee damage", ranks("Super combat potion", 3)),
	RANGING_POTION(Style.RANGED, false, "+4% ranged damage", ranks("Ranging potion", 3)),
	IMBUED_HEART(Style.MAGIC, false, "+4% magic damage", ranks("Imbued heart", 3)),
	PROTECTION(null, false, "Take 5% less damage", "Thick Skin", "Rock Skin", "Steel Skin"),
	PROTECT_MELEE(null, false, "Take 20% less damage from monsters touching you", "Protect from Melee"),
	PROTECT_MISSILES(null, false, "Take half damage from arrows and spells", "Protect from Missiles"),
	RAPID_HEAL(null, false, "Regenerate 0.6 hitpoints/s", ranks("Rapid Heal", 3)),
	REDEMPTION(null, false, "Below 15% hitpoints, heal to half (every 45 s)", "Redemption"),
	BONECRUSHER(null, false, "Heal 1 hitpoint per kill", ranks("Bonecrusher", 3)),
	HITPOINTS(null, false, "+20 max hitpoints and heal 20", ranks("Hitpoints", 10)),
	AGILITY(null, false, "+8% movement speed", ranks("Agility", 5)),
	SWIFTNESS(null, false, "Attack 8% more often", ranks("Swiftness", 5)),
	LONGRANGE(null, false, "+12% attack range", ranks("Longrange", 4)),
	MULTICOMBAT(null, false, "Hit one more enemy per attack", ranks("Multicombat", 4)),
	TELEKINETIC_GRAB(null, false, "+40 gem pickup radius", ranks("Telekinetic Grab", 5)),
	LAMP(null, false, "+5% XP from gems", ranks("Lamp of Knowledge", 3)),
	VENGEANCE(null, false, "Enemies touching you take 15 damage/s", ranks("Vengeance", 3)),
	SPECIAL_ATTACK(null, false, "A double-damage special attack every 12 s, 2 s sooner per rank", ranks("Special Attack", 3)),
	CANNON(null, true, "Sets up a cannon that fires at monsters around it", ranks("Dwarf multicannon", 4)),
	THRALLS(null, true, "A thrall fights beside you, one more per rank", "Resurrect Lesser Thrall", "Resurrect Superior Thrall",
		"Resurrect Greater Thrall"),
	ICE_BARRAGE(null, true, "Freezes and hits a clump of monsters every few seconds", "Ice Rush", "Ice Burst", "Ice Blitz", "Ice Barrage"),
	CHINCHOMPAS(null, true, "Throws exploding chinchompas at monsters", ranks("Chinchompas", 4)),
	OVERLOAD(null, false, "+4% damage with any style", ranks("Overload", 3)),
	SLAYER(null, false, "+20% damage against champions", ranks("Slayer", 3)),
	THIEVING(null, false, "Kills have a 10% chance to drop a second gem", ranks("Thieving", 3)),
	RETRIBUTION(null, false, "Below 25% hitpoints, blast nearby monsters for 3x your max hit (every 30 s)", "Retribution"),
	BLOOD_BARRAGE(null, true, "Drains a clump of monsters every few seconds, healing you for 15% of the damage", "Blood Rush",
		"Blood Burst", "Blood Blitz", "Blood Barrage"),
	GOD_SPELLS(null, true, "Calls down god spells on two monsters every few seconds, one more per rank", "Saradomin Strike",
		"Claws of Guthix", "Flames of Zamorak"),
	DRAGONFIRE(null, true, "Breathes dragonfire over the monsters in front of you every few seconds", ranks("Dragonfire shield", 3));

	private final Style style;
	private final boolean ability;
	private final String effect;
	private final String[] titles;

	Passive(Style style, boolean ability, String effect, String... titles)
	{
		this.style = style;
		this.ability = ability;
		this.effect = effect;
		this.titles = titles;
	}

	private static String[] ranks(String title, int count)
	{
		String[] t = new String[count];
		for (int i = 0; i < count; i++)
		{
			t[i] = count == 1 ? title : title + " " + roman(i + 1);
		}
		return t;
	}

	private static String roman(int n)
	{
		String[] numerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
		return numerals[n - 1];
	}

	/** The style this passive is tied to, or null if it helps every style. */
	public Style getStyle()
	{
		return style;
	}

	/** True for abilities that attack by themselves. */
	public boolean isAbility()
	{
		return ability;
	}

	public int getMaxRank()
	{
		return titles.length;
	}

	/** Name of a rank, 1-based. */
	public String title(int rank)
	{
		return titles[rank - 1];
	}

	/** What one rank does. */
	public String getEffect()
	{
		return effect;
	}
}
