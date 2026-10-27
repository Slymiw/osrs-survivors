package com.osrssurvivors.game;

import static com.osrssurvivors.game.ProgressionTest.levelledUp;
import static com.osrssurvivors.game.TestSessions.EPS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Random;
import org.junit.Test;

/** The bag: gear for other styles kept to switch to, potions drunk for a while, food eaten. */
public class InventoryTest
{
	private static GameSession session()
	{
		GameSession s = new GameSession(new Random(1));
		s.disableSpawning();
		return s;
	}

	private static void run(GameSession s, double seconds)
	{
		for (int i = 0; i < Math.round(seconds / GameSession.STEP); i++)
		{
			s.advance(GameSession.STEP);
		}
	}

	private static int slotOf(GameSession s, Gear gear)
	{
		for (int i = 0; i < Inventory.SIZE; i++)
		{
			Item item = s.getInventory().get(i);
			if (item != null && item.getGear() == gear)
			{
				return i;
			}
		}
		return -1;
	}

	@Test
	public void oneClickOnAWeaponSwitchesStyleWithItsGear()
	{
		GameSession s = session();
		s.grant(Offer.gear(Gear.RUNE_SCIMITAR));
		s.grant(Offer.gear(Gear.RUNE_FULL_HELM));
		s.grant(Offer.gear(Gear.RUNE_DEFENDER));
		s.grant(Offer.gear(Gear.FIRE_CAPE));
		s.grant(Offer.stored(Gear.MAGIC_SHORTBOW));
		s.grant(Offer.stored(Gear.COIF));
		Build b = s.getBuild();
		assertEquals("stored gear isn't worn", Gear.RUNE_SCIMITAR, b.get(Slot.WEAPON));
		assertEquals(Style.MELEE, b.getStyle());
		assertTrue(s.carriedStyles().contains(Style.RANGED));

		assertTrue(s.useItem(slotOf(s, Gear.MAGIC_SHORTBOW)));
		assertEquals(Gear.MAGIC_SHORTBOW, b.get(Slot.WEAPON));
		assertEquals("the style's helm comes on with it", Gear.COIF, b.get(Slot.HEAD));
		assertEquals("any-style pieces stay on", Gear.FIRE_CAPE, b.get(Slot.CAPE));
		assertNull("the bow is two-handed", b.get(Slot.SHIELD));
		for (Gear off : new Gear[]{Gear.RUNE_SCIMITAR, Gear.RUNE_FULL_HELM, Gear.RUNE_DEFENDER})
		{
			assertTrue(off + " went in the bag", slotOf(s, off) >= 0);
		}

		// and back again, shield included
		assertTrue(s.useItem(slotOf(s, Gear.RUNE_SCIMITAR)));
		assertEquals(Gear.RUNE_FULL_HELM, b.get(Slot.HEAD));
		assertEquals(Gear.RUNE_DEFENDER, b.get(Slot.SHIELD));
		assertTrue(slotOf(s, Gear.MAGIC_SHORTBOW) >= 0);
		assertTrue(slotOf(s, Gear.COIF) >= 0);
	}

	@Test
	public void upgradesReplaceWeakerPiecesInTheBag()
	{
		GameSession s = session();
		s.grant(Offer.gear(Gear.RUNE_SCIMITAR));
		s.grant(Offer.stored(Gear.YEW_SHORTBOW));
		s.grant(Offer.stored(Gear.MAGIC_SHORTBOW));
		assertEquals(-1, slotOf(s, Gear.YEW_SHORTBOW));
		assertTrue(slotOf(s, Gear.MAGIC_SHORTBOW) >= 0);
		// a plain upgrade of the worn piece isn't kept, but the replaced other-style piece is
		s.grant(Offer.gear(Gear.DRAGON_SCIMITAR));
		assertEquals(-1, slotOf(s, Gear.RUNE_SCIMITAR));
		assertEquals(27, s.getInventory().freeSlots());
	}

