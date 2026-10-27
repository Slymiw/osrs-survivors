package com.osrssurvivors.audio;

import com.osrssurvivors.game.Volume;

/** Where a sound is mixed, each with its own volume setting under the master volume. */
public enum Bus
{
	/** Combat and the world: attacks, hits, deaths, pickups, champions. */
	EFFECTS(Volume.EFFECTS),
	/** Menus, buttons, level-ups and the casket's reels. */
	INTERFACE(Volume.INTERFACE);

	public final Volume volume;

	Bus(Volume volume)
	{
		this.volume = volume;
	}
}
