package com.osrssurvivors.game;

import static org.junit.Assert.assertTrue;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

/**
 * Melee, ranged and magic should be about equally strong. The same scripted bot plays each style
 * through the same seeds in an easy, a middling and a hard location; average survival times must
 * stay within a quarter of each other (bot runs vary a lot from seed to seed). Set
 * -Dbalance.report=true to print the numbers.
 */
public class BalanceTest
{
	/** Seeds drawn from one generator (neighbouring small seeds play out nearly alike). */
	private static final long[] SEEDS = seeds(48);
	private static final double MAX_SECONDS = 1200;
	private static final double ALLOWED_SPREAD = 1.25;

	private static long[] seeds(int n)
	{
		java.util.Random random = new java.util.Random(2026);
		long[] seeds = new long[n];
		for (int i = 0; i < n; i++)
		{
			seeds[i] = random.nextLong();
		}
		return seeds;
	}

	@Test
	public void stylesSurviveAboutEquallyLong()
	{
		for (Location location : new Location[]{Location.LUMBRIDGE, Location.VARROCK, Location.WILDERNESS})
		{
			Map<Style, Double> survival = new EnumMap<>(Style.class);
			StringBuilder report = new StringBuilder(location.getTitle());
			for (Style style : Style.values())
			{
				double time = 0;
				double kills = 0;
				double level = 0;
				for (long seed : SEEDS)
				{
					BalanceBot.Result r = BalanceBot.play(style, location, seed, MAX_SECONDS);
					time += r.survived;
					kills += r.kills;
					level += r.level;
				}
				survival.put(style, time / SEEDS.length);
				report.append(String.format("%n  %-6s survived %5.0f s, %5.0f kills, level %3.0f", style,
					time / SEEDS.length, kills / SEEDS.length, level / SEEDS.length));
			}
			if (Boolean.getBoolean("balance.report"))
			{
				System.out.println(report);
			}
			double min = survival.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
			double max = survival.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
			assertTrue(report + "\nspread " + max / min, max / min <= ALLOWED_SPREAD);
		}
	}
}
