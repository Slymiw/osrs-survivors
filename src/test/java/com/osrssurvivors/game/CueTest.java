package com.osrssurvivors.game;

import static com.osrssurvivors.game.TestSessions.quiet;
import static com.osrssurvivors.game.TestSessions.run;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;

/** What the session reports for sound and screen effects, and when hits are seen to land. */
public class CueTest
{
	private static List<Cue> of(List<Cue> cues, Cue.Type type)
	{
		List<Cue> out = new ArrayList<>();
		for (Cue c : cues)
		{
			if (c.type == type)
			{
				out.add(c);
			}
		}
		return out;
	}

	private static void wear(GameSession s, Gear gear) throws Exception
	{
		Method wear = GameSession.class.getDeclaredMethod("wear", Gear.class);
		wear.setAccessible(true);
		wear.invoke(s, gear);
		Method apply = GameSession.class.getDeclaredMethod("applyBuild");
		apply.setAccessible(true);
		apply.invoke(s);
	}

	@Test
	public void aSwingIsSeenAtOnce()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		Enemy e = s.spawnEnemyAt(p.x + 30, p.y, 1e9);
		s.advance(GameSession.STEP);
		List<Cue> cues = s.takeCues();
		assertEquals(1, of(cues, Cue.Type.ATTACK).size());
		Cue impact = of(cues, Cue.Type.IMPACT).get(0);
		assertEquals(0, impact.delay, 0);
		assertEquals(e.getKind(), impact.kind);
		assertTrue(s.getHitsplats().get(0).isLanded());
		assertTrue(e.isFlashing());
		assertTrue("taken", s.takeCues().isEmpty());
	}

	@Test
	public void missilesLandBeforeTheirHitsShow() throws Exception
	{
		GameSession s = quiet();
		wear(s, Gear.MAGIC_SHORTBOW);
		Player p = s.getPlayer();
		Enemy e = s.spawnEnemyAt(p.x + 200, p.y, 1e9);
		for (int i = 0; i < 120 && s.getStrikes().isEmpty(); i++)
		{
			s.advance(GameSession.STEP);
		}
		Strike arrow = s.getStrikes().get(0);
		assertTrue("an arrow flies", arrow.flight > 0.15 && arrow.flight <= Strike.MAX_FLIGHT);
		Cue impact = of(s.takeCues(), Cue.Type.IMPACT).get(0);
		assertEquals("the impact waits for the arrow", arrow.flight, impact.delay, 1e-9);
		Effects.Hitsplat splat = s.getHitsplats().get(0);
		assertFalse("the hitsplat waits too", splat.isLanded());
		assertFalse("and so does the flash", e.isFlashing());
		assertTrue("the damage is dealt straight away", e.getHealth() < e.getMaxHealth());
		run(s, arrow.flight + GameSession.STEP);
		assertTrue(splat.isLanded());
		assertTrue(e.isFlashing());
		assertTrue("the arrow has arrived and flashes", arrow.getImpactAge() >= 0);
	}

	@Test
	public void killsLevelUpsAndPickupsAreReported()
	{
		GameSession s = quiet();
		Player p = s.getPlayer();
		s.spawnEnemyAt(p.x + 30, p.y, 1);
		run(s, 0.5);
		List<Cue> cues = s.takeCues();
		Cue kill = of(cues, Cue.Type.KILL).get(0);
		assertEquals(0, kill.value);
		assertEquals("gems picked up", 1, of(cues, Cue.Type.XP).size());
		s.devGrantLevels(1);
		assertEquals(1, of(s.takeCues(), Cue.Type.LEVEL_UP).size());
		s.chooseUpgrade(0);
		assertEquals(1, of(s.takeCues(), Cue.Type.CHOOSE).size());
	}

	@Test
	public void theReelsReportEachStopAndThePrizeOnce()
	{
		GameSession s = Scenarios.bossDrop(new Random(5));
		assertEquals(1, of(s.takeCues(), Cue.Type.REELS_START).size());
		Scenarios.spin(s, BossDrop.CLAIMABLE + 1);
		List<Cue> cues = s.takeCues();
		assertEquals(3, of(cues, Cue.Type.REEL_STOP).size());
		assertEquals(1, of(cues, Cue.Type.DROP_REVEALED).size());
		assertEquals(s.getBossDrop().getRarity().ordinal(), of(cues, Cue.Type.DROP_REVEALED).get(0).value);
		s.claimDrop();
		assertEquals(1, of(s.takeCues(), Cue.Type.DROP_CLAIMED).size());
	}

	@Test
	public void skippingTheReelsStillReportsThem()
	{
		GameSession s = Scenarios.bossDrop(new Random(6));
		s.takeCues();
		s.claimDrop();
		List<Cue> cues = s.takeCues();
		assertEquals(3, of(cues, Cue.Type.REEL_STOP).size());
		assertEquals(1, of(cues, Cue.Type.DROP_REVEALED).size());
	}

	@Test
	public void championsAreAnnouncedOnce()
	{
		GameSession s = new GameSession(new Random(9));
		List<Cue> cues = new ArrayList<>();
		for (int i = 0; i < 60 * 200 && s.getChampion() == null; i++)
		{
			if (s.getPhase() == Phase.LEVEL_UP)
			{
				s.chooseUpgrade(0);
			}
			// kept alive until the first champion is due
			s.getPlayer().health = s.getPlayer().maxHealth;
			s.advance(GameSession.STEP);
			cues.addAll(s.takeCues());
		}
		assertTrue(s.getChampion() != null);
		assertEquals(1, of(cues, Cue.Type.CHAMPION_WARNING).size());
		assertEquals(1, of(cues, Cue.Type.CHAMPION_ARRIVES).size());
	}

	@Test
	public void cuesNobodyTakesDontPileUp()
	{
		GameSession s = new GameSession(new Random(2));
		for (int i = 0; i < 60 * 120; i++)
		{
			if (s.getPhase() == Phase.LEVEL_UP)
			{
				s.chooseUpgrade(0);
			}
			s.advance(GameSession.STEP);
		}
		assertTrue(s.takeCues().size() <= GameSession.MAX_CUES);
		s.restart();
		assertTrue(s.takeCues().isEmpty());
	}

	@Test
	public void theBagReportsWhatItDoes()
	{
		GameSession s = quiet();
		s.getInventory().add(Item.food(Food.SHARK));
		s.getInventory().add(Item.food(Food.SHARK));
		assertTrue(s.useItem(0));
		assertEquals(1, of(s.takeCues(), Cue.Type.EAT).size());
		assertFalse("too soon to eat again", s.useItem(1));
		assertEquals(1, of(s.takeCues(), Cue.Type.REFUSED).size());
		assertTrue(s.dropItem(1));
		assertEquals(1, of(s.takeCues(), Cue.Type.ITEM_DROPPED).size());
	}
}
