package com.osrssurvivors.render;

import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.Slot;
import java.util.Map;

/** Everything needed to draw the player character this frame. */
public final class PlayerPose
{
	/** Direction faced in radians (0 = east, PI/2 = south). */
	public final double facing;
	public final boolean moving;
	/** Seconds, for picking walk and idle frames. */
	public final double time;
	public final Map<Slot, Gear> worn;
	/** Changes whenever {@link #worn} does. */
	public final int gearVersion;
	/** Progress through the attack animation from 0 to 1, or -1 when not attacking. */
	public final double attackProgress;

	public PlayerPose(double facing, boolean moving, double time, Map<Slot, Gear> worn, int gearVersion, double attackProgress)
	{
		this.facing = facing;
		this.moving = moving;
		this.time = time;
		this.worn = worn;
		this.gearVersion = gearVersion;
		this.attackProgress = attackProgress;
	}

	public boolean isAttacking()
	{
		return attackProgress >= 0;
	}

	/** The held weapon, or null for fists. */
	public Gear weapon()
	{
		return worn.get(Slot.WEAPON);
	}
}
