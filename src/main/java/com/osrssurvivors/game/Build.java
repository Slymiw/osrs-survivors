package com.osrssurvivors.game;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Everything the player has collected this run (worn gear and passive ranks) and the combat
 * numbers derived from it, plus what carries over between runs (permanent upgrades and pets).
 * Starts empty: no gear, bare fists. Attacks start at the game's own speeds and get faster with
 * every level gained in the run.
 */
public final class Build
{
	// bare-handed punches, four ticks apart like the game's
	public static final double FIST_DAMAGE = 28;
	public static final double FIST_COOLDOWN = 4 * Gear.TICK;
	public static final double FIST_RANGE = 40;

	/** Damage reduction from defence is {@code defence / (defence + DEFENCE_SCALE)}. */
	static final double DEFENCE_SCALE = 150;
	static final double MAX_REDUCTION = 0.75;
	static final double MIN_COOLDOWN = 0.15;
	/** Attack time multiplier per level gained in a run, down to {@link #MIN_LEVEL_HASTE}. */
	static final double LEVEL_HASTE = 0.977;
	static final double MIN_LEVEL_HASTE = 0.45;
	// what each style does beside its main target
	/** Melee: extra enemies a swing catches next to its target. */
	static final int MELEE_CLEAVE = 2;
	/** Ranged: extra enemies each missile passes through (two from tier 5 weapons). */
	static final int RANGED_PIERCE = 1;
	/** Magic: radius of the splash around each target. */
	static final double MAGIC_SPLASH = 34;
	/** Melee: seconds each hit staggers a monster. */
	static final double MELEE_STAGGER = 0.6;
	/** Melee: damage reduction from fighting up close in heavier armour. */
	static final double MELEE_GUARD = 0.14;

	private final Map<Slot, Gear> worn = new EnumMap<>(Slot.class);
	private final Map<Passive, Integer> ranks = new EnumMap<>(Passive.class);
	/** Potion doses taken this run. */
	private final Map<Boost, Integer> doses = new EnumMap<>(Boost.class);
	/** Bumped on every gear change so renderers can tell when the character's look changed. */
	private int gearVersion;
	/** Permanent upgrades; null for none. */
	private MetaProgress meta;
	private final Set<Pet> pets = EnumSet.noneOf(Pet.class);
	private int level = 1;

	public Gear get(Slot slot)
	{
		return worn.get(slot);
	}

	public Map<Slot, Gear> getWorn()
	{
		return Collections.unmodifiableMap(worn);
	}

	public int rank(Passive p)
	{
		return ranks.getOrDefault(p, 0);
	}

	public Map<Passive, Integer> getRanks()
	{
		return Collections.unmodifiableMap(ranks);
	}

	public int getGearVersion()
	{
		return gearVersion;
	}

	/** The style of the held weapon; bare fists count as melee. */
	public Style getStyle()
	{
		Gear weapon = worn.get(Slot.WEAPON);
		return weapon == null ? Style.MELEE : weapon.getStyle();
	}

	/** Puts on an item, replacing whatever was in its slot. Two-handed weapons remove the shield. */
	void equip(Gear gear)
	{
		worn.put(gear.getSlot(), gear);
		if (gear.getSlot() == Slot.WEAPON && gear.isTwoHanded())
		{
			worn.remove(Slot.SHIELD);
		}
		gearVersion++;
	}

	void addRank(Passive p)
	{
		ranks.merge(p, 1, Integer::sum);
	}

	void addDose(Boost b)
	{
		doses.merge(b, 1, Integer::sum);
	}

	public int doses(Boost b)
	{
		return doses.getOrDefault(b, 0);
	}

	public Map<Boost, Integer> getDoses()
	{
		return Collections.unmodifiableMap(doses);
	}

	void clear()
	{
		worn.clear();
		ranks.clear();
		doses.clear();
		level = 1;
		gearVersion++;
	}

	void setMeta(MetaProgress meta)
	{
		this.meta = meta;
	}

	/** Pets following the player: those won before and any won this run. */
	void setPets(Set<Pet> owned)
	{
		pets.clear();
		pets.addAll(owned);
	}

	public Set<Pet> getPets()
	{
		return Collections.unmodifiableSet(pets);
	}

