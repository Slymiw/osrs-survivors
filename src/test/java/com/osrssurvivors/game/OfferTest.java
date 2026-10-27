package com.osrssurvivors.game;

import static com.osrssurvivors.game.ProgressionTest.levelledUp;
import static com.osrssurvivors.game.TestSessions.EPS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

/** Gear chains, build numbers and which offers appear on level-up. */
public class OfferTest
{
	private static GameSession session(int seed)
	{
		GameSession s = new GameSession(new Random(seed));
		s.disableSpawning();
		return s;
	}

	@Test
	public void everyChainClimbsByOneTier()
	{
		for (Gear g : Gear.values())
		{
			List<Gear> chain = Gear.chain(g.getSlot(), g.getStyle());
			if (g.isUnique())
			{
				// combination weapons and boss drops never come up as plain upgrades
				assertFalse(g + " stays out of its chain", chain.contains(g));
				assertFalse(g + " is never a plain offer", Gear.at(g.getSlot(), g.getStyle(), g.getTier()).contains(g));
				continue;
			}
			// each tier of a chain offers its plain item and any sidegrades (sets, other weapons)
			if (g.getTier() > 0)
			{
				assertTrue(g + " is offered at its tier", Gear.at(g.getSlot(), g.getStyle(), g.getTier()).contains(g));
				assertTrue(g + " has a chain to sit in", chain.size() >= g.getTier());
			}
			for (int i = 0; i < chain.size(); i++)
			{
				assertEquals(i + 1, chain.get(i).getTier());
			}
		}
		assertEquals(10, Gear.chain(Slot.WEAPON, Style.MELEE).size());
		assertEquals(Gear.DRAGON_SCIMITAR, Gear.RUNE_SCIMITAR.next());
		assertNull(Gear.ASCENSION_CROSSBOWS.next());
		assertEquals("the twisted bow is a megarare, not the top of the chain", Gear.ASCENSION_CROSSBOWS,
			Gear.chain(Slot.WEAPON, Style.RANGED).get(9));
		assertEquals(Gear.RED_CAPE, Gear.first(Slot.CAPE, Style.MAGIC));
		// books and wards for ranged; most ranged weapons are two-handed and can't use them
		assertEquals(Gear.ODIUM_WARD, Gear.chain(Slot.SHIELD, Style.RANGED).get(2));
	}

	@Test
	public void weaponsHaveSensibleNumbers()
	{
		for (Gear g : Gear.values())
		{
			if (g.getSlot() == Slot.WEAPON)
			{
				assertTrue(g + " damage", g.getDamage() > Build.FIST_DAMAGE);
				assertTrue(g + " cooldown", g.getCooldown() > 0);
				assertTrue(g + " range", g.getRange() >= Build.FIST_RANGE);
				assertFalse(g.describe().isEmpty());
			}
		}
		assertTrue(Gear.SHORTBOW.getRange() > Gear.BRONZE_SCIMITAR.getRange() * 3);
	}

	@Test
	public void bareHandsAreOfferedAWeapon()
	{
		for (int seed = 0; seed < 30; seed++)
		{
			GameSession s = levelledUp(session(seed), 1);
			boolean weapon = false;
			for (Offer o : s.getChoices())
			{
				weapon |= o.getKind() == Offer.Kind.GEAR && o.getGear().getSlot() == Slot.WEAPON;
			}
			assertTrue("seed " + seed, weapon);
		}
	}

	@Test
	public void gearOffersAreTheNextTier()
	{
		for (int seed = 0; seed < 40; seed++)
		{
			GameSession s = session(seed);
			s.grant(Offer.gear(Gear.STEEL_SCIMITAR));
			s.grant(Offer.gear(Gear.IRON_PLATEBODY));
			levelledUp(s, 1);
			for (Offer o : s.getChoices())
			{
				if (o.getKind() != Offer.Kind.GEAR)
				{
					continue;
				}
				Gear g = o.getGear();
				if (g.getSlot() == Slot.WEAPON && g.getStyle() == Style.MELEE)
				{
					assertEquals(Gear.MITHRIL_SCIMITAR, g);
				}
				if (g.getSlot() == Slot.BODY)
				{
					assertEquals(Gear.STEEL_PLATEBODY, g);
				}
				if (g.getSlot() == Slot.HEAD)
				{
					assertEquals("armour follows the weapon's style", Gear.BRONZE_FULL_HELM, g);
				}
			}
		}
	}

