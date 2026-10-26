package com.osrssurvivors.game;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** The player's display settings, shared by every session and kept between visits. */
public final class Options
{
	private final Set<Option> off = EnumSet.noneOf(Option.class);

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
}
