package com.osrssurvivors.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public class RarityTest
{
	@Test
	public void rarityRisesAlongEveryChain()
	{
		for (Style style : Style.values())
		{
			for (Slot slot : Slot.values())
			{
				List<Gear> chain = Gear.chain(slot, style);
				for (int i = 1; i < chain.size(); i++)
				{
					assertTrue(chain.get(i) + " is at least as rare as " + chain.get(i - 1),
						chain.get(i).getRarity().ordinal() >= chain.get(i - 1).getRarity().ordinal());
				}
				if (chain.size() > 3)
				{
					assertEquals("chains start common", ItemRarity.COMMON, chain.get(0).getRarity());
					assertEquals("and top out epic", ItemRarity.EPIC, chain.get(chain.size() - 1).getRarity());
				}
			}
		}
	}

	@Test
	public void uniquesAreLegendaryAndMegararesRarest()
	{
		for (Gear g : BossDrop.ITEMS)
		{
			assertEquals(g.name(), ItemRarity.LEGENDARY, g.getRarity());
		}
		assertEquals(ItemRarity.LEGENDARY, Gear.SANGUINE_SCYTHE_OF_VITUR.getRarity());
		for (Gear g : BossDrop.MEGARARES)
		{
			assertEquals(g.name(), ItemRarity.MEGARARE, g.getRarity());
		}
		assertTrue("rarer than a pet", BossDrop.MEGARARE_CHANCE < BossDrop.JACKPOT_CHANCE / 10);
	}
}