	@Test
	public void passivesFollowTheWeaponStyleAndCaps()
	{
		Set<Passive> offered = EnumSet.noneOf(Passive.class);
		for (int seed = 0; seed < 200; seed++)
		{
			GameSession s = session(seed);
			s.grant(Offer.gear(Gear.STAFF_OF_AIR));
			for (int r = 0; r < Passive.HITPOINTS.getMaxRank(); r++)
			{
				s.grant(Offer.passive(Passive.HITPOINTS, r + 1));
			}
			levelledUp(s, 1);
			for (Offer o : s.getChoices())
			{
				if (o.getKind() == Offer.Kind.PASSIVE)
				{
					offered.add(o.getPassive());
				}
			}
		}
		assertTrue(offered.contains(Passive.MAGIC_PRAYER));
		assertFalse("not your style", offered.contains(Passive.MELEE_PRAYER));
		assertFalse("not your style", offered.contains(Passive.RANGED_PRAYER));
		assertFalse("maxed out", offered.contains(Passive.HITPOINTS));
	}

	@Test
	public void foodIsOfferedWhenHurt()
	{
		boolean food = false;
		for (int seed = 0; seed < 30 && !food; seed++)
		{
			GameSession s = session(seed);
			s.getPlayer().health = 20;
			levelledUp(s, 1);
			for (Offer o : s.getChoices())
			{
				food |= o.getKind() == Offer.Kind.FOOD;
			}
		}
		assertTrue(food);

		GameSession healthy = levelledUp(session(1), 1);
		for (Offer o : healthy.getChoices())
		{
			assertFalse(o.getKind() == Offer.Kind.FOOD);
		}
	}

	@Test
	public void eatingHeals()
	{
		GameSession s = session(1);
		s.getPlayer().health = 10;
		s.grant(Offer.food(Food.SHARK, 1));
		assertEquals("food goes in the bag", Item.food(Food.SHARK), s.getInventory().get(0));
		assertEquals(10, s.getPlayer().health, EPS);
		assertTrue(s.useItem(0));
		assertEquals(10 + Player.BASE_MAX_HEALTH * Food.SHARK.getHeal(), s.getPlayer().health, EPS);
		assertNull("and is eaten", s.getInventory().get(0));
	}

	@Test
	public void twoHandedWeaponsRemoveTheShield()
	{
		Build b = new Build();
		b.equip(Gear.RUNE_SCIMITAR);
		b.equip(Gear.RUNE_DEFENDER);
		assertNotNull(b.get(Slot.SHIELD));
		b.equip(Gear.SCYTHE_OF_VITUR);
		assertNull(b.get(Slot.SHIELD));

		for (int seed = 0; seed < 40; seed++)
		{
			GameSession s = session(seed);
			s.grant(Offer.gear(Gear.YEW_SHORTBOW));
			levelledUp(s, 1);
			for (Offer o : s.getChoices())
			{
				assertFalse(o.getKind() == Offer.Kind.GEAR && o.getGear().getSlot() == Slot.SHIELD);
			}
		}
	}

