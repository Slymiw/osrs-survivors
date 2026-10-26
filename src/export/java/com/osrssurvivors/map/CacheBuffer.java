package com.osrssurvivors.map;

import java.nio.charset.StandardCharsets;

/** Big-endian reader over a cache file, with the cache's variable-length number formats. */
final class CacheBuffer
{
	private final byte[] data;
	private int pos;

	CacheBuffer(byte[] data)
	{
		this.data = data;
	}

	int remaining()
	{
		return data.length - pos;
	}

	int u8()
	{
		return data[pos++] & 0xff;
	}

	int s8()
	{
		return data[pos++];
	}

	int u16()
	{
		int v = (data[pos] & 0xff) << 8 | data[pos + 1] & 0xff;
		pos += 2;
		return v;
	}

	int s16()
	{
		return (short) u16();
	}

	int u24()
	{
		int v = (data[pos] & 0xff) << 16 | (data[pos + 1] & 0xff) << 8 | data[pos + 2] & 0xff;
		pos += 3;
		return v;
	}

	int i32()
	{
		int v = (data[pos] & 0xff) << 24 | (data[pos + 1] & 0xff) << 16 | (data[pos + 2] & 0xff) << 8 | data[pos + 3] & 0xff;
		pos += 4;
		return v;
	}

	/** One byte below 128, else two bytes minus 32768. */
	int smart()
	{
		return (data[pos] & 0xff) < 128 ? u8() : u16() - 32768;
	}

	/** Two bytes, or four when the top bit is set. */
	int bigSmart()
	{
		if (data[pos] < 0)
		{
			return i32() & 0x7fffffff;
		}
		int v = u16();
		return v == 32767 ? -1 : v;
	}

	String string()
	{
		int start = pos;
		while (data[pos] != 0)
		{
			pos++;
		}
		String s = new String(data, start, pos - start, StandardCharsets.ISO_8859_1);
		pos++;
		return s;
	}

	void skip(int n)
	{
		pos += n;
	}

	/** Skips a parameter table (key-value pairs of ints and strings). */
	void skipParams()
	{
		int n = u8();
		for (int i = 0; i < n; i++)
		{
			boolean isString = u8() == 1;
			skip(3);
			if (isString)
			{
				string();
			}
			else
			{
				skip(4);
			}
		}
	}
}
