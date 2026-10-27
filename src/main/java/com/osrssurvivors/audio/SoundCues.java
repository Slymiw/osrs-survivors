package com.osrssurvivors.audio;

import com.osrssurvivors.game.BossDrop;
import com.osrssurvivors.game.Combination;
import com.osrssurvivors.game.Cue;
import com.osrssurvivors.game.Gear;
import com.osrssurvivors.game.Passive;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Turns what happened in a run ({@link Cue}s) into sounds to play. Keys name entries of the
 * exported sound table (see the exporter's SoundTable, which ties each to an OSRS sound effect):
 * <ul>
 * <li>{@code attack.<gear>} / {@code attack.FISTS}: the weapon's swing, shot or cast</li>
 * <li>{@code impact.<gear>}: a spell landing</li>
 * <li>{@code hit.<monster>}, {@code death.<monster>}: the monster's own grunt and death</li>
 * <li>{@code shot.<MAGIC|ARROW>}: a monster's missile</li>
 * <li>{@code ability.<passive>} (fired), {@code land.<passive>} (an area spell going off),
 * {@code prayer.<passive>} (a prayer chosen), {@code proc.<passive>} (Redemption or Retribution going off)</li>
 * <li>{@code event.*} and {@code ui.*}: everything else</li>
 * </ul>
 * Sounds from the world are panned by where they happen and quieter further away. Plain Java,
 * so the choices are tested without a browser.
 */
public final class SoundCues
{
	/** World units from the player at which world sounds start to fade, and how far they fade over. */
	static final double NEAR = 220;
	static final double FADE = 420;
	/** Quietest a world sound gets with distance. */
	static final double FAR_GAIN = 0.3;
	/** World units from the middle to a side of the screen, roughly; sounds pan up to {@link #MAX_PAN}. */
	static final double PAN_WIDTH = 360;
	static final double MAX_PAN = 0.6;
	/** XP pickups within this many seconds count as a streak and rise in pitch. */
	static final double STREAK_GAP = 0.7;
	static final double STREAK_STEP = 0.025;
	static final double STREAK_MAX = 0.3;

	private final Random random;
	private double lastXp = -1e9;
	private int streak;

	public SoundCues(Random random)
	{
		this.random = random;
	}

	/**
	 * The sounds for one cue.
	 *
	 * @param now real seconds, for pickup streaks
	 * @param px the player's position, for panning and distance
	 */
	public List<SoundRequest> forCue(Cue c, double now, double px, double py)
	{
		List<SoundRequest> out = new ArrayList<>(2);
		switch (c.type)
		{
			case ATTACK:
				world(out, "attack." + (c.gear == null ? "FISTS" : c.gear.name()), Policy.ATTACK, c, px, py, 1);
				break;
			case IMPACT:
				if (c.gear != null && c.gear.getMissile() == Gear.Missile.SPELL)
				{
					world(out, "impact." + c.gear.name(), Policy.IMPACT, c, px, py, 1);
				}
				if (c.kind != null)
				{
					world(out, "hit." + c.kind.name(), Policy.ENEMY_HIT, c, px, py, 1);
				}
				break;
			case KILL:
				if (c.value == 1)
				{
					world(out, "death." + c.kind.name(), Policy.BIG, c, px, py, 1);
					out.add(new SoundRequest("event.CHAMPION_DEATH", Policy.BIG, Policy.BIG.gain, 1, 0, c.delay + 0.15));
				}
				else
				{
					world(out, "death." + c.kind.name(), Policy.ENEMY_DEATH, c, px, py, 1);
				}
				break;
			case ENEMY_SHOT:
				world(out, "shot." + c.shot.name(), Policy.ENEMY_SHOT, c, px, py, 1);
				break;
			case PLAYER_HURT:
				// louder for a bigger hit, up to full for a tenth of base hitpoints
				out.add(new SoundRequest("event.PLAYER_HURT", Policy.PLAYER_HURT,
					Policy.PLAYER_HURT.gain * (0.6 + 0.4 * Math.min(1, c.value / 10.0)), vary(Policy.PLAYER_HURT), 0, 0));
				break;
			case PLAYER_DEATH:
				out.add(new SoundRequest("event.PLAYER_DEATH", Policy.BIG));
				break;
			case REVIVE:
				out.add(new SoundRequest("event.REVIVE", Policy.BIG));
				break;
			case PRAYER_PROC:
				out.add(new SoundRequest("proc." + c.passive.name(), Policy.BIG));
				break;
			case ABILITY:
				world(out, (c.value == 1 ? "land." : "ability.") + c.passive.name(), Policy.ABILITY, c, px, py, 1);
				break;
			case XP:
				out.add(pickup(now));
				break;
			case CASKET_PICKUP:
				out.add(new SoundRequest("event.CASKET", Policy.BIG));
				break;
			case LEVEL_UP:
				out.add(new SoundRequest("event.LEVEL_UP", Policy.FANFARE));
				break;
			case CHOOSE:
				if (c.gear != null && Combination.making(c.gear) != null)
				{
					out.add(new SoundRequest("event.COMBINATION", Policy.FANFARE));
				}
				out.add(new SoundRequest(chooseKey(c), Policy.UI));
				break;
			case REROLL:
				out.add(new SoundRequest("event.REROLL", Policy.UI));
				break;
			case CHAMPION_WARNING:
				out.add(new SoundRequest("event.CHAMPION_WARNING", Policy.BIG));
				break;
			case CHAMPION_ARRIVES:
				world(out, "event.CHAMPION_ARRIVES", Policy.BIG, c, px, py, 1);
				break;
			case REELS_START:
				out.add(new SoundRequest("event.REELS_START", Policy.FANFARE));
				break;
			case REEL_STOP:
				// each reel lands a little higher, building up to the prize
				out.add(new SoundRequest("event.REEL_STOP", Policy.FANFARE, Policy.FANFARE.gain, 1 + 0.08 * c.value, 0, 0));
				break;
			case DROP_REVEALED:
				out.add(new SoundRequest("event.WIN." + BossDrop.Rarity.values()[c.value].name(), Policy.FANFARE));
				break;
			case DROP_CLAIMED:
				out.add(new SoundRequest(c.gear != null ? "event.EQUIP" : "event.CLAIM", Policy.UI));
				break;
			case EAT:
				out.add(new SoundRequest("event.EAT", Policy.UI));
				break;
			case DRINK:
				out.add(new SoundRequest("event.DRINK", Policy.UI));
				break;
			case EQUIP:
				out.add(new SoundRequest(equipKey(c.gear), Policy.UI));
				break;
			case ITEM_DROPPED:
				out.add(new SoundRequest("event.DROP", Policy.UI));
				break;
			case REFUSED:
				out.add(new SoundRequest("ui.error", Policy.UI));
				break;
			default:
				break;
		}
		return out;
	}

	/** A sound from somewhere in the world: panned, quieter with distance, its rate varied a little. */
	private void world(List<SoundRequest> out, String key, Policy policy, Cue c, double px, double py, double gain)
	{
		double dx = c.x - px;
		double dy = c.y - py;
		double d = Math.hypot(dx, dy);
		double fade = d <= NEAR ? 1 : Math.max(FAR_GAIN, 1 - (d - NEAR) / FADE * (1 - FAR_GAIN));
		double pan = Math.max(-MAX_PAN, Math.min(MAX_PAN, dx / PAN_WIDTH * MAX_PAN));
		out.add(new SoundRequest(key, policy, policy.gain * gain * fade, vary(policy), pan, Math.max(0, c.delay)));
	}

	private double vary(Policy p)
	{
		return p.spread <= 0 ? 1 : 1 + (random.nextDouble() * 2 - 1) * p.spread;
	}

	/** A gem: rising in pitch while pickups keep coming. */
	private SoundRequest pickup(double now)
	{
		streak = now - lastXp <= STREAK_GAP ? streak + 1 : 0;
		lastXp = now;
		double rate = 1 + Math.min(STREAK_MAX, streak * STREAK_STEP);
		return new SoundRequest("event.XP", Policy.PICKUP, Policy.PICKUP.gain, rate, 0, 0);
	}

	private static String chooseKey(Cue c)
	{
		if (c.gear != null)
		{
			return equipKey(c.gear);
		}
		Passive p = c.passive;
		if (p != null && (p == Passive.MELEE_PRAYER || p == Passive.RANGED_PRAYER || p == Passive.MAGIC_PRAYER
			|| p == Passive.PROTECTION || p == Passive.PROTECT_MELEE || p == Passive.PROTECT_MISSILES || p == Passive.RAPID_HEAL
			|| p == Passive.REDEMPTION || p == Passive.RETRIBUTION))
		{
			return "prayer." + p.name();
		}
		return "event.CHOOSE";
	}

	/** The game's own "putting this on" sound for the kind of gear. */
	static String equipKey(Gear g)
	{
		return g == null ? "event.CHOOSE" : "equip." + g.name();
	}

	/** Sounds for pressing things on menus. */
	public static SoundRequest click()
	{
		return new SoundRequest("ui.click", Policy.UI);
	}

	public static SoundRequest error()
	{
		return new SoundRequest("ui.error", Policy.UI);
	}

	public static SoundRequest purchase()
	{
		return new SoundRequest("ui.buy", Policy.UI);
	}

	public static SoundRequest unlock()
	{
		return new SoundRequest("ui.unlock", Policy.FANFARE);
	}
}