	/** The run's level, for the attack speed-up. */
	void setLevel(int level)
	{
		this.level = Math.max(1, level);
	}

	public int perk(Perk p)
	{
		return meta == null ? 0 : meta.rank(p);
	}

	private boolean pet(Pet p)
	{
		return pets.contains(p);
	}

	/** Attack time multiplier from levels gained this run: 1 at level 1, falling to 0.45. */
	public double levelHaste()
	{
		return Math.max(MIN_LEVEL_HASTE, Math.pow(LEVEL_HASTE, level - 1));
	}

	/** Sets whose armour is worn in full. */
	public Set<GearSet> activeSets()
	{
		Map<GearSet, Integer> pieces = new EnumMap<>(GearSet.class);
		for (Gear g : worn.values())
		{
			if (g.getSet() != null && g.getSlot() != Slot.WEAPON)
			{
				pieces.merge(g.getSet(), 1, Integer::sum);
			}
		}
		Set<GearSet> active = EnumSet.noneOf(GearSet.class);
		for (Map.Entry<GearSet, Integer> e : pieces.entrySet())
		{
			if (e.getValue() >= e.getKey().getPieces())
			{
				active.add(e.getKey());
			}
		}
		return active;
	}

	public boolean hasSet(GearSet set)
	{
		return activeSets().contains(set);
	}

	// derived numbers

	public double weaponDamage()
	{
		Gear w = worn.get(Slot.WEAPON);
		return w == null ? FIST_DAMAGE : w.getDamage();
	}

	public double attackCooldown()
	{
		Gear w = worn.get(Slot.WEAPON);
		double base = w == null ? FIST_COOLDOWN : w.getCooldown();
		if (getStyle() == Style.MELEE && hasSet(GearSet.INQUISITOR))
		{
			base *= 0.9;
		}
		base *= levelHaste() * Math.pow(0.97, perk(Perk.HASTE)) * (pet(Pet.TZREK_JAD) ? 0.96 : 1) * (pet(Pet.AKKHITO) ? 0.98 : 1);
		return Math.max(MIN_COOLDOWN, base * Math.pow(0.92, rank(Passive.SWIFTNESS)));
	}

	public double attackRange()
	{
		Gear w = worn.get(Slot.WEAPON);
		double base = w == null ? FIST_RANGE : w.getRange();
		return base * (1 + 0.12 * rank(Passive.LONGRANGE));
	}

	/** Separate main targets per attack. */
	public int targets()
	{
		Gear w = worn.get(Slot.WEAPON);
		return (w == null ? 1 : w.getTargets()) + rank(Passive.MULTICOMBAT);
	}

	/** Melee: extra enemies caught by each swing. */
	public int cleave()
	{
		Gear w = worn.get(Slot.WEAPON);
		if (w == null || w.getStyle() != Style.MELEE)
		{
			return 0;
		}
		return MELEE_CLEAVE + w.getCleave() + (hasSet(GearSet.VERAC) ? 2 : 0);
	}

	/** Ranged: extra enemies each missile passes through. */
	public int pierce()
	{
		Gear w = worn.get(Slot.WEAPON);
		if (w == null || w.getStyle() != Style.RANGED || w.getMissile() == Gear.Missile.CHINCHOMPA)
		{
			return 0;
		}
		return RANGED_PIERCE + (w.getTier() >= 5 ? 1 : 0);
	}

	/** Magic (and chinchompas): splash radius around each target, 0 for none. */
	public double splash()
	{
		Gear w = worn.get(Slot.WEAPON);
		if (w == null || w.getStyle() != Style.MAGIC && w.getSplash() == 0)
		{
			return 0;
		}
		double base = w.getStyle() == Style.MAGIC ? MAGIC_SPLASH : 0;
		return base + w.getSplash() + (w.getStyle() == Style.MAGIC && hasSet(GearSet.AHRIM) ? 16 : 0);
	}

	/** Missiles per attack. */
	public int volley()
	{
		Gear w = worn.get(Slot.WEAPON);
		return w == null ? 1 : w.getVolley();
	}

	/** Chance an attack goes off twice (Karil's set). */
	public double doubleAttackChance()
	{
		return getStyle() == Style.RANGED && hasSet(GearSet.KARIL) ? 0.25 : 0;
	}

