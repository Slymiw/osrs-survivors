package com.osrssurvivors.game;

import static com.osrssurvivors.game.ProgressionTest.levelledUp;
import static com.osrssurvivors.game.TestSessions.quiet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

/** Champion caskets and their slot-machine drops, combinations and the ability cap. */
public class DropTest
{
	@Test
	public void casketSpinsBeforeItsChoices()
	{
		GameSession s = Scenarios.bossDrop(new Random(5));
		BossDrop drop = s.getBossDrop();
		assertNotNull(drop);
		assertTrue(drop.isSpinning());
		double elapsed = s.getElapsed();
		Scenarios.spin(s, 1);
		assertEquals("the run waits while the reels spin", elapsed, s.getElapsed(), 0);

		// the first claim only stops the reels
		assertFalse(s.claimDrop());
		assertFalse(drop.isSpinning());
		assertEquals(Phase.BOSS_DROP, s.getPhase());
		Scenarios.spin(s, BossDrop.CLAIMABLE);
		assertTrue(s.claimDrop());
		assertNull(s.getBossDrop());
		assertEquals("then the casket's gear", Phase.LEVEL_UP, s.getPhase());
		assertTrue(s.isCasketOpen());
	}

	@Test
	public void reelsLandOnThePrize()
	{
		BossDrop drop = BossDrop.roll(new Random(1), new ArrayList<>(), BossDrop.ITEMS, new ArrayList<>(), 3);
		for (int i = 0; i < drop.reelCount(); i++)
		{
			assertEquals(0, drop.reelPosition(i), 1e-9);
		}
		drop.advance(BossDrop.STOPS[0] + 0.01);
		assertTrue(drop.isStopped(0));
		assertFalse(drop.isStopped(1));
		drop.advance(10);
		for (int i = 0; i < drop.reelCount(); i++)
		{
			assertEquals(BossDrop.STRIP - 1, drop.reelPosition(i), 1e-9);
			assertTrue(drop.reel(i).get(BossDrop.STRIP - 1) == drop.getPrize());
		}
	}

	@Test
	public void raritiesFollowTheOdds()
	{
		Random random = new Random(9);
		List<Pet> pets = new ArrayList<>();
		Collections.addAll(pets, Pet.values());
		Map<BossDrop.Rarity, Integer> counts = new EnumMap<>(BossDrop.Rarity.class);
		int n = 20_000;
		for (int i = 0; i < n; i++)
		{
			counts.merge(BossDrop.roll(random, BossDrop.MEGARARES, BossDrop.ITEMS, pets, 2).getRarity(), 1, Integer::sum);
		}
		assertEquals(BossDrop.JACKPOT_CHANCE, counts.get(BossDrop.Rarity.JACKPOT) / (double) n, 0.01);
		assertEquals(BossDrop.RARE_CHANCE, counts.get(BossDrop.Rarity.RARE) / (double) n, 0.015);
		assertEquals(BossDrop.MEGARARE_CHANCE, counts.get(BossDrop.Rarity.MEGARARE) / (double) n, 0.004);

		// nothing left to win: the roll falls back to lamps and coins
		for (int i = 0; i < 200; i++)
		{
			BossDrop d = BossDrop.roll(random, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 2);
			assertTrue(d.getRarity() == BossDrop.Rarity.UNCOMMON || d.getRarity() == BossDrop.Rarity.COMMON);
		}
	}

	@Test
	public void wonPetsAreKeptAndFollowYou()
	{
		MetaProgress meta = new MetaProgress();
		// seeds from one generator: neighbouring seeds start out nearly alike
		Random seeds = new Random(17);
		for (int attempt = 0; attempt < 400; attempt++)
		{
			GameSession s = new GameSession(new Random(seeds.nextLong()));
			s.disableSpawning();
			s.setMeta(meta);
			s.dropCasketAt(s.getPlayer().x, s.getPlayer().y);
			s.advance(GameSession.STEP);
			BossDrop drop = s.getBossDrop();
			if (drop.getPrize().getKind() != BossDrop.Prize.Kind.PET)
			{
				continue;
			}
			Pet pet = drop.getPrize().getPet();
			drop.advance(BossDrop.CLAIMABLE);
			assertTrue(s.claimDrop());
			assertTrue(meta.hasPet(pet));
			assertTrue(s.getBuild().getPets().contains(pet));
			// and in the next run too
			GameSession next = quiet();
			next.setMeta(meta);
			assertTrue(next.getBuild().getPets().contains(pet));
			return;
		}
		throw new AssertionError("no pet in 400 caskets");
	}

