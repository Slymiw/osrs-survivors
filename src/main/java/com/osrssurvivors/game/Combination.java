package com.osrssurvivors.game;

/**
 * Weapon combinations, like Vampire Survivors' evolutions: holding the base weapon with the
 * passive at its top rank makes the combined weapon the lead offer of every level-up and casket
 * until it is taken. Combined weapons sit at their base's tier, but hit harder and do more.
 */
public enum Combination
{
	ABYSSAL_TENTACLE(Gear.ABYSSAL_WHIP, Passive.SWIFTNESS, Gear.ABYSSAL_TENTACLE),
	ARMADYL_GODSWORD_OR(Gear.ARMADYL_GODSWORD, Passive.SPECIAL_ATTACK, Gear.ARMADYL_GODSWORD_OR),
	SANGUINE_SCYTHE(Gear.SCYTHE_OF_VITUR, Passive.BONECRUSHER, Gear.SANGUINE_SCYTHE_OF_VITUR),
	MAGIC_SHORTBOW_I(Gear.MAGIC_SHORTBOW, Passive.LONGRANGE, Gear.MAGIC_SHORTBOW_I),
	DRAGON_HUNTER_CROSSBOW_T(Gear.DRAGON_HUNTER_CROSSBOW, Passive.RANGED_PRAYER, Gear.DRAGON_HUNTER_CROSSBOW_T),
	ICE_ANCIENT_SCEPTRE(Gear.ANCIENT_STAFF, Passive.ICE_BARRAGE, Gear.ICE_ANCIENT_SCEPTRE),
	TRIDENT_OF_THE_SWAMP_E(Gear.TRIDENT_OF_THE_SWAMP, Passive.MULTICOMBAT, Gear.TRIDENT_OF_THE_SWAMP_E),
	HOLY_SANGUINESTI_STAFF(Gear.SANGUINESTI_STAFF, Passive.RAPID_HEAL, Gear.HOLY_SANGUINESTI_STAFF);

	private final Gear base;
	private final Passive passive;
	private final Gear result;

	Combination(Gear base, Passive passive, Gear result)
	{
		this.base = base;
		this.passive = passive;
		this.result = result;
	}

	public Gear getBase()
	{
		return base;
	}

	public Passive getPassive()
	{
		return passive;
	}

	public Gear getResult()
	{
		return result;
	}

	/** The combination a build can make right now, or null. */
	static Combination ready(Build build)
	{
		Gear weapon = build.get(Slot.WEAPON);
		for (Combination c : values())
		{
			if (weapon == c.base && build.rank(c.passive) >= c.passive.getMaxRank())
			{
				return c;
			}
		}
		return null;
	}

	/** The combination a weapon makes with a passive, or null. */
	public static Combination from(Gear weapon, Passive passive)
	{
		for (Combination c : values())
		{
			if (c.base == weapon && c.passive == passive)
			{
				return c;
			}
		}
		return null;
	}

	/** The combination that makes a weapon, or null if it isn't a combined weapon. */
	public static Combination making(Gear result)
	{
		for (Combination c : values())
		{
			if (c.result == result)
			{
				return c;
			}
		}
		return null;
	}

	/** What goes into it, for offer cards: e.g. "Abyssal whip + Swiftness V". */
	public String recipe()
	{
		return base.getTitle() + " + " + passive.title(passive.getMaxRank());
	}
}