	@Test
	public void potionsWorkForAMinuteADose()
	{
		GameSession s = session();
		s.grant(Offer.gear(Gear.RUNE_SCIMITAR));
		s.grant(Offer.boost(Boost.DIVINE_SUPER_COMBAT));
		Build b = s.getBuild();
		double plain = b.damageMultiplier();
		assertEquals(Item.potion(Boost.DIVINE_SUPER_COMBAT, 4), s.getInventory().get(0));

		assertTrue(s.useItem(0));
		assertEquals(plain + Boost.DAMAGE, b.damageMultiplier(), EPS);
		assertEquals(3, s.getInventory().get(0).getDoses());
		assertFalse("one drink at a time", s.useItem(0));

		run(s, Boost.DOSE_SECONDS - 1);
		assertTrue(b.has(Boost.DIVINE_SUPER_COMBAT));
		run(s, 2);
		assertFalse("worn off", b.has(Boost.DIVINE_SUPER_COMBAT));
		assertEquals(plain, b.damageMultiplier(), EPS);

		for (int dose = 3; dose >= 1; dose--)
		{
			assertTrue(s.useItem(0));
			run(s, GameSession.DRINK_DELAY + 0.05);
		}
		assertNull("the empty vial is gone", s.getInventory().get(0));
	}

	@Test
	public void brewRaisesHitpointsWhileItWorks()
	{
		GameSession s = session();
		s.grant(Offer.boost(Boost.SARADOMIN_BREW));
		Player p = s.getPlayer();
		p.health = 50;
		assertTrue(s.useItem(0));
		assertEquals(Player.BASE_MAX_HEALTH * (1 + Boost.HITPOINTS), p.getMaxHealth(), EPS);
		assertEquals(50 + p.getMaxHealth() * Boost.HITPOINTS, p.getHealth(), 0.5);
		run(s, Boost.DOSE_SECONDS + 1);
		assertEquals(Player.BASE_MAX_HEALTH, p.getMaxHealth(), EPS);
	}

	@Test
	public void foodTakesThreeTicksButKarambwansCombo()
	{
		GameSession s = session();
		s.grant(Offer.food(Food.SHARK, 2));
		s.grant(Offer.food(Food.COOKED_KARAMBWAN, 1));
		s.getPlayer().health = 10;
		assertTrue(s.useItem(0));
		assertFalse("still chewing", s.useItem(1));
		assertTrue("a karambwan goes down straight after", s.useItem(2));
		run(s, Food.EAT_DELAY + 0.05);
		assertTrue(s.useItem(1));
		assertEquals(28, s.getInventory().freeSlots());
	}

	@Test
	public void nothingIsOfferedThatTheBagCantHold()
	{
		GameSession s = session();
		s.grant(Offer.gear(Gear.RUNE_SCIMITAR));
		for (int i = 0; i < Inventory.SIZE; i++)
		{
			s.grant(Offer.food(Food.SHARK, 1));
		}
		assertEquals(0, s.getInventory().freeSlots());
		Random seeds = new Random(4);
		for (int i = 0; i < 40; i++)
		{
			levelledUp(s, 1);
			for (Offer o : s.getChoices())
			{
				assertFalse("no room for " + o, o.isStored() || o.getKind() == Offer.Kind.BOOST || o.getKind() == Offer.Kind.FOOD);
			}
			s.chooseUpgrade((int) (seeds.nextDouble() * s.getChoices().size()));
		}
	}

