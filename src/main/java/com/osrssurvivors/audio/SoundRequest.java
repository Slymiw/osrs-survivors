package com.osrssurvivors.audio;

/**
 * One sound to play: which (a key of the exported sound table, such as {@code attack.ABYSSAL_WHIP}
 * or {@code ui.click}), how loud, when, where, and the {@link Policy} that keeps it from piling up.
 */
public final class SoundRequest
{
	/** Name in the sound table. */
	public final String key;
	public final Policy policy;
	/** 0..1 before the volume settings. */
	public final double gain;
	/** Playback rate: 1 plays as recorded, a little more or less varies a sound heard often. */
	public final double rate;
	/** Left (-1) to right (1). */
	public final double pan;
	/** Seconds from now. */
	public final double delay;

	public SoundRequest(String key, Policy policy, double gain, double rate, double pan, double delay)
	{
		this.key = key;
		this.policy = policy;
		this.gain = gain;
		this.rate = rate;
		this.pan = pan;
		this.delay = delay;
	}

	public SoundRequest(String key, Policy policy)
	{
		this(key, policy, policy.gain, 1, 0, 0);
	}

	public Bus getBus()
	{
		return policy.bus;
	}

	@Override
	public String toString()
	{
		return key + " (" + policy + ")";
	}
}