	/** Whether damage grows as hitpoints fall (Dharok's set with a melee weapon). */
	public boolean berserk()
	{
		return getStyle() == Style.MELEE && worn.get(Slot.WEAPON) != null && hasSet(GearSet.DHAROK);
	}

	/** Share of damage dealt that heals the player. */
	public double lifesteal()
	{
		double l = 0;
		for (Gear g : worn.values())
		{
			l += g.getLifesteal();
		}
		if (getStyle() == Style.MELEE && hasSet(GearSet.GUTHAN))
		{
			l += 0.15;
		}
		return l;
	}

	/** Total damage multiplier from prayers, potions, matching gear and set bonuses. */
	public double damageMultiplier()
	{
		Style style = getStyle();
		double bonus = 0;
		for (Gear g : worn.values())
		{
			if (g.getSlot() != Slot.WEAPON && (g.getStyle() == null || g.getStyle() == style))
			{
				bonus += g.getDamageBonus();
			}
		}
		switch (style)
		{
			case MELEE:
				bonus += 0.05 * rank(Passive.MELEE_PRAYER) + 0.04 * rank(Passive.SUPER_COMBAT) + Boost.DAMAGE * doses(Boost.DIVINE_SUPER_COMBAT);
				bonus += hasSet(GearSet.INQUISITOR) ? 0.15 : 0;
				if (hasSet(GearSet.OBSIDIAN))
				{
					bonus += worn.get(Slot.WEAPON) == Gear.TZHAAR_KET_OM ? 0.3 : 0.1;
				}
				break;
			case RANGED:
				bonus += 0.05 * rank(Passive.RANGED_PRAYER) + 0.04 * rank(Passive.RANGING_POTION) + Boost.DAMAGE * doses(Boost.DIVINE_RANGING);
				if (hasSet(GearSet.CRYSTAL))
				{
					bonus += worn.get(Slot.WEAPON) == Gear.BOW_OF_FAERDHINEN ? 0.4 : 0.1;
				}
				break;
			default:
				bonus += 0.05 * rank(Passive.MAGIC_PRAYER) + 0.04 * rank(Passive.IMBUED_HEART) + Boost.DAMAGE * doses(Boost.DIVINE_MAGIC);
				bonus += hasSet(GearSet.AHRIM) ? 0.2 : 0;
				bonus += hasSet(GearSet.VIRTUS) ? 0.12 : 0;
				break;
		}
		bonus += hasSet(GearSet.VOID) ? 0.2 : 0;
		bonus += 0.04 * rank(Passive.OVERLOAD) + 0.04 * perk(Perk.MIGHT) + (pet(Pet.PET_CHAOS_ELEMENTAL) ? 0.05 : 0)
			+ (pet(Pet.TEKTINY) ? 0.03 : 0) + (pet(Pet.MIMI) ? 0.03 : 0);
		return 1 + bonus;
	}

	/**
	 * Seconds each hit freezes its target: the weapon's own, Virtus robes with magic, or the
	 * stagger every melee hit (fists included) gives, which holds the crowd off between swings.
	 */
	public double freeze()
	{
		Gear w = worn.get(Slot.WEAPON);
		double f = w == null ? 0 : w.getFreeze();
		if (getStyle() == Style.MELEE)
		{
			f = Math.max(f, MELEE_STAGGER);
		}
		return getStyle() == Style.MAGIC && hasSet(GearSet.VIRTUS) ? Math.max(f, 0.6) : f;
	}

	/** Damage multiplier against champions (Slayer). */
	public double championMultiplier()
	{
		return 1 + 0.2 * rank(Passive.SLAYER) + (pet(Pet.ORION) ? 0.1 : 0);
	}

	/** Chance a kill drops a second gem (Thieving). */
	public double extraGemChance()
	{
		return 0.1 * rank(Passive.THIEVING);
	}

	/** Coins multiplier for a finished run. */
	public double coinMultiplier()
	{
		return 1 + 0.1 * perk(Perk.GREED) + (pet(Pet.OLMLET) ? 0.1 : 0) + (pet(Pet.XARI) ? 0.05 : 0);
	}

