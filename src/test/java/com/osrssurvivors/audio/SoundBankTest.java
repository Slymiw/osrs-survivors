package com.osrssurvivors.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.osrssurvivors.game.Options;
import com.osrssurvivors.game.Volume;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** The sound bank against a stand-in for the browser, with the game's real sounds. */
public class SoundBankTest
{
	private static final ExportedSounds SOUNDS = ExportedSounds.load();

	/** Records what the bank asks of the browser. */
	private static final class FakeDevice implements SoundBank.Device
	{
		boolean ready = true;
		double now;
		final Map<Integer, float[]> buffers = new HashMap<>();
		final List<Integer> played = new ArrayList<>();
		final List<Double> gains = new ArrayList<>();
		final List<Integer> stopped = new ArrayList<>();
		final List<double[]> volumes = new ArrayList<>();
		final Map<Integer, byte[]> overrides = new HashMap<>();

		@Override
		public boolean ready()
		{
			return ready;
		}

		@Override
		public double now()
		{
			return now;
		}

		@Override
		public byte[] raw(int id)
		{
			return overrides.containsKey(id) ? overrides.get(id) : SOUNDS.raw(id);
		}

		@Override
		public void add(int id, float[] samples, int rate)
		{
			buffers.put(id, samples);
		}

		@Override
		public int play(int id, int bus, double gain, double rate, double pan, double delay)
		{
			played.add(id);
			gains.add(gain);
			return played.size();
		}

		@Override
		public void stop(int voice)
		{
			stopped.add(voice);
		}

		@Override
		public void volumes(double master, double effects, double ui)
		{
			volumes.add(new double[]{master, effects, ui});
		}
	}

	@Test
	public void playsKnownSoundsOnceTheBrowserAllowsSound()
	{
		FakeDevice device = new FakeDevice();
		device.ready = false;
		SoundBank bank = new SoundBank(SOUNDS.table, device);
		assertFalse("nothing before the player presses something", bank.play(SoundCues.click()));
		assertTrue(device.buffers.isEmpty());
		device.ready = true;
		assertTrue(bank.play(SoundCues.click()));
		int id = SOUNDS.table.get("ui.click");
		assertEquals(Integer.valueOf(id), device.played.get(0));
		assertTrue("its samples were handed over", device.buffers.get(id).length > 0);
	}

	@Test
	public void missingOrBrokenSoundsAreSilentNotErrors()
	{
		FakeDevice device = new FakeDevice();
		Map<String, Integer> table = new HashMap<>();
		table.put("ui.click", 999999);
		table.put("ui.error", 2268);
		device.overrides.put(2268, new byte[]{1, 2, 3});
		SoundBank bank = new SoundBank(table, device);
		assertFalse("not in the cache export", bank.play(SoundCues.click()));
		assertFalse("broken data", bank.play(SoundCues.error()));
		assertFalse("not in the table at all", bank.play(SoundCues.purchase()));
		assertTrue(device.played.isEmpty());
		// and with no table (no assets/sounds.js) at all
		SoundBank none = new SoundBank(null, device);
		assertFalse(none.play(SoundCues.click()));
		none.warm();
	}

	@Test
	public void warmsUpAFewSoundsAFrame()
	{
		FakeDevice device = new FakeDevice();
		SoundBank bank = new SoundBank(SOUNDS.table, device);
		bank.warm();
		assertEquals(SoundBank.WARM_PER_CALL, bank.prepared());
		for (int i = 0; i < 500; i++)
		{
			bank.warm();
		}
		assertEquals("every sound in the table gets ready", device.buffers.size(), bank.prepared());
		assertEquals(new java.util.HashSet<>(SOUNDS.table.values()).size(), bank.prepared());
	}

	@Test
	public void volumesGoToTheBrowserWhenTheyChange()
	{
		FakeDevice device = new FakeDevice();
		SoundBank bank = new SoundBank(SOUNDS.table, device);
		Options options = new Options();
		bank.setVolumes(options);
		bank.setVolumes(options);
		assertEquals("unchanged volumes aren't sent again", 1, device.volumes.size());
		options.setVolume(Volume.EFFECTS, 0);
		bank.setVolumes(options);
		assertEquals(2, device.volumes.size());
		assertEquals(0, device.volumes.get(1)[1], 0);
		options.setMuted(true);
		bank.setVolumes(options);
		assertEquals("mute silences the master", 0, device.volumes.get(2)[0], 0);
	}

	@Test
	public void loudnessIsEvenedOutWithinLimits()
	{
		assertEquals(1, SoundBank.level(SoundBank.TARGET_RMS), 1e-9);
		assertTrue("quiet sounds are raised", SoundBank.level(0.04) > 1);
		assertTrue("loud ones lowered", SoundBank.level(0.6) < 1);
		assertEquals(SoundBank.MAX_LEVEL, SoundBank.level(1e-6), 1e-9);
		assertEquals(SoundBank.MIN_LEVEL, SoundBank.level(100), 1e-9);
		assertEquals(1, SoundBank.level(0), 1e-9);
	}

	@Test
	public void aCrowdOfHitsIsThinnedByTheMixer()
	{
		FakeDevice device = new FakeDevice();
		SoundBank bank = new SoundBank(SOUNDS.table, device);
		SoundRequest hit = new SoundRequest("hit.ZOMBIE", Policy.ENEMY_HIT);
		for (int i = 0; i < 100; i++)
		{
			bank.play(hit);
		}
		assertEquals(1, device.played.size());
	}
}
