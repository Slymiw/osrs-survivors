package com.osrssurvivors.game;

import static com.osrssurvivors.game.ProgressionTest.levelledUp;
import static com.osrssurvivors.game.TestSessions.EPS;
import static com.osrssurvivors.game.TestSessions.quiet;
import static com.osrssurvivors.game.TestSessions.run;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

/** What lasts between runs: coins, the upgrade shop, pets, revivals, rerolls and location unlocks. */
public class MetaTest
{
	@Test
	public void ranksCostMoreEachTimeAndStopAtTheTop()
	{
		MetaProgress meta = new MetaProgress();
		meta.coins = 10_000;
		assertEquals(Perk.MIGHT.cost(0), meta.cost(Perk.MIGHT));
		assertTrue(meta.buy(Perk.MIGHT));
		assertEquals(1, meta.rank(Perk.MIGHT));
		assertEquals(2 * Perk.MIGHT.cost(0), meta.cost(Perk.MIGHT));
		while (meta.buy(Perk.MIGHT))
		{
			// buy to the top
		}
		assertEquals(Perk.MIGHT.getMaxRank(), meta.rank(Perk.MIGHT));
		assertEquals(-1, meta.cost(Perk.MIGHT));
		assertFalse(meta.canBuy(Perk.MIGHT));
	}

	@Test
	public void cantBuyWithoutTheCoins()
	{
		MetaProgress meta = new MetaProgress();
		meta.coins = Perk.REVIVAL.cost(0) - 1;
		int changes = meta.getChanges();
		assertFalse(meta.buy(Perk.REVIVAL));
		assertEquals(0, meta.rank(Perk.REVIVAL));
		assertEquals(changes, meta.getChanges());
	}

	@Test
	public void storedAsNamesAndIgnoresUnknownOnes()
	{
		Gson gson = new Gson();
		MetaProgress meta = new MetaProgress();
		meta.coins = 500;
		meta.perks.put(Perk.HASTE.name(), 2);
		meta.addPet(Pet.OLMLET);
		MetaProgress back = gson.fromJson(gson.toJson(meta), MetaProgress.class);
		assertEquals(500 + 0, back.coins);
		assertEquals(2, back.rank(Perk.HASTE));
		assertTrue(back.hasPet(Pet.OLMLET));

		MetaProgress odd = gson.fromJson("{\"coins\":3,\"perks\":{\"NOT_A_PERK\":4,\"MIGHT\":99},\"pets\":[\"NOT_A_PET\"]}",
			MetaProgress.class);
		assertEquals(Perk.MIGHT.getMaxRank(), odd.rank(Perk.MIGHT));
		assertTrue(odd.getPets().isEmpty());
	}

	@Test
	public void upgradesAndPetsShapeEveryRun()
	{
		MetaProgress meta = new MetaProgress();
		meta.perks.put(Perk.MIGHT.name(), 2);
		meta.perks.put(Perk.VITALITY.name(), 3);
		meta.perks.put(Perk.HASTE.name(), 1);
		meta.addPet(Pet.VORKI);
		GameSession s = quiet();
		s.setMeta(meta);
		Build b = s.getBuild();
		assertEquals(1 + 0.08, b.damageMultiplier(), EPS);
		assertEquals(Player.BASE_MAX_HEALTH + 30, s.getPlayer().getMaxHealth(), EPS);
		// a fresh run starts at full hitpoints, upgrades included
		assertEquals(s.getPlayer().getMaxHealth(), s.getPlayer().getHealth(), EPS);
		assertEquals(Build.FIST_COOLDOWN * 0.97, b.attackCooldown(), EPS);
		assertEquals(Player.BASE_MAGNET_RADIUS + 40, b.magnetRadius(), EPS);
	}

	@Test
	public void deathPaysCoinsIntoTheProfile()
	{
		MetaProgress meta = new MetaProgress();
		GameSession s = quiet();
		s.setMeta(meta);
		run(s, 60);
		Player p = s.getPlayer();
		p.health = 0.01;
		s.spawnEnemyAt(p.x + Enemy.RADIUS + Player.RADIUS, p.y, 1e9);
		run(s, 1);
		assertEquals(Phase.GAME_OVER, s.getPhase());
		assertTrue(s.getCoinsEarned() > 0);
		assertEquals(s.getCoinsEarned(), meta.coins);
	}

