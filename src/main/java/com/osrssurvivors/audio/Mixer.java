package com.osrssurvivors.audio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decides which requested sounds actually play, so a swarm never becomes a wall of noise: each
 * sound waits its {@link Policy#cooldown} before starting again and plays at most
 * {@link Policy#voices} copies at once, and no more than {@link #MAX_VOICES} sounds play in all.
 * When full, a sound of higher priority takes the place of the oldest lowest-priority one (which
 * the caller stops); otherwise it's dropped. Knows nothing of the browser: the caller says how
 * long each sound lasts and does the playing.
 */
public final class Mixer
{
	/** Sounds playing at once, at most. */
	public static final int MAX_VOICES = 18;

	/** A sound that was let through, until it ends at {@link #end}. */
	public static final class Voice
	{
		public final String key;
		public final int priority;
		public final double start;
		public final double end;
		/** Whatever the caller uses to stop it. */
		public Object handle;

		Voice(String key, int priority, double start, double end)
		{
			this.key = key;
			this.priority = priority;
			this.start = start;
			this.end = end;
		}
	}

	/** What {@link #admit} decided: the new voice, and a voice to stop to make room (or null). */
	public static final class Decision
	{
		public final Voice voice;
		public final Voice stolen;

		Decision(Voice voice, Voice stolen)
		{
			this.voice = voice;
			this.stolen = stolen;
		}
	}

	private final List<Voice> playing = new ArrayList<>();
	private final Map<String, Double> lastStart = new HashMap<>();

	/**
	 * Whether {@code r} may start at {@code now} (seconds, plus its delay) and last
	 * {@code duration} seconds. Returns null when it's dropped.
	 */
	public Decision admit(SoundRequest r, double now, double duration)
	{
		double start = now + Math.max(0, r.delay);
		expire(now);
		Double last = lastStart.get(r.key);
		if (last != null && Math.abs(start - last) < r.policy.cooldown)
		{
			return null;
		}
		int same = 0;
		for (Voice v : playing)
		{
			if (v.key.equals(r.key) && v.end > start)
			{
				same++;
			}
		}
		if (same >= r.policy.voices)
		{
			return null;
		}
		Voice stolen = null;
		if (playing.size() >= MAX_VOICES)
		{
			for (Voice v : playing)
			{
				if (v.priority < r.policy.priority && (stolen == null || v.priority < stolen.priority
					|| v.priority == stolen.priority && v.start < stolen.start))
				{
					stolen = v;
				}
			}
			if (stolen == null)
			{
				return null;
			}
			playing.remove(stolen);
		}
		double length = duration / Math.max(0.25, r.rate);
		Voice voice = new Voice(r.key, r.policy.priority, start, start + length);
		playing.add(voice);
		lastStart.put(r.key, start);
		return new Decision(voice, stolen);
	}

	private void expire(double now)
	{
		playing.removeIf(v -> v.end <= now);
	}

	/** Sounds playing (or about to) at {@code now}. */
	public int playing(double now)
	{
		expire(now);
		return playing.size();
	}

	/** Forgets everything, as when the page's sound stops. */
	public void clear()
	{
		playing.clear();
		lastStart.clear();
	}
}
