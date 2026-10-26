package com.osrssurvivors.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Random;
import org.junit.Test;

/** Overtime must end every run: no build may outlast the monsters, or stall unable to kill them. */
public class OvertimeTest
{
	/** A maxed melee tank standing its ground in overtime: full Torva, sanguine scythe, every defensive prayer. */
	private static GameSession tank(long seed)
	{
		MetaProgress meta = new MetaProgress();
		meta.maxOut();
		GameSession s = new GameSession(new Random(seed), Arena.open(GameSession.OPEN_TILES_X, GameSession.OPEN_TILES_Y), Location.LUMBRIDGE);
		s.setMeta(meta);
		s.setVisibleArea(1000, 480);
		RunSave save = new RunSave();
		save.location = Location.LUMBRIDGE.name();
		save.elapsed = Spawner.OVERTIME_START + 300;
		save.level = 100;
		save.gear.put(Slot.WEAPON.name(), Gear.SANGUINE_SCYTHE_OF_VITUR.name());
		save.gear.put(Slot.HEAD.name(), Gear.TORVA_FULL_HELM.name());
		save.gear.put(Slot.BODY.name(), Gear.TORVA_PLATEBODY.name());
		save.gear.put(Slot.LEGS.name(), Gear.TORVA_PLATELEGS.name());
		for (Passive p : new Passive[]{Passive.PROTECTION, Passive.PROTECT_MELEE, Passive.RAPID_HEAL, Passive.BONECRUSHER, Passive.HITPOINTS})
		{
			save.passives.put(p.name(), p.getMaxRank());
		}
		s.load(save);
		return s;
	}

	@Test
	public void aStandingTankStillFallsAndKeepsKilling()
	{
		Random seeds = new Random(5);
		for (int i = 0; i < 3; i++)
		{
			GameSession s = tank(seeds.nextLong());
			double start = s.getElapsed();
			int kills = s.getKills();
			while (s.getPhase() != Phase.GAME_OVER && s.getElapsed() < start + 900)
			{
				if (s.getPhase() == Phase.LEVEL_UP)
				{
					s.chooseUpgrade(0);
					continue;
				}
				if (s.getPhase() == Phase.BOSS_DROP)
				{
					if (!s.claimDrop())
					{
						s.advance(1 / 30.0);
					}
					continue;
				}
				s.advance(1 / 30.0);
			}
			assertEquals("the tank fell within 15 minutes of overtime", Phase.GAME_OVER, s.getPhase());
			double lasted = s.getElapsed() - start;
			assertTrue("and kept killing on the way: " + (s.getKills() - kills) + " in " + (int) lasted + " s", s.getKills() - kills > 20);
		}
	}

	@Test
	public void stallingRaisesMonsterDamageOnlyInOvertime()
	{
		GameSession s = tank(1);
		assertEquals(1, s.stallMultiplier(), 1e-9);
		// a minute and more with no kills
		s.disableSpawning();
		for (int i = 0; i < 60 * 70; i++)
		{
			s.advance(GameSession.STEP);
		}
		assertTrue("a stalled overtime hits harder: " + s.stallMultiplier(), s.stallMultiplier() > 5);

		GameSession early = new GameSession(new Random(1));
		early.disableSpawning();
		for (int i = 0; i < 60 * 120; i++)
		{
			early.advance(GameSession.STEP);
		}
		assertEquals("never before overtime", 1, early.stallMultiplier(), 1e-9);
	}
}
