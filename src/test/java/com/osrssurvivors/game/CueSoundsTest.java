package com.osrssurvivors.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.osrssurvivors.audio.ExportedSounds;
import com.osrssurvivors.audio.Policy;
import com.osrssurvivors.audio.SoundCues;
import com.osrssurvivors.audio.SoundRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;

/** Every sound the game can ask for is in the exported table, and world sounds sit where they happen. */
public class CueSoundsTest
{
	private static final ExportedSounds SOUNDS = ExportedSounds.load();

	/** One of every cue the game makes, in every variety it comes in. */
	private static List<Cue> everyCue()
	{
		List<Cue> cues = new ArrayList<>();
		cues.add(Cue.of(Cue.Type.ATTACK));
		for (Gear g : Gear.values())
		{
			if (g.getSlot() == Slot.WEAPON)
			{
				cues.add(Cue.of(Cue.Type.ATTACK).withGear(g));
				cues.add(Cue.of(Cue.Type.IMPACT).withGear(g).withKind(EnemyKind.RAT));
			}
			cues.add(Cue.of(Cue.Type.EQUIP).withGear(g));
			cues.add(Cue.of(Cue.Type.CHOOSE).withGear(g));
			cues.add(Cue.of(Cue.Type.DROP_CLAIMED).withGear(g));
		}
		for (EnemyKind k : EnemyKind.values())
		{
			cues.add(Cue.of(Cue.Type.IMPACT).withKind(k));
			cues.add(Cue.of(Cue.Type.KILL).withKind(k));
			cues.add(Cue.of(Cue.Type.KILL).withKind(k).withValue(1));
			cues.add(Cue.of(Cue.Type.CHAMPION_ARRIVES).withKind(k));
		}
		for (EnemyKind.Attack a : EnemyKind.Attack.values())
		{
			if (a != EnemyKind.Attack.NONE)
			{
				cues.add(Cue.of(Cue.Type.ENEMY_SHOT).withShot(a));
			}
		}
		for (Passive p : Passive.values())
		{
			cues.add(Cue.of(Cue.Type.CHOOSE).withPassive(p));
		}
		for (Passive p : new Passive[]{Passive.CANNON, Passive.THRALLS, Passive.CHINCHOMPAS})
		{
			cues.add(Cue.of(Cue.Type.ABILITY).withPassive(p));
		}
		for (Passive p : new Passive[]{Passive.ICE_BARRAGE, Passive.BLOOD_BARRAGE, Passive.GOD_SPELLS, Passive.DRAGONFIRE,
			Passive.CHINCHOMPAS})
		{
			cues.add(Cue.of(Cue.Type.ABILITY).withPassive(p).withValue(1));
		}
		cues.add(Cue.of(Cue.Type.PRAYER_PROC).withPassive(Passive.REDEMPTION));
		cues.add(Cue.of(Cue.Type.PRAYER_PROC).withPassive(Passive.RETRIBUTION));
		for (BossDrop.Rarity r : BossDrop.Rarity.values())
		{
			cues.add(Cue.of(Cue.Type.DROP_REVEALED).withValue(r.ordinal()));
		}
		for (Cue.Type t : new Cue.Type[]{Cue.Type.PLAYER_HURT, Cue.Type.PLAYER_DEATH, Cue.Type.REVIVE, Cue.Type.XP,
			Cue.Type.CASKET_PICKUP, Cue.Type.LEVEL_UP, Cue.Type.REROLL, Cue.Type.CHAMPION_WARNING, Cue.Type.REELS_START,
			Cue.Type.REEL_STOP, Cue.Type.DROP_CLAIMED, Cue.Type.EAT, Cue.Type.DRINK, Cue.Type.ITEM_DROPPED, Cue.Type.REFUSED,
			Cue.Type.CHOOSE})
		{
			cues.add(Cue.of(t));
		}
		return cues;
	}

	@Test
	public void everyCueSoundIsInTheExport()
	{
		SoundCues sounds = new SoundCues(new Random(1));
		int asked = 0;
		for (Cue c : everyCue())
		{
			for (SoundRequest r : sounds.forCue(c, 0, 0, 0))
			{
				assertTrue(r.key + " (for " + c + ") is in sounds.js; re-export with ./gradlew exportSounds",
					SOUNDS.table.containsKey(r.key));
				asked++;
			}
		}
		for (SoundRequest r : new SoundRequest[]{SoundCues.click(), SoundCues.error(), SoundCues.purchase(), SoundCues.unlock()})
		{
			assertTrue(r.key, SOUNDS.table.containsKey(r.key));
		}
		assertTrue(asked > 500);
	}

	@Test
	public void everyCueTheGameMakesHasASound()
	{
		SoundCues sounds = new SoundCues(new Random(1));
		for (Cue c : everyCue())
		{
			if (c.type == Cue.Type.IMPACT && c.kind == null || c.type == Cue.Type.ATTACK && c.gear == null)
			{
				continue;
			}
			assertTrue(c + " is heard", !sounds.forCue(c, 0, 0, 0).isEmpty());
		}
	}

	@Test
	public void worldSoundsArePannedAndFadeWithDistance()
	{
		SoundCues sounds = new SoundCues(new Random(1));
		SoundRequest near = sounds.forCue(Cue.at(Cue.Type.KILL, 10, 0).withKind(EnemyKind.RAT), 0, 0, 0).get(0);
		SoundRequest right = sounds.forCue(Cue.at(Cue.Type.KILL, 300, 0).withKind(EnemyKind.RAT), 0, 0, 0).get(0);
		SoundRequest left = sounds.forCue(Cue.at(Cue.Type.KILL, -300, 0).withKind(EnemyKind.RAT), 0, 0, 0).get(0);
		SoundRequest far = sounds.forCue(Cue.at(Cue.Type.KILL, 0, 2000).withKind(EnemyKind.RAT), 0, 0, 0).get(0);
		assertTrue(right.pan > 0.3 && left.pan < -0.3);
		assertEquals(0, far.pan, 1e-9);
		assertEquals(Policy.ENEMY_DEATH.gain, near.gain, 1e-9);
		assertTrue(far.gain < near.gain && far.gain >= Policy.ENEMY_DEATH.gain * 0.29);
		// a missile's impact is heard when it lands
		SoundRequest late = sounds.forCue(Cue.at(Cue.Type.IMPACT, 0, 0).withKind(EnemyKind.RAT).after(0.2), 0, 0, 0).get(0);
		assertEquals(0.2, late.delay, 1e-9);
	}

	@Test
	public void gemsRiseInPitchDuringAStreak()
	{
		SoundCues sounds = new SoundCues(new Random(1));
		double first = sounds.forCue(Cue.of(Cue.Type.XP), 0, 0, 0).get(0).rate;
		double second = sounds.forCue(Cue.of(Cue.Type.XP), 0.2, 0, 0).get(0).rate;
		double third = sounds.forCue(Cue.of(Cue.Type.XP), 0.4, 0, 0).get(0).rate;
		double after = sounds.forCue(Cue.of(Cue.Type.XP), 5, 0, 0).get(0).rate;
		assertEquals(1, first, 1e-9);
		assertTrue(second > first && third > second);
		assertEquals("a pause starts again", 1, after, 1e-9);
	}
}
