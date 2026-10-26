package com.osrssurvivors;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.Passive;
import com.osrssurvivors.game.Slot;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class GearArtTest
{
	@Test
	public void everyItemHasAnItemId()
	{
		Set<Integer> ids = new HashSet<>();
		for (Gear g : Gear.values())
		{
			assertTrue(g + " has an item", GearArt.ITEMS.containsKey(g));
			assertTrue(g + " has its own item", ids.add(GearArt.itemId(g)));
		}
	}

	@Test
	public void everyWeaponHasAnAttackAnimation()
	{
		for (Gear g : Gear.values())
		{
			if (g.getSlot() == Slot.WEAPON)
			{
				assertTrue(g + " animates", g.getStyle().name().equals("MELEE") && g.getTier() == 0
					|| GearArt.attackAnimation(g) != GearArt.PUNCH);
				if (g.getMissile() == Gear.Missile.SPELL)
				{
					assertNotEquals(g + " has a spell icon", -1, GearArt.spellSprite(g));
				}
			}
		}
	}

	@Test
	public void everyPassiveHasAnIcon()
	{
		for (Passive p : Passive.values())
		{
			for (int rank = 1; rank <= p.getMaxRank(); rank++)
			{
				assertTrue(p + " " + rank, GearArt.passiveSprite(p, rank) >= 0 || GearArt.passiveItem(p) >= 0);
			}
		}
	}
}
