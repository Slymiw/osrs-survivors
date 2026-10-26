package com.osrssurvivors.game;

public enum Phase
{
	/** The start menu: new run or continue a saved one. */
	TITLE,
	/** Simulation advancing. */
	RUNNING,
	/** Paused by the player (or automatically on focus loss). */
	PAUSED,
	/** Simulation frozen while the player picks one of the offered upgrades. */
	LEVEL_UP,
	/** Simulation frozen while a champion's drop spins on the reels. */
	BOSS_DROP,
	/** Player died; only a restart is possible. */
	GAME_OVER
}
