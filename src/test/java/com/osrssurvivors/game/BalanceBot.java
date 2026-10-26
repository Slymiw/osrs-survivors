package com.osrssurvivors.game;

import java.util.List;
import java.util.Random;

/**
 * A simple scripted player for balance checks: keeps away from the crowd, drifts towards gems
 * when it's safe, and always picks rewards for one combat style. The same bot plays every style,
 * so differences in how long it survives come from the styles themselves.
 */
final class BalanceBot
{
	/** How a bot run ended. */
	static final class Result
	{
		final double survived;
		final int kills;
		final int level;
		final double dps;
		/** Share of damage taken that came from monster missiles. */
		double missileShare;
		/** Whether a champion was alive at the end. */
		boolean championAlive;
		/** Coins the run paid out (or would have, had it ended). */
		int coins;

		Result(double survived, int kills, int level, double dps)
		{
			this.survived = survived;
			this.kills = kills;
			this.level = level;
			this.dps = dps;
		}
	}

	private BalanceBot()
	{
	}

	static Result play(Style style, Location location, long seed, double maxSeconds)
	{
		return play(style, location, seed, maxSeconds, null);
	}

	/** A run with a profile's permanent upgrades and pets; null for none. */
	static Result play(Style style, Location location, long seed, double maxSeconds, MetaProgress meta)
	{
		GameSession s = new GameSession(new Random(seed), Arena.open(GameSession.OPEN_TILES_X, GameSession.OPEN_TILES_Y), location);
		if (meta != null)
		{
			s.setMeta(meta);
		}
		s.setVisibleArea(1000, 480);
		double frame = 1 / 30.0;
		while (s.getPhase() != Phase.GAME_OVER && s.getElapsed() < maxSeconds)
		{
			if (s.getPhase() == Phase.LEVEL_UP)
			{
				s.chooseUpgrade(pick(s, style));
				continue;
			}
			if (s.getPhase() == Phase.BOSS_DROP)
			{
				// stops the reels, then takes the drop once it can be claimed
				if (!s.claimDrop())
				{
					s.advance(frame);
				}
				continue;
			}
			steer(s);
			s.advance(frame);
		}
		Result r = new Result(s.getElapsed(), s.getKills(), s.getProgression().getLevel(), s.getWeapon().getDps());
		r.missileShare = s.missileDamageTaken / Math.max(1, s.missileDamageTaken + s.contactDamageTaken);
		r.championAlive = s.getChampion() != null;
		r.coins = s.getCoinsEarned() >= 0 ? s.getCoinsEarned() : s.coinsSoFar();
		return r;
	}

	/**
	 * Backs away from monsters that are about to touch it, otherwise closes in until the nearest
	 * monster is within weapon range (melee walks in, ranged and magic keep their distance), and
	 * picks up gems when nothing is near. Stays off the edges.
	 */
	private static void steer(GameSession s)
	{
		Player p = s.getPlayer();
		double reach = s.getWeapon().getRange();
		double mx = 0;
		double my = 0;
		Enemy nearest = null;
		double nearestD = Double.MAX_VALUE;
		for (Enemy e : s.getEnemies())
		{
			double dx = p.x - e.x;
			double dy = p.y - e.y;
			double d = Math.hypot(dx, dy);
			if (d < nearestD)
			{
				nearestD = d;
				nearest = e;
			}
			double danger = e.radius + Player.RADIUS + 10;
			if (d < danger && d > 1e-6)
			{
				mx += dx / d * (danger - d) / danger;
				my += dy / d * (danger - d) / danger;
			}
		}
		if (mx == 0 && my == 0 && nearest != null && nearestD > reach * 0.8)
		{
			mx = (nearest.x - p.x) / nearestD * 0.3;
			my = (nearest.y - p.y) / nearestD * 0.3;
		}
		XpOrb best = null;
		double bestD = Double.MAX_VALUE;
		for (XpOrb o : s.getOrbs())
		{
			double d = Math.hypot(o.x - p.x, o.y - p.y);
			if (d < bestD)
			{
				bestD = d;
				best = o;
			}
		}
		if (best != null && bestD < 250 && nearestD > 90)
		{
			mx += (best.x - p.x) / bestD * 0.5;
			my += (best.y - p.y) / bestD * 0.5;
		}
		Arena a = s.getArena();
		double margin = 160;
		mx += p.x < margin ? 0.5 : p.x > a.getWidth() - margin ? -0.5 : 0;
		my += p.y < margin ? 0.5 : p.y > a.getHeight() - margin ? -0.5 : 0;
		s.setMoveDirection(mx, my);
	}

	/**
	 * The reward a player of one style would take: weapon upgrades first, then abilities, then
	 * a mix of armour and prayers (stronger armour wins over plain passives).
	 */
	private static int pick(GameSession s, Style style)
	{
		List<Offer> choices = s.getChoices();
		int best = 0;
		double bestScore = -1;
		Player p = s.getPlayer();
		for (int i = 0; i < choices.size(); i++)
		{
			Offer o = choices.get(i);
			double score;
			switch (o.getKind())
			{
				case GEAR:
					Gear g = o.getGear();
					if (g.getSlot() == Slot.WEAPON)
					{
						score = g.getStyle() == style ? 100 : -10;
					}
					else
					{
						score = g.getStyle() == null || g.getStyle() == style ? 38 + g.getPower() * 10 : -5;
					}
					break;
				case PASSIVE:
					// like a player: abilities while there are slots, then prayers and potions for the style
					Passive pa = o.getPassive();
					score = pa.isAbility() ? 55 : pa.getStyle() != null ? 46 : 36;
					break;
				default:
					score = p.health < p.maxHealth * 0.5 ? 120 : 0;
					break;
			}
			if (score > bestScore)
			{
				bestScore = score;
				best = i;
			}
		}
		return best;
	}
}
