package com.osrssurvivors.audio;

/**
 * How a kind of sound behaves in a crowd: its bus and loudness, the shortest gap between two
 * starts of the same sound, how many of it may play at once, and its priority when the mixer is
 * full (a higher one stops the quietest lower one). Sixty monsters dying in a second would
 * otherwise play sixty death sounds; with these, a swarm sounds busy without turning to noise.
 */
public enum Policy
{
	/** The player's weapon: always heard, it's the rhythm of the game. */
	ATTACK(Bus.EFFECTS, 0.55, 0.05, 2, 7, 0.04),
	/** A spell landing (melee and arrows have no impact sound of their own in the game). */
	IMPACT(Bus.EFFECTS, 0.45, 0.06, 3, 5, 0.05),
	/** A monster's grunt when hit. */
	ENEMY_HIT(Bus.EFFECTS, 0.32, 0.11, 2, 3, 0.06),
	ENEMY_DEATH(Bus.EFFECTS, 0.42, 0.07, 3, 4, 0.05),
	ENEMY_SHOT(Bus.EFFECTS, 0.22, 0.3, 2, 2, 0.06),
	/** Cannon, thralls, barrages and the like. */
	ABILITY(Bus.EFFECTS, 0.45, 0.12, 2, 6, 0.04),
	PLAYER_HURT(Bus.EFFECTS, 0.6, 0.55, 1, 8, 0.03),
	/** Gems: soft and frequent, pitched up as a streak goes on. */
	PICKUP(Bus.EFFECTS, 0.3, 0.06, 2, 2, 0),
	/** Champions, revival, death, prayers going off: rare and important. */
	BIG(Bus.EFFECTS, 0.75, 0.25, 2, 9, 0),
	/** Buttons, the bag. */
	UI(Bus.INTERFACE, 0.5, 0.03, 2, 10, 0),
	/** Level-ups, the casket's reels and prizes. */
	FANFARE(Bus.INTERFACE, 0.7, 0.1, 2, 10, 0);

	public final Bus bus;
	public final double gain;
	/** Seconds before the same sound may start again. */
	public final double cooldown;
	/** Copies of one sound that may play at once. */
	public final int voices;
	public final int priority;
	/** Random playback-rate spread (plus or minus), so repeats don't sound mechanical. */
	public final double spread;

	Policy(Bus bus, double gain, double cooldown, int voices, int priority, double spread)
	{
		this.bus = bus;
		this.gain = gain;
		this.cooldown = cooldown;
		this.voices = voices;
		this.priority = priority;
		this.spread = spread;
	}
}