	public double defence()
	{
		double d = (hasSet(GearSet.TORAG) ? 40 : 0) + (hasSet(GearSet.OBSIDIAN) ? 20 : 0);
		d += 6 * perk(Perk.ARMOUR) + (pet(Pet.PRINCE_BLACK_DRAGON) ? 12 : 0) + (pet(Pet.VANGUARD) ? 8 : 0) + (pet(Pet.DORK) ? 6 : 0);
		for (Gear g : worn.values())
		{
			d += g.getDefence();
		}
		return d;
	}

	/** Fraction of incoming damage prevented, from armour and protection prayers. */
	public double damageReduction()
	{
		double d = defence();
		double fromArmour = d / (d + DEFENCE_SCALE);
		return Math.min(MAX_REDUCTION, fromArmour + 0.05 * rank(Passive.PROTECTION) + (getStyle() == Style.MELEE ? MELEE_GUARD : 0)
			+ Boost.REDUCTION * doses(Boost.ANTIFIRE));
	}

	/** Extra reduction against monsters touching the player (Protect from Melee). */
	public double contactReduction()
	{
		return rank(Passive.PROTECT_MELEE) > 0 ? 0.2 : 0;
	}

	/** Extra reduction against arrows and spells (Protect from Missiles). */
	public double missileReduction()
	{
		return rank(Passive.PROTECT_MISSILES) > 0 ? 0.5 : 0;
	}

	public double maxHealth()
	{
		return Player.BASE_MAX_HEALTH + 20 * rank(Passive.HITPOINTS) + 10 * perk(Perk.VITALITY) + (pet(Pet.BABI) ? 10 : 0)
			+ Boost.HITPOINTS * doses(Boost.SARADOMIN_BREW);
	}

	public double speed()
	{
		double bonus = 0.08 * rank(Passive.AGILITY) + 0.04 * perk(Perk.SPEED) + (pet(Pet.BABY_MOLE) ? 0.06 : 0) + (pet(Pet.VESPINA) ? 0.04 : 0)
			+ Boost.SPEED * doses(Boost.STAMINA);
		for (Gear g : worn.values())
		{
			bonus += g.getSpeedBonus();
		}
		return Player.BASE_SPEED * (1 + bonus);
	}

	public double magnetRadius()
	{
		double r = Player.BASE_MAGNET_RADIUS + 40 * rank(Passive.TELEKINETIC_GRAB) + 20 * perk(Perk.MAGNET) + (pet(Pet.VORKI) ? 40 : 0) + (pet(Pet.KEPHRITI) ? 20 : 0);
		for (Gear g : worn.values())
		{
			r += g.getMagnet();
		}
		return r;
	}

	public double xpMultiplier()
	{
		return 1 + 0.2 * rank(Passive.LAMP) + 0.06 * perk(Perk.WISDOM) + (pet(Pet.LIL_ZIK) ? 0.08 : 0) + (pet(Pet.TUMEKENS_GUARDIAN) ? 0.05 : 0);
	}

	public double regenPerSecond()
	{
		return Player.BASE_REGEN + 0.6 * rank(Passive.RAPID_HEAL) + (hasSet(GearSet.TORAG) ? 0.5 : 0)
			+ 0.15 * perk(Perk.RECOVERY) + (pet(Pet.PET_DARK_CORE) ? 0.3 : 0) + (pet(Pet.VASA_MINIRIO) ? 0.2 : 0)
			+ Boost.REGEN * doses(Boost.SUPER_RESTORE);
	}

	/** Hitpoints healed per kill. */
	public double healPerKill()
	{
		return rank(Passive.BONECRUSHER) + (pet(Pet.CHUBBLES) ? 1 : 0);
	}

	/** Damage per second dealt to each enemy touching the player. */
	public double thornsDps()
	{
		double dps = 15 * rank(Passive.VENGEANCE);
		for (Gear g : worn.values())
		{
			dps += g.getReflectDps();
		}
		return dps;
	}

	/** Seconds between special attacks, or 0 without the passive. */
	public double specialInterval()
	{
		int r = rank(Passive.SPECIAL_ATTACK);
		return r == 0 ? 0 : 12 - 2 * (r - 1);
	}
}
