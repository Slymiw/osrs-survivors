package com.osrssurvivors.game;

/**
 * Something that just happened in a run, for sound and screen effects: an attack, a hit landing,
 * a kill, a pickup, a level-up. The session records cues as it goes and the page takes them each
 * frame ({@link GameSession#takeCues()}). They carry no gameplay weight.
 */
public final class Cue
{
	public enum Type
	{
		/** The player's weapon attacks ({@link #gear}, null for fists). */
		ATTACK,
		/** A player's missile or swing lands on {@link #kind} ({@link #gear} as for ATTACK). */
		IMPACT,
		/** A monster dies; {@link #value} is 1 for a champion. */
		KILL,
		/** A monster shoots ({@link #shot} says how). */
		ENEMY_SHOT,
		/** The player takes damage; {@link #value} is the hitsplat's number. */
		PLAYER_HURT,
		PLAYER_DEATH,
		/** The Revival upgrade saves the player. */
		REVIVE,
		/** Redemption or Retribution goes off ({@link #passive}). */
		PRAYER_PROC,
		/** An ability attacks ({@link #passive}); for area spells, where it lands. */
		ABILITY,
		/** XP gems picked up; {@link #value} is the XP. */
		XP,
		CASKET_PICKUP,
		LEVEL_UP,
		/** A level-up or casket offer taken ({@link #gear} for gear, {@link #passive} for passives). */
		CHOOSE,
		REROLL,
		/** A champion is about to arrive, and when it does. */
		CHAMPION_WARNING,
		CHAMPION_ARRIVES,
		/** The casket's reels start, one stops ({@link #value} = reel), the prize is revealed and taken. */
		REELS_START,
		REEL_STOP,
		DROP_REVEALED,
		DROP_CLAIMED,
		EAT,
		DRINK,
		/** Gear put on from the bag ({@link #gear}). */
		EQUIP,
		ITEM_DROPPED,
		/** Something the player tried can't be done (a full bag, eating too soon). */
		REFUSED
	}

	public final Type type;
	/** Where it happened in the world. */
	public final double x;
	public final double y;
	/** Seconds from now until it should be seen and heard (a missile still in flight). */
	public final double delay;
	public final Gear gear;
	public final EnemyKind kind;
	public final Passive passive;
	public final EnemyKind.Attack shot;
	public final int value;

	Cue(Type type, double x, double y, double delay, Gear gear, EnemyKind kind, Passive passive, EnemyKind.Attack shot, int value)
	{
		this.type = type;
		this.x = x;
		this.y = y;
		this.delay = delay;
		this.gear = gear;
		this.kind = kind;
		this.passive = passive;
		this.shot = shot;
		this.value = value;
	}

	static Cue at(Type type, double x, double y)
	{
		return new Cue(type, x, y, 0, null, null, null, null, 0);
	}

	static Cue of(Type type)
	{
		return at(type, 0, 0);
	}

	Cue withValue(int v)
	{
		return new Cue(type, x, y, delay, gear, kind, passive, shot, v);
	}

	Cue withGear(Gear g)
	{
		return new Cue(type, x, y, delay, g, kind, passive, shot, value);
	}

	Cue withKind(EnemyKind k)
	{
		return new Cue(type, x, y, delay, gear, k, passive, shot, value);
	}

	Cue withPassive(Passive p)
	{
		return new Cue(type, x, y, delay, gear, kind, p, shot, value);
	}

	Cue withShot(EnemyKind.Attack a)
	{
		return new Cue(type, x, y, delay, gear, kind, passive, a, value);
	}

	Cue after(double seconds)
	{
		return new Cue(type, x, y, seconds, gear, kind, passive, shot, value);
	}

	@Override
	public String toString()
	{
		return type + (gear != null ? " " + gear : "") + (kind != null ? " " + kind : "") + (passive != null ? " " + passive : "")
			+ (value != 0 ? " " + value : "");
	}
}
