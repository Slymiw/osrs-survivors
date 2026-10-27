package com.osrssurvivors.audio;

import com.osrssurvivors.game.Options;
import com.osrssurvivors.game.Volume;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The game's sounds, ready to play: looks each requested key up in the exported table, turns the
 * OSRS sound effect into samples the first time it's wanted (or ahead of time, a few a frame),
 * evens out loudness, lets the {@link Mixer} decide whether it plays, and hands it to the
 * {@link Device}. A missing table, a missing or broken sound, or a device that can't play yet all
 * mean silence, never an error.
 */
public final class SoundBank
{
	/** What actually makes sound: the browser's audio, or a stand-in in tests. */
	public interface Device
	{
		/** Whether sound can play now (browsers wait for the player to press something). */
		boolean ready();

		/** Seconds on the sound clock. */
		double now();

		/** A sound's cache file, or null if there isn't one. */
		byte[] raw(int id);

		/** Keeps samples to play later under {@code id}. */
		void add(int id, float[] samples, int rate);

		/** Starts a sound; returns a handle for {@link #stop}, or 0 if it couldn't. */
		int play(int id, int bus, double gain, double rate, double pan, double delay);

		void stop(int voice);

		/** Master, effects and interface loudness, 0..1. */
		void volumes(double master, double effects, double ui);
	}

	/** Overall loudness (RMS) sounds are brought towards, half way in decibels. */
	static final double TARGET_RMS = 0.16;
	static final double MIN_LEVEL = 0.5;
	static final double MAX_LEVEL = 2.5;
	/** Sounds made ready ahead of time per {@link #warm} call. */
	static final int WARM_PER_CALL = 2;

	private final Map<String, Integer> table;
	private final Device device;
	private final Mixer mixer = new Mixer();
	/** Seconds each made-ready sound lasts, and how much to raise or lower it. */
	private final Map<Integer, Double> durations = new HashMap<>();
	private final Map<Integer, Double> levels = new HashMap<>();
	private final Set<Integer> broken = new HashSet<>();
	private final Deque<Integer> waiting = new ArrayDeque<>();
	private double[] sentVolumes;

	public SoundBank(Map<String, Integer> table, Device device)
	{
		this.table = table == null ? new HashMap<>() : new HashMap<>(table);
		this.device = device;
		for (Integer id : new HashSet<>(this.table.values()))
		{
			waiting.add(id);
		}
	}

	/** Whether the table has a sound for this key. */
	public boolean has(String key)
	{
		return table.containsKey(key);
	}

	public int size()
	{
		return table.size();
	}

	/** Sends the volume settings to the device when they've changed. */
	public void setVolumes(Options options)
	{
		double[] v = {options.gain(Volume.MASTER), options.gain(Volume.EFFECTS), options.gain(Volume.INTERFACE)};
		if (sentVolumes == null || v[0] != sentVolumes[0] || v[1] != sentVolumes[1] || v[2] != sentVolumes[2])
		{
			device.volumes(v[0], v[1], v[2]);
			sentVolumes = v;
		}
	}

	/** Gets a couple more sounds ready, so the first of each doesn't wait on the synthesiser. */
	public void warm()
	{
		if (!device.ready())
		{
			return;
		}
		for (int i = 0; i < WARM_PER_CALL && !waiting.isEmpty(); i++)
		{
			prepare(waiting.poll());
		}
	}

	/** Plays a request if the table, the device and the mixer allow; returns whether it started. */
	public boolean play(SoundRequest r)
	{
		Integer id = table.get(r.key);
		if (id == null || !device.ready() || !prepare(id))
		{
			return false;
		}
		Mixer.Decision d = mixer.admit(r, device.now(), durations.get(id));
		if (d == null)
		{
			return false;
		}
		if (d.stolen != null && d.stolen.handle instanceof Integer)
		{
			device.stop((Integer) d.stolen.handle);
		}
		int handle = device.play(id, r.getBus().ordinal(), r.gain * levels.get(id), r.rate, r.pan, r.delay);
		d.voice.handle = handle;
		return handle != 0;
	}

	/** Makes a sound playable; false if it's missing or broken. */
	private boolean prepare(int id)
	{
		if (durations.containsKey(id))
		{
			return true;
		}
		if (broken.contains(id))
		{
			return false;
		}
		try
		{
			byte[] raw = device.raw(id);
			if (raw == null)
			{
				broken.add(id);
				return false;
			}
			float[] samples = SynthSound.trimLead(SynthSound.decode(raw).mix());
			if (samples.length == 0)
			{
				broken.add(id);
				return false;
			}
			device.add(id, samples, SynthSound.SAMPLE_RATE);
			durations.put(id, samples.length / (double) SynthSound.SAMPLE_RATE);
			levels.put(id, level(SynthSound.rms(samples)));
			return true;
		}
		catch (RuntimeException e)
		{
			broken.add(id);
			return false;
		}
	}

	/**
	 * How much to raise or lower a sound of this loudness: half way (in decibels) to
	 * {@link #TARGET_RMS}, within limits. The game's sounds range from a whisper to clipping, and
	 * played side by side the loud ones would bury the quiet ones.
	 */
	static double level(double rms)
	{
		if (rms <= 0)
		{
			return 1;
		}
		return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, Math.sqrt(TARGET_RMS / rms)));
	}

	/** Sounds made ready so far. */
	public int prepared()
	{
		return durations.size();
	}

	public Mixer getMixer()
	{
		return mixer;
	}
}
