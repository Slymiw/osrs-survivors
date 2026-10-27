package com.osrssurvivors.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

/** How many sounds get through when a crowd asks for them all at once. */
public class MixerTest
{
	@Test
	public void theSameSoundWaitsItsCooldown()
	{
		Mixer m = new Mixer();
		SoundRequest death = new SoundRequest("death.RAT", Policy.ENEMY_DEATH);
		assertNotNull(m.admit(death, 0, 1));
		assertNull("too soon", m.admit(death, Policy.ENEMY_DEATH.cooldown / 2, 1));
		assertNotNull("after the cooldown", m.admit(death, Policy.ENEMY_DEATH.cooldown * 1.01, 1));
		assertNotNull("another sound isn't held up", m.admit(new SoundRequest("death.COW", Policy.ENEMY_DEATH), 0.01, 1));
	}

	@Test
	public void aSwarmPlaysOnlyAFewCopiesOfOneSound()
	{
		Mixer m = new Mixer();
		SoundRequest hit = new SoundRequest("hit.ZOMBIE", Policy.ENEMY_HIT);
		int played = 0;
		// sixty hits a second for a second, each sound a second long
		for (int i = 0; i < 60; i++)
		{
			played += m.admit(hit, i / 60.0, 1) != null ? 1 : 0;
		}
		assertEquals(Policy.ENEMY_HIT.voices, played);
		assertEquals(Policy.ENEMY_HIT.voices, m.playing(0.99));
		assertEquals("they end", 0, m.playing(5));
	}

	@Test
	public void importantSoundsTakeThePlaceOfLesserOnes()
	{
		Mixer m = new Mixer();
		for (int i = 0; i < Mixer.MAX_VOICES; i++)
		{
			assertNotNull(m.admit(new SoundRequest("shot." + i, Policy.ENEMY_SHOT), 0, 5));
		}
		assertNull("a full mixer drops more of the same importance", m.admit(new SoundRequest("shot.more", Policy.ENEMY_SHOT), 0, 5));
		Mixer.Decision d = m.admit(new SoundRequest("event.LEVEL_UP", Policy.FANFARE), 0, 5);
		assertNotNull("a level-up always gets through", d);
		assertNotNull(d.stolen);
		assertEquals(Policy.ENEMY_SHOT.priority, d.stolen.priority);
		assertEquals(Mixer.MAX_VOICES, m.playing(0));
	}

	@Test
	public void delayedSoundsCountFromWhenTheyStart()
	{
		Mixer m = new Mixer();
		SoundRequest late = new SoundRequest("impact.X", Policy.IMPACT, 1, 1, 0, 0.2);
		Mixer.Decision d = m.admit(late, 0, 0.5);
		assertEquals(0.2, d.voice.start, 1e-9);
		assertEquals(0.7, d.voice.end, 1e-9);
		// a faster rate makes it shorter
		Mixer.Decision fast = m.admit(new SoundRequest("impact.Y", Policy.IMPACT, 1, 2, 0, 0), 0, 0.5);
		assertEquals(0.25, fast.voice.end, 1e-9);
		m.clear();
		assertEquals(0, m.playing(0));
		assertSame(null, m.admit(new SoundRequest("impact.Z", Policy.IMPACT, 1, 1, 0, 0), 0, 1).stolen);
	}
}