	@Test
	public void greedPaysMore()
	{
		MetaProgress greedy = new MetaProgress();
		greedy.perks.put(Perk.GREED.name(), 5);
		GameSession plain = quiet();
		GameSession rich = quiet();
		rich.setMeta(greedy);
		run(plain, 120);
		run(rich, 120);
		assertTrue(rich.coinsSoFar() > plain.coinsSoFar());
	}

	@Test
	public void revivalSavesOneDeath()
	{
		MetaProgress meta = new MetaProgress();
		meta.perks.put(Perk.REVIVAL.name(), 1);
		GameSession s = quiet();
		s.setMeta(meta);
		Player p = s.getPlayer();
		assertEquals(1, s.getRevivesLeft());
		p.health = 0.01;
		Enemy e = s.spawnEnemyAt(p.x + Enemy.RADIUS + Player.RADIUS, p.y, 1e9);
		run(s, 0.1);
		assertEquals(Phase.RUNNING, s.getPhase());
		assertEquals(0, s.getRevivesLeft());
		assertTrue(p.getHealth() > p.getMaxHealth() * 0.4);
		assertTrue("monsters nearby are frozen", e.isFrozen());

		p.health = 0.01;
		e.frozen = 0;
		run(s, 0.5);
		assertEquals(Phase.GAME_OVER, s.getPhase());
	}

	@Test
	public void rerollsReplaceTheOffersWhileTheyLast()
	{
		MetaProgress meta = new MetaProgress();
		meta.perks.put(Perk.REROLL.name(), 1);
		GameSession s = quiet();
		s.setMeta(meta);
		assertFalse("only on a level-up", s.reroll());
		levelledUp(s, 1);
		assertEquals(1, s.getRerollsLeft());
		assertTrue(s.reroll());
		assertEquals(Phase.LEVEL_UP, s.getPhase());
		assertEquals(GameSession.CHOICE_COUNT, s.getChoices().size());
		assertEquals(0, s.getRerollsLeft());
		assertFalse(s.reroll());
	}

	@Test
	public void harderPlacesOpenUpOneDifficultyAtATime()
	{
		Map<Location, Double> best = new EnumMap<>(Location.class);
		assertTrue(Location.LUMBRIDGE.isUnlocked(best));
		assertTrue(Location.HERE.isUnlocked(best));
		assertFalse(Location.VARROCK.isUnlocked(best));
		best.put(Location.LUMBRIDGE, Location.UNLOCK_SECONDS - 1);
		assertFalse(Location.VARROCK.isUnlocked(best));
		best.put(Location.LUMBRIDGE, Location.UNLOCK_SECONDS);
		assertTrue(Location.VARROCK.isUnlocked(best));
		assertTrue(Location.DRAYNOR.isUnlocked(best));
		assertFalse("two steps up stays shut", Location.FALADOR.isUnlocked(best));
		best.put(Location.HERE, 9999.0);
		assertFalse("where you stand doesn't count", Location.FALADOR.isUnlocked(best));
	}

	@Test
	public void saveKeepsRunResources()
	{
		MetaProgress meta = new MetaProgress();
		meta.perks.put(Perk.REROLL.name(), 2);
		GameSession s = quiet();
		s.setMeta(meta);
		levelledUp(s, 1);
		s.reroll();
		RunSave save = new Gson().fromJson(new Gson().toJson(s.save()), RunSave.class);

		GameSession again = quiet();
		again.setMeta(meta);
		again.load(save);
		assertEquals(1, again.getRerollsLeft());
	}

	@Test
	public void attacksSpeedUpWithLevels()
	{
		Build b = new Build();
		b.equip(Gear.RUNE_SCIMITAR);
		assertEquals(4 * Gear.TICK, b.attackCooldown(), EPS);
		b.setLevel(20);
		double faster = b.attackCooldown();
		assertTrue(faster < 4 * Gear.TICK * 0.7);
		b.setLevel(500);
		assertEquals(4 * Gear.TICK * Build.MIN_LEVEL_HASTE, b.attackCooldown(), EPS);
		assertNotEquals(faster, b.attackCooldown(), EPS);
	}
}
