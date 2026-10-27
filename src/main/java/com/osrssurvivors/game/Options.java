package com.osrssurvivors.game;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** The player's display and sound settings, shared by every session and kept between visits. */
public final class Options
{
	private final Set<Option> off = EnumSet.noneOf(Option.class);
	private final Map<Volume, Integer> volumes = new EnumMap<>(Volume.class);
	private boolean muted;

	public Options()
	{
		for (Volume v : Volume.values())
		{
			volumes.put(v, v.getInitial());
		}
	}

	public boolean isOn(Option option)
	{
		return !off.contains(option);
	}

	public void toggle(Option option)
	{
		if (!off.remove(option))
		{
			off.add(option);
		}
	}

	/** The settings turned off, for saving. */
	public Set<Option> getOff()
	{
		return Collections.unmodifiableSet(off);
	}

	public void turnOff(Option option)
	{
		off.add(option);
	}

	/** A volume setting, 0 to {@link Volume#MAX}. */
	public int getVolume(Volume v)
	{
		return volumes.get(v);
	}

	public void setVolume(Volume v, int level)
	{
		volumes.put(v, Math.max(0, Math.min(Volume.MAX, level)));
		if (v == Volume.MASTER && level > 0)
		{
			muted = false;
		}
	}

	/** Mute (the M key) silences everything without losing the volume settings. */
	public boolean isMuted()
	{
		return muted;
	}

	public void setMuted(boolean muted)
	{
		this.muted = muted;
	}

	/**
	 * The loudness a setting gives, 0..1. Volume is heard roughly logarithmically, so the steps
	 * follow a curve rather than a straight line: half way is about a quarter of full.
	 */
	public double gain(Volume v)
	{
		if (muted && v == Volume.MASTER)
		{
			return 0;
		}
		double level = getVolume(v) / (double) Volume.MAX;
		return level * level;
	}
}