	@Test
	public void lampGivesLevels()
	{
		GameSession s = quiet();
		int level = s.getProgression().getLevel();
		for (int i = 0; i < 100; i++)
		{
			s.dropCasketAt(s.getPlayer().x, s.getPlayer().y);
			s.advance(GameSession.STEP);
			BossDrop drop = s.getBossDrop();
			drop.advance(BossDrop.CLAIMABLE);
			boolean lamp = drop.getPrize().getKind() == BossDrop.Prize.Kind.LAMP;
			s.claimDrop();
			if (lamp)
			{
				assertEquals(level + drop.getPrize().getAmount(), s.getProgression().getLevel());
				return;
			}
			// clear the casket's choices and any queued levels before the next try
			while (s.getPhase() == Phase.LEVEL_UP)
			{
				s.chooseUpgrade(0);
			}
			level = s.getProgression().getLevel();
		}
		throw new AssertionError("no lamp in 100 caskets");
	}

	@Test
	public void readyCombinationLeadsTheOffers()
	{
		GameSession s = quiet();
		s.grant(Offer.gear(Gear.ABYSSAL_WHIP));
		for (int i = 0; i < Passive.SWIFTNESS.getMaxRank() - 1; i++)
		{
			s.grant(Offer.passive(Passive.SWIFTNESS, i + 1));
		}
		levelledUp(s, 1);
		assertFalse(s.getChoices().contains(Offer.gear(Gear.ABYSSAL_TENTACLE)));
		s.chooseUpgrade(0);

		s.grant(Offer.passive(Passive.SWIFTNESS, Passive.SWIFTNESS.getMaxRank()));
		if (s.getBuild().get(Slot.WEAPON) != Gear.ABYSSAL_WHIP)
		{
			s.grant(Offer.gear(Gear.ABYSSAL_WHIP));
		}
		levelledUp(s, 1);
		assertEquals(Offer.gear(Gear.ABYSSAL_TENTACLE), s.getChoices().get(0));
		assertEquals(GameSession.CHOICE_COUNT, s.getChoices().size());
	}

	@Test
	public void everyCombinationIsAUniqueWeaponOfItsBasesStyle()
	{
		for (Combination c : Combination.values())
		{
			assertTrue(c.getResult().isUnique());
			assertEquals(Slot.WEAPON, c.getResult().getSlot());
			assertEquals(c.getBase().getStyle(), c.getResult().getStyle());
			assertEquals(c.getBase().getTier(), c.getResult().getTier());
			assertTrue(c + " hits harder", c.getResult().getDamage() / c.getResult().getCooldown()
				> c.getBase().getDamage() / c.getBase().getCooldown());
			assertEquals(c, Combination.making(c.getResult()));
		}
	}

	@Test
	public void abilitiesAreCappedAtFour()
	{
		GameSession s = quiet();
		Passive[] four = {Passive.CANNON, Passive.THRALLS, Passive.ICE_BARRAGE, Passive.CHINCHOMPAS};
		for (Passive p : four)
		{
			s.grant(Offer.passive(p, 1));
		}
		for (int i = 0; i < 30; i++)
		{
			levelledUp(s, 1);
			for (Offer o : s.getChoices())
			{
				if (o.getKind() == Offer.Kind.PASSIVE && o.getPassive().isAbility())
				{
					assertTrue(o + " is one of the four", o.getRank() > 1);
				}
			}
			s.chooseUpgrade(0);
			while (s.getPhase() == Phase.LEVEL_UP)
			{
				s.chooseUpgrade(0);
			}
		}
	}

	@Test
	public void bossItemsTopTheirSlots()
	{
		for (Gear g : BossDrop.ITEMS)
		{
			assertTrue(g.isUnique());
			for (Gear other : Gear.values())
			{
				if (!other.isUnique() && other.getSlot() == g.getSlot() && g.getSlot() != Slot.SHIELD)
				{
					assertTrue(g + " beats " + other, g.getDamageBonus() + g.getSpeedBonus() >= other.getDamageBonus() + other.getSpeedBonus());
				}
			}
		}
	}

	@Test
	public void megararesMatchTheStyleAndAreUnique()
	{
		assertEquals(java.util.Arrays.asList(Gear.SCYTHE_OF_VITUR, Gear.OBLIGATOR, Gear.TWISTED_BOW, Gear.TUMEKENS_SHADOW),
			BossDrop.MEGARARES);
		for (Style style : Style.values())
		{
			long count = BossDrop.MEGARARES.stream().filter(g -> g.getStyle() == style).count();
			assertTrue("at least one megarare for " + style, count >= 1);
		}
		for (Gear g : BossDrop.MEGARARES)
		{
			assertTrue(g + " stays out of the upgrade chains", g.isUnique());
			assertEquals(Slot.WEAPON, g.getSlot());
			// clearly better than the best plain weapon of the style (on paper; a volley like the
			// Ascension crossbows' needs extra power to keep up in play, so the margin is smaller here)
			Gear best = Gear.chain(Slot.WEAPON, g.getStyle()).get(Gear.chain(Slot.WEAPON, g.getStyle()).size() - 1);
			double mine = g.getDamage() * g.getVolley() / g.getCooldown();
			double theirs = best.getDamage() * best.getVolley() / best.getCooldown();
			assertTrue(g + " out-damages " + best, mine > theirs * 1.15);
		}
	}
}