	@Test
	public void buildNumbers()
	{
		Build b = new Build();
		assertEquals(Style.MELEE, b.getStyle());
		assertEquals(1, b.damageMultiplier(), EPS);
		// fists count as melee, which guards a little
		assertEquals(Build.MELEE_GUARD, b.damageReduction(), EPS);

		b.equip(Gear.MAGIC_SHORTBOW);
		b.equip(Gear.BLACK_DHIDE_BODY);
		b.equip(Gear.RUNE_FULL_HELM);
		b.addRank(Passive.RANGED_PRAYER);
		b.addRank(Passive.MELEE_PRAYER);
		assertEquals(Style.RANGED, b.getStyle());
		// ranged body and prayer count; the melee prayer and helm give no ranged damage
		assertEquals(1 + Gear.BLACK_DHIDE_BODY.getDamageBonus() + 0.05, b.damageMultiplier(), EPS);
		assertTrue(b.damageReduction() > 0);

		b.addRank(Passive.SWIFTNESS);
		assertEquals(Gear.MAGIC_SHORTBOW.getCooldown() * 0.92, b.attackCooldown(), EPS);
		b.addRank(Passive.LONGRANGE);
		assertEquals(Gear.MAGIC_SHORTBOW.getRange() * 1.12, b.attackRange(), EPS);
		b.addRank(Passive.MULTICOMBAT);
		assertEquals(2, b.targets());

		b.equip(Gear.PRIMORDIAL_BOOTS);
		b.addRank(Passive.AGILITY);
		assertEquals(Player.BASE_SPEED * (1 + 0.08 + Gear.PRIMORDIAL_BOOTS.getSpeedBonus()), b.speed(), EPS);
		b.equip(Gear.RING_OF_WEALTH);
		b.addRank(Passive.TELEKINETIC_GRAB);
		assertEquals(Player.BASE_MAGNET_RADIUS + 40 + 60, b.magnetRadius(), EPS);
	}

	@Test
	public void protectionIsCapped()
	{
		Build b = new Build();
		for (Gear g : new Gear[]{Gear.TORVA_FULL_HELM, Gear.TORVA_PLATEBODY, Gear.TORVA_PLATELEGS, Gear.AVERNIC_DEFENDER})
		{
			b.equip(g);
		}
		for (int i = 0; i < Passive.PROTECTION.getMaxRank(); i++)
		{
			b.addRank(Passive.PROTECTION);
		}
		assertTrue(b.damageReduction() <= Build.MAX_REDUCTION);
		assertTrue(b.damageReduction() > 0.5);
	}

	@Test
	public void gearMakesTheWeaponStrongerInPlay()
	{
		GameSession s = session(1);
		double fists = s.getWeapon().getMaxHit();
		s.grant(Offer.gear(Gear.DRAGON_SCIMITAR));
		s.grant(Offer.gear(Gear.INFERNAL_CAPE));
		s.grant(Offer.passive(Passive.MELEE_PRAYER, 1));
		assertEquals(Gear.DRAGON_SCIMITAR.getDamage() * (1 + 0.10 + 0.05), s.getWeapon().getMaxHit(), EPS);
		assertTrue(s.getWeapon().getMaxHit() > fists * 5);
	}

	@Test
	public void rankTitlesFollowThePrayerChain()
	{
		assertEquals("Burst of Strength", Passive.MELEE_PRAYER.title(1));
		assertEquals("Piety", Passive.MELEE_PRAYER.title(5));
		assertEquals("Agility III", Passive.AGILITY.title(3));
		assertEquals("Redemption", Passive.REDEMPTION.title(1));
		assertEquals("Piety", Offer.passive(Passive.MELEE_PRAYER, 5).getTitle());
	}

	@Test
	public void aCombinedWeaponIsNeverSwappedForItsBase()
	{
		Random seeds = new Random(5);
		for (int i = 0; i < 60; i++)
		{
			GameSession s = new GameSession(new Random(seeds.nextLong()));
			s.disableSpawning();
			s.grant(Offer.gear(Gear.SANGUINE_SCYTHE_OF_VITUR));
			levelledUp(s, 1);
			for (Offer o : s.getChoices())
			{
				assertFalse("offered " + o, o.getGear() == Gear.SCYTHE_OF_VITUR);
			}
		}
	}

