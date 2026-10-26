package com.osrssurvivors.game;

import static com.osrssurvivors.game.TestSessions.EPS;
import static com.osrssurvivors.game.TestSessions.quiet;
import static com.osrssurvivors.game.TestSessions.run;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Random;
import org.junit.Test;

/** Saving a run and continuing it later, including through JSON as it is stored. */
public class SaveTest
{
	private static GameSession played()
	{
		GameSession s = quiet();
		s.grant(Offer.gear(Gear.RUNE_SCIMITAR));
		s.grant(Offer.gear(Gear.ADAMANT_PLATEBODY));
		s.grant(Offer.gear(Gear.AMULET_OF_GLORY));
		s.grant(Offer.passive(Passive.MELEE_PRAYER, 1));
		s.grant(Offer.passive(Passive.MELEE_PRAYER, 2));
		s.grant(Offer.passive(Passive.HITPOINTS, 1));
		Player p = s.getPlayer();
		s.spawnEnemyAt(p.x + 30, p.y, 1);
		run(s, 2);
		s.getPlayer().health = 77;
		return s;
	}

	@Test
	public void roundTripsThroughJson()
	{
		GameSession original = played();
		String json = new Gson().toJson(original.save());
		RunSave restored = new Gson().fromJson(json, RunSave.class);

		GameSession resumed = new GameSession(new Random(9));
		resumed.load(restored);

		assertEquals(original.getElapsed(), resumed.getElapsed(), EPS);
		assertEquals(original.getKills(), resumed.getKills());
		assertEquals(original.getProgression().getLevel(), resumed.getProgression().getLevel());
		assertEquals(original.getProgression().getXp(), resumed.getProgression().getXp());
		assertEquals(77, resumed.getPlayer().getHealth(), EPS);
		assertEquals(original.getBuild().getWorn(), resumed.getBuild().getWorn());
		assertEquals(original.getBuild().getRanks(), resumed.getBuild().getRanks());
		assertEquals(original.getWeapon().getMaxHit(), resumed.getWeapon().getMaxHit(), EPS);
		assertEquals(original.getPlayer().getMaxHealth(), resumed.getPlayer().getMaxHealth(), EPS);
		assertEquals(Phase.RUNNING, resumed.getPhase());
		assertTrue("the board starts fresh", resumed.getEnemies().isEmpty());
	}

	@Test
	public void pendingLevelUpsComeBack()
	{
		GameSession s = ProgressionTest.levelledUp(quiet(), 2);
		RunSave save = s.save();
		assertEquals(2, save.pendingLevelUps);
		GameSession resumed = quiet();
		resumed.load(save);
		assertEquals(Phase.LEVEL_UP, resumed.getPhase());
		assertEquals(2, resumed.getProgression().getPendingLevelUps());
	}

	@Test
	public void unknownNamesAreSkipped()
	{
		RunSave save = played().save();
		save.gear.put("HEAD", "NOT_A_REAL_HELM");
		save.passives.put("NOT_A_PRAYER", 3);
		save.passives.put("AGILITY", 99);
		GameSession resumed = quiet();
		resumed.load(save);
		assertNull(resumed.getBuild().get(Slot.HEAD));
		assertEquals("ranks are capped", Passive.AGILITY.getMaxRank(), resumed.getBuild().rank(Passive.AGILITY));
	}

	@Test
	public void titleOffersTheSavedRun()
	{
		GameSession s = quiet();
		s.showTitle("Level 7, 4:32");
		assertEquals(Phase.TITLE, s.getPhase());
		assertEquals("Level 7, 4:32", s.getContinueSummary());
		double elapsed = s.getElapsed();
		s.advance(1);
		assertEquals("nothing runs on the title screen", elapsed, s.getElapsed(), 0);
		s.togglePause();
		assertEquals(Phase.TITLE, s.getPhase());

		s.restart();
		assertEquals(Phase.RUNNING, s.getPhase());
	}

	@Test
	public void summaryReadsLikeTheHud()
	{
		RunSave save = new RunSave();
		save.level = 7;
		save.elapsed = 272.9;
		assertEquals("Where you stand, level 7, 4:32", save.summary());
		save.location = Location.VARROCK.name();
		assertEquals("Varrock, level 7, 4:32", save.summary());
	}
}
