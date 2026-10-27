package com.osrssurvivors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.osrssurvivors.audio.ExportedSounds;
import com.osrssurvivors.game.EnemyKind;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.Slot;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class SoundTableTest
{
	@Test
	public void everyWeaponAndMonsterHasItsSounds()
	{
		Map<String, SoundTable.Sfx> table = SoundTable.build();
		for (Gear g : Gear.values())
		{
			if (g.getSlot() == Slot.WEAPON)
			{
				assertNotNull(g + " attacks with a sound", table.get("attack." + g.name()));
				if (g.getMissile() == Gear.Missile.SPELL)
				{
					assertNotNull(g + "'s spell lands with a sound", table.get("impact." + g.name()));
				}
			}
			assertNotNull(g + " is put on with a sound", table.get("equip." + g.name()));
		}
		for (EnemyKind k : EnemyKind.values())
		{
			assertNotNull(table.get("hit." + k.name()));
			assertNotNull(table.get("death." + k.name()));
		}
		Map<Integer, String> names = new HashMap<>();
		for (SoundTable.Sfx s : table.values())
		{
			assertTrue(s.id > 0 && !s.name.isEmpty());
			String before = names.put(s.id, s.name);
			assertTrue("sound " + s.id + " has one name, not " + before + " and " + s.name, before == null || before.equals(s.name));
		}
	}

	@Test
	public void theCommittedExportMatchesTheTable()
	{
		Map<String, SoundTable.Sfx> table = SoundTable.build();
		ExportedSounds exported = ExportedSounds.load();
		assertEquals("re-export with ./gradlew exportSounds", table.size(), exported.table.size());
		for (Map.Entry<String, SoundTable.Sfx> e : table.entrySet())
		{
			assertEquals(e.getKey(), Integer.valueOf(e.getValue().id), exported.table.get(e.getKey()));
			assertEquals(e.getValue().name, exported.names.get(String.valueOf(e.getValue().id)));
		}
	}
}
