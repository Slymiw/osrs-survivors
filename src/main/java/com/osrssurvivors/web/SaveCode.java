package com.osrssurvivors.web;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A player's progress as one line of text to carry to another device: the profile (coins,
 * upgrades, pets), the best times and the saved run, as JSON in URL-safe base64 with a prefix and
 * a checksum, e.g. {@code OSRS1-eyJ2Ijox...-1a2b3c4d}. Options stay with each device.
 */
final class SaveCode
{
	static final String PREFIX = "OSRS1-";
	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

	/** The stored records a code carries, each as JSON, or null when there was none. */
	static final class Contents
	{
		final String profile;
		final String bestTimes;
		final String run;

		Contents(String profile, String bestTimes, String run)
		{
			this.profile = profile;
			this.bestTimes = bestTimes;
			this.run = run;
		}

		/** What the code holds, for asking before it replaces this device's progress. */
		String describe()
		{
			Map<String, Object> p = profile == null ? null : Json.object(Json.parse(profile));
			int coins = p == null ? 0 : Json.integer(p.get("coins"), 0);
			int ranks = 0;
			Map<String, Object> perks = p == null ? null : Json.object(p.get("perks"));
			if (perks != null)
			{
				for (Object rank : perks.values())
				{
					ranks += Json.integer(rank, 0);
				}
			}
			List<Object> pets = p == null ? null : Json.array(p.get("pets"));
			Map<String, Object> times = bestTimes == null ? null : Json.object(Json.parse(bestTimes));
			StringBuilder b = new StringBuilder();
			b.append(coins).append(" coins, ").append(ranks).append(ranks == 1 ? " upgrade rank, " : " upgrade ranks, ")
				.append(pets == null ? 0 : pets.size()).append(pets != null && pets.size() == 1 ? " pet, " : " pets, ")
				.append("best times in ").append(times == null ? 0 : times.size()).append(times != null && times.size() == 1 ? " place" : " places");
			Map<String, Object> r = run == null ? null : Json.object(Json.parse(run));
			if (r != null)
			{
				b.append(" and a saved run (level ").append(Json.integer(r.get("level"), 1)).append(")");
			}
			return b.toString();
		}
	}

	private SaveCode()
	{
	}

	static String encode(Contents contents)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("v", 1);
		put(m, "profile", contents.profile);
		put(m, "bestTimes", contents.bestTimes);
		put(m, "run", contents.run);
		String body = base64(Json.write(m).getBytes(StandardCharsets.UTF_8));
		return PREFIX + body + "-" + checksum(body);
	}

	/** Reads a code as pasted (spaces and line breaks are ignored); throws with a reason to show the player. */
	static Contents decode(String code)
	{
		String text = code == null ? "" : code.replaceAll("\\s+", "");
		if (text.isEmpty())
		{
			throw new IllegalArgumentException("No code was pasted.");
		}
		if (!text.startsWith(PREFIX))
		{
			throw new IllegalArgumentException("That isn't an OSRS Survivors save code.");
		}
		int dash = text.lastIndexOf('-');
		String body = dash > PREFIX.length() ? text.substring(PREFIX.length(), dash) : "";
		if (body.isEmpty() || !checksum(body).equals(text.substring(dash + 1)))
		{
			throw new IllegalArgumentException("The code is incomplete or mistyped. Copy all of it and try again.");
		}
		Map<String, Object> m;
		try
		{
			m = Json.object(Json.parse(new String(unbase64(body), StandardCharsets.UTF_8)));
		}
		catch (RuntimeException e)
		{
			m = null;
		}
		if (m == null || Json.integer(m.get("v"), 0) != 1)
		{
			throw new IllegalArgumentException("The code couldn't be read. It may be from a newer version of the game.");
		}
		return new Contents(record(m, "profile"), record(m, "bestTimes"), record(m, "run"));
	}

	private static void put(Map<String, Object> m, String key, String json)
	{
		Object value = null;
		try
		{
			value = json == null || json.isEmpty() ? null : Json.object(Json.parse(json));
		}
		catch (RuntimeException e)
		{
			// an unreadable record isn't worth carrying
		}
		if (value != null)
		{
			m.put(key, value);
		}
	}

	private static String record(Map<String, Object> m, String key)
	{
		Map<String, Object> value = Json.object(m.get(key));
		return value == null ? null : Json.write(value);
	}

	/** Eight hex digits of a 32-bit FNV-1a hash, so a mistyped or cut-off code is caught. */
	static String checksum(String text)
	{
		int h = 0x811c9dc5;
		for (int i = 0; i < text.length(); i++)
		{
			h ^= text.charAt(i);
			h *= 0x01000193;
		}
		String hex = Integer.toHexString(h);
		while (hex.length() < 8)
		{
			hex = "0" + hex;
		}
		return hex;
	}

	static String base64(byte[] data)
	{
		StringBuilder b = new StringBuilder((data.length + 2) / 3 * 4);
		for (int i = 0; i < data.length; i += 3)
		{
			int n = (data[i] & 0xff) << 16 | (i + 1 < data.length ? (data[i + 1] & 0xff) << 8 : 0)
				| (i + 2 < data.length ? data[i + 2] & 0xff : 0);
			int chars = Math.min(4, (data.length - i) * 4 / 3 + 1);
			for (int c = 0; c < chars; c++)
			{
				b.append(ALPHABET.charAt(n >> (18 - 6 * c) & 63));
			}
		}
		return b.toString();
	}

	static byte[] unbase64(String text)
	{
		int n = text.length();
		if (n % 4 == 1)
		{
			throw new IllegalArgumentException("bad length");
		}
		byte[] out = new byte[n * 3 / 4];
		int bits = 0;
		int count = 0;
		int o = 0;
		for (int i = 0; i < n; i++)
		{
			int v = ALPHABET.indexOf(text.charAt(i));
			if (v < 0)
			{
				throw new IllegalArgumentException("bad character");
			}
			bits = bits << 6 | v;
			count += 6;
			if (count >= 8)
			{
				count -= 8;
				out[o++] = (byte) (bits >> count);
			}
		}
		return out;
	}
}
