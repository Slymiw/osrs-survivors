package com.osrssurvivors.game;

import static com.osrssurvivors.game.TestSessions.quiet;
import static com.osrssurvivors.game.TestSessions.run;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.HashSet;
import org.junit.Test;

/** XP, levels and the level-up flow. Offer contents are covered in {@link OfferTest}. */
public class ProgressionTest
{
	@Test
	public void levelCurve()
	{
		Progression p = new Progression();
		assertEquals(1, p.getLevel());
		assertEquals(4, p.getXpToNext());
		assertEquals(0, p.addXp(3));
		assertEquals(1, p.addXp(1));
		assertEquals(2, p.getLevel());
		assertEquals(0, p.getXp());
		assertEquals(7, p.getXpToNext());
		// steeper later on
		assertTrue(Progression.xpToNext(50) > 10 * Progression.xpToNext(1));
	}

	@Test
	public void bigXpGainGivesSeveralLevels()
	{
		Progression p = new Progression();
		// 4 to reach level 2, 7 to reach level 3, 9 to reach level 4, 1 left over
		assertEquals(3, p.addXp(21));
		assertEquals(4, p.getLevel());
		assertEquals(1, p.getXp());
		assertEquals(3, p.getPendingLevelUps());
	}

	@Test
	public void magnetPullsInGems()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		s.dropOrbAt(p.x + Player.BASE_MAGNET_RADIUS - 1, p.y, 1);
		run(s, 0.5);
		assertTrue(s.getOrbs().isEmpty());
		assertEquals(1, s.getProgression().getXp());
	}

	@Test
	public void gemsOutsideMagnetStay()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		XpOrb orb = s.dropOrbAt(p.x + Player.BASE_MAGNET_RADIUS + 50, p.y, 1);
		run(s, 1.0);
		assertEquals(1, s.getOrbs().size());
		assertEquals(p.x + Player.BASE_MAGNET_RADIUS + 50, orb.x, 0);
		assertEquals(0, s.getProgression().getXp());
	}

	@Test
	public void lampBoostsXp()
	{
		GameSession s = quiet();
		s.grant(Offer.passive(Passive.LAMP, 1));
		s.grant(Offer.passive(Passive.LAMP, 2));
		Player p = s.getPlayer();
		s.dropOrbAt(p.x, p.y, 5);
		s.advance(GameSession.STEP);
		// 5 xp * 1.4 = 7: level 2 (4 xp) with 3 over
		assertEquals(2, s.getProgression().getLevel());
		assertEquals(3, s.getProgression().getXp());
	}

	@Test
	public void killToLevelUpEndToEnd()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		for (int i = 0; i < 4; i++)
		{
			s.spawnEnemyAt(p.x + 30, p.y + i, 1);
			run(s, Build.FIST_COOLDOWN + 0.05);
		}
		run(s, 1.0);
		assertEquals(4, s.getKills());
		assertEquals(2, s.getProgression().getLevel());
		assertEquals(Phase.LEVEL_UP, s.getPhase());
	}

	@Test
	public void levelUpOffersThreeDistinctChoicesAndFreezes()
	{
		GameSession s = levelledUp(quiet(), 1);
		assertEquals(Phase.LEVEL_UP, s.getPhase());
		assertEquals(GameSession.CHOICE_COUNT, s.getChoices().size());
		assertEquals(GameSession.CHOICE_COUNT, new HashSet<>(s.getChoices()).size());

		double elapsed = s.getElapsed();
		double x = s.getPlayer().x;
		s.setMoveDirection(1, 0);
		run(s, 1.0);
		assertEquals(elapsed, s.getElapsed(), 0);
		assertEquals(x, s.getPlayer().x, 0);
	}

	@Test
	public void choosingTakesTheOfferAndResumes()
	{
		GameSession s = levelledUp(quiet(), 1);
		Offer chosen = s.getChoices().get(0);
		assertTrue(s.chooseUpgrade(0));

		assertEquals(Phase.RUNNING, s.getPhase());
		assertTrue(s.getChoices().isEmpty());
		assertEquals(0, s.getProgression().getPendingLevelUps());
		if (chosen.getKind() == Offer.Kind.GEAR)
		{
			assertEquals(chosen.getGear(), s.getBuild().get(chosen.getGear().getSlot()));
		}
		else if (chosen.getKind() == Offer.Kind.PASSIVE)
		{
			assertEquals(chosen.getRank(), s.getBuild().rank(chosen.getPassive()));
		}
	}

	@Test
	public void invalidChoiceIsRejected()
	{
		GameSession s = levelledUp(quiet(), 1);
		assertFalse(s.chooseUpgrade(-1));
		assertFalse(s.chooseUpgrade(3));
		assertEquals(Phase.LEVEL_UP, s.getPhase());

		assertFalse(quiet().chooseUpgrade(0));
	}

	@Test
	public void queuedLevelUpsAreOfferedOneAfterAnother()
	{
		GameSession s = levelledUp(quiet(), 2);
		assertEquals(2, s.getProgression().getPendingLevelUps());
		assertTrue(s.chooseUpgrade(0));
		assertEquals(Phase.LEVEL_UP, s.getPhase());
		assertFalse(s.getChoices().isEmpty());
		assertTrue(s.chooseUpgrade(0));
		assertEquals(Phase.RUNNING, s.getPhase());
	}

	/** Gives the session enough XP for {@code levels} levels at once. */
	static GameSession levelledUp(GameSession s, int levels)
	{
		int xp = 0;
		for (int level = s.getProgression().getLevel(); level < s.getProgression().getLevel() + levels; level++)
		{
			xp += Progression.xpToNext(level);
		}
		Player p = s.getPlayer();
		s.dropOrbAt(p.x, p.y, xp - s.getProgression().getXp());
		s.advance(GameSession.STEP);
		assertEquals(Phase.LEVEL_UP, s.getPhase());
		return s;
	}
}
