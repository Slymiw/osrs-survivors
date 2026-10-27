package com.osrssurvivors.audio;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Arrays;
import org.junit.Test;

/** The OSRS synthesiser, on the sounds the game actually ships. */
public class SynthSoundTest
{
	private static final ExportedSounds SOUNDS = ExportedSounds.load();

	@Test
	public void everyExportedSoundDecodesToSoundWithinRange()
	{
		int n = 0;
		for (String id : SOUNDS.ids())
		{
			SynthSound s = SynthSound.decode(SOUNDS.raw(Integer.parseInt(id)));
			float[] pcm = s.mix();
			assertEquals(id + " lasts as long as its instruments", s.getDurationMs() * SynthSound.SAMPLE_RATE / 1000, pcm.length);
			float peak = 0;
			for (float f : pcm)
			{
				assertTrue(id + " stays within -1..1", f >= -1 && f <= 1);
				peak = Math.max(peak, Math.abs(f));
			}
			assertTrue(id + " (" + SOUNDS.names.get(id) + ") is audible", peak > 0.02);
			n++;
		}
		assertTrue("the export has the game's sounds", n >= 100);
	}

	@Test
	public void synthesisIsTheSameEveryTime()
	{
		// the noise table is the client's own (java.util.Random(0)), so noisy sounds repeat exactly
		byte[] hit = SOUNDS.raw(2720);
		assertArrayEquals(SynthSound.decode(hit).mix(), SynthSound.decode(hit).mix(), 0f);
	}

	@Test
	public void leadingSilenceIsTrimmed()
	{
		// hacksword_slash waits 0.35 s for the client's attack animation; the game hits at once
		float[] pcm = SynthSound.decode(SOUNDS.raw(2500)).mix();
		float[] trimmed = SynthSound.trimLead(pcm);
		int cut = pcm.length - trimmed.length;
		assertTrue("about a third of a second goes", cut > SynthSound.SAMPLE_RATE * 0.3 && cut < SynthSound.SAMPLE_RATE * 0.4);
		int first = 0;
		while (Math.abs(trimmed[first]) < SynthSound.SILENCE)
		{
			first++;
		}
		assertTrue("sound starts within a few milliseconds", first <= SynthSound.LEAD_KEEP);
		assertArrayEquals("the rest is untouched", Arrays.copyOfRange(pcm, cut, pcm.length), trimmed, 0f);
		float[] silent = new float[100];
		assertEquals("all silence is left alone", silent, SynthSound.trimLead(silent));
	}

	@Test
	public void brokenDataIsRefused()
	{
		byte[] whole = SOUNDS.raw(2500);
		try
		{
			SynthSound.decode(Arrays.copyOf(whole, whole.length / 2));
			fail("half a sound decoded");
		}
		catch (IllegalArgumentException expected)
		{
			// a cut-off file says so instead of failing somewhere else
		}
	}

	@Test
	public void rmsMeasuresLoudness()
	{
		assertEquals(0, SynthSound.rms(new float[0]), 0);
		assertEquals(0.5, SynthSound.rms(new float[]{0.5f, -0.5f, 0.5f}), 1e-6);
	}
}