	@Test
	public void aFullBagRefusesASwapThatFreesTwoPieces()
	{
		GameSession s = session();
		s.grant(Offer.gear(Gear.RUNE_SCIMITAR));
		s.grant(Offer.gear(Gear.RUNE_DEFENDER));
		s.grant(Offer.stored(Gear.MAGIC_SHORTBOW));
		for (int i = 1; i < Inventory.SIZE; i++)
		{
			s.grant(Offer.food(Food.SHARK, 1));
		}
		assertEquals(0, s.getInventory().freeSlots());
		int bow = slotOf(s, Gear.MAGIC_SHORTBOW);
		assertFalse("the scimitar and defender need two slots", s.useItem(bow));
		assertNotNull(s.getNotice());
		assertEquals(Gear.RUNE_SCIMITAR, s.getBuild().get(Slot.WEAPON));
		s.dropItem(5);
		assertTrue(s.useItem(bow));
		assertEquals(Gear.MAGIC_SHORTBOW, s.getBuild().get(Slot.WEAPON));
	}

	@Test
	public void itemsMoveBetweenSlots()
	{
		GameSession s = session();
		s.grant(Offer.food(Food.SHARK, 1));
		assertTrue(s.moveItem(0, 27));
		assertNull(s.getInventory().get(0));
		assertEquals(Item.food(Food.SHARK), s.getInventory().get(27));
		assertTrue(s.dropItem(27));
		assertEquals(Inventory.SIZE, s.getInventory().freeSlots());
	}

	@Test
	public void theBagAndPotionsAreSaved()
	{
		GameSession s = session();
		s.grant(Offer.gear(Gear.RUNE_SCIMITAR));
		s.grant(Offer.stored(Gear.MAGIC_SHORTBOW));
		s.grant(Offer.boost(Boost.STAMINA));
		s.grant(Offer.food(Food.ANGLERFISH, 1));
		s.moveItem(2, 10);
		s.useItem(1);
		run(s, 20);
		RunSave save = s.save();

		GameSession loaded = session();
		loaded.load(save);
		for (int i = 0; i < Inventory.SIZE; i++)
		{
			assertEquals("slot " + i, s.getInventory().get(i), loaded.getInventory().get(i));
		}
		assertEquals(Item.potion(Boost.STAMINA, 3), loaded.getInventory().get(1));
		assertEquals(Boost.DOSE_SECONDS - 20, loaded.getBuild().getPotions().get(Boost.STAMINA), 0.1);
		assertEquals(s.getPlayer().getSpeed(), loaded.getPlayer().getSpeed(), EPS);
	}

	@Test
	public void potionsThatLastedTheRunComeBackAsPotions()
	{
		RunSave old = new RunSave();
		old.version = 3;
		old.location = Location.LUMBRIDGE.name();
		old.level = 40;
		old.boosts.put(Boost.DIVINE_RANGING.name(), 6);
		old.boosts.put("SOMETHING_GONE", 2);
		GameSession s = session();
		s.load(old);
		assertEquals(Item.potion(Boost.DIVINE_RANGING, 4), s.getInventory().get(0));
		assertEquals(Item.potion(Boost.DIVINE_RANGING, 2), s.getInventory().get(1));
		assertNull(s.getInventory().get(2));
		assertTrue(s.getBuild().getPotions().isEmpty());
	}

	@Test
	public void drainingHealsOnlySoFast()
	{
		GameSession s = session();
		s.grant(Offer.gear(Gear.SANGUINE_SCYTHE_OF_VITUR));
		Player p = s.getPlayer();
		// a wall of tough monsters in reach but not touching; the scythe hits three at a time
		for (int i = 0; i < 12; i++)
		{
			Enemy e = s.spawnEnemyAt(p.x + 36 + (i % 3) * 6, p.y - 15 + (i / 3) * 10, 1e9);
			e.frozen = 1e9;
		}
		p.health = 1;
		double seconds = 10;
		run(s, seconds);
		double healed = p.getHealth() - 1 - s.getBuild().regenPerSecond() * seconds;
		double refill = p.getMaxHealth() * GameSession.DRAIN_RATE * seconds;
		assertTrue("healed " + healed + ", at most " + refill, healed <= refill + EPS);
		assertTrue("but it does heal: " + healed, healed > refill * 0.8);
	}
}