	@Test
	public void fillersWhenUpgradesRunOut()
	{
		GameSession s = session(3);
		// everything maxed: no upgrades left at all
		for (Passive p : Passive.values())
		{
			for (int r = 0; r < p.getMaxRank(); r++)
			{
				s.grant(Offer.passive(p, r + 1));
			}
		}
		for (Slot slot : Slot.values())
		{
			List<Gear> chain = Gear.chain(slot, Style.MELEE);
			if (!chain.isEmpty())
			{
				s.grant(Offer.gear(chain.get(chain.size() - 1)));
			}
		}
		// already combined
		s.grant(Offer.gear(Gear.SANGUINE_SCYTHE_OF_VITUR));
		levelledUp(s, 1);
		// with nothing left to upgrade for melee: fillers (and now and then another style's gear for
		// the bag), which change from level to level
		java.util.Set<String> seen = new java.util.HashSet<>();
		boolean paid = false;
		for (int level = 0; level < 12; level++)
		{
			List<Offer> choices = s.getChoices();
			assertEquals("always a full choice", GameSession.CHOICE_COUNT, choices.size());
			assertEquals("no repeats on one screen", choices.size(), new java.util.HashSet<>(choices).size());
			Offer pick = null;
			for (Offer o : choices)
			{
				assertTrue(o.getKind() == Offer.Kind.BOOST || o.getKind() == Offer.Kind.COINS || o.getKind() == Offer.Kind.FOOD
					|| o.isStored() && o.getGear().getStyle() != Style.MELEE);
				assertTrue("melee only gets the melee potion", o.getBoost() == null || o.getBoost().getStyle() == null
					|| o.getBoost().getStyle() == Style.MELEE);
				seen.add(o.getBoost() != null ? o.getBoost().name() : o.getKind().name());
				pick = pick == null && !o.isStored() ? o : pick;
			}
			int coins = s.coinsSoFar();
			s.chooseUpgrade(choices.indexOf(pick));
			if (pick.getKind() == Offer.Kind.COINS)
			{
				assertTrue(s.coinsSoFar() >= coins + pick.getCoins());
				paid = true;
			}
			if (pick.getBoost() != null)
			{
				assertEquals("a full potion in the bag", Item.potion(pick.getBoost(), Boost.DOSES), s.getInventory().get(0));
			}
			// a player drinks and eats them, so the bag never fills up here
			for (int i = 0; i < Inventory.SIZE; i++)
			{
				s.dropItem(i);
			}
			levelledUp(s, 1);
		}
		assertTrue("a varied mix, not the same three: " + seen, seen.size() >= 5);
		// back to playing: bag items are used during play, not over the level-up screen
		s.chooseUpgrade(0);
		assertEquals(Phase.RUNNING, s.getPhase());
		s.grant(Offer.boost(Boost.DIVINE_SUPER_COMBAT));
		double before = s.getBuild().damageMultiplier();
		int slot = -1;
		for (int i = 0; i < Inventory.SIZE && slot < 0; i++)
		{
			slot = s.getInventory().get(i) != null && s.getInventory().get(i).getPotion() == Boost.DIVINE_SUPER_COMBAT ? i : -1;
		}
		assertTrue(s.useItem(slot));
		assertEquals("a dose of the style's potion adds damage", before + Boost.DAMAGE, s.getBuild().damageMultiplier(), 1e-9);
	}

	@Test
	public void otherStylesWeaponsGoInTheBag()
	{
		Random seeds = new Random(9);
		boolean offered = false;
		for (int i = 0; i < 80; i++)
		{
			GameSession s = new GameSession(new Random(seeds.nextLong()));
			s.disableSpawning();
			s.grant(Offer.gear(Gear.RUNE_SCIMITAR));
			levelledUp(s, 1);
			for (Offer o : s.getChoices())
			{
				if (o.getGear() != null && o.getGear().getStyle() != null && o.getGear().getStyle() != Style.MELEE)
				{
					offered = true;
					assertTrue("offered for the bag: " + o, o.isStored());
				}
			}
		}
		assertTrue("other styles' weapons do come up", offered);
	}
}
