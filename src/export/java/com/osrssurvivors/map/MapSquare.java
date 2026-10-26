package com.osrssurvivors.map;

import java.util.ArrayList;
import java.util.List;

/**
 * One 64x64-tile map square, decoded from the client cache: per-tile ground (heights, underlay
 * and overlay ids, overlay shapes, flags) on each of the four levels, and the scenery placed on it.
 */
public final class MapSquare
{
	public static final int SIZE = 64;
	public static final int LEVELS = 4;
	/** Tile flag: unwalkable (water, cliffs). */
	public static final int FLAG_BLOCKED = 1;
	/** Tile flag on level 1: a bridge; level 1's ground is shown and walked at level 0. */
	public static final int FLAG_BRIDGE = 2;
	/** Marks a tile whose height wasn't stored and comes from the game's noise function. */
	public static final int NO_HEIGHT = Integer.MIN_VALUE;

	/** A scenery object placed on a tile. Coordinates are within the square. */
	public static final class Loc
	{
		public final int id;
		public final int level;
		public final int x;
		public final int y;
		public final int type;
		public final int orientation;

		Loc(int id, int level, int x, int y, int type, int orientation)
		{
			this.id = id;
			this.level = level;
			this.x = x;
			this.y = y;
			this.type = type;
			this.orientation = orientation;
		}
	}

	final int[] height = new int[LEVELS * SIZE * SIZE];
	final short[] underlay = new short[LEVELS * SIZE * SIZE];
	final short[] overlay = new short[LEVELS * SIZE * SIZE];
	final byte[] shape = new byte[LEVELS * SIZE * SIZE];
	final byte[] rotation = new byte[LEVELS * SIZE * SIZE];
	final byte[] flags = new byte[LEVELS * SIZE * SIZE];
	final List<Loc> locs = new ArrayList<>();

	static int index(int level, int x, int y)
	{
		return (level * SIZE + x) * SIZE + y;
	}

	/** Region id of the square containing a world tile. */
	public static int regionOf(int worldX, int worldY)
	{
		return (worldX >> 6) << 8 | worldY >> 6;
	}

	public int height(int level, int x, int y)
	{
		return height[index(level, x, y)];
	}

	public int underlay(int level, int x, int y)
	{
		return underlay[index(level, x, y)];
	}

	public int overlay(int level, int x, int y)
	{
		return overlay[index(level, x, y)];
	}

	public int shape(int level, int x, int y)
	{
		return shape[index(level, x, y)];
	}

	public int rotation(int level, int x, int y)
	{
		return rotation[index(level, x, y)];
	}

	public int flags(int level, int x, int y)
	{
		return flags[index(level, x, y)];
	}

	public List<Loc> getLocs()
	{
		return locs;
	}

	/**
	 * @param terrain the square's ground file
	 * @param locations its scenery file, or null for none
	 * @throws IllegalArgumentException if the ground data doesn't decode
	 */
	public static MapSquare decode(byte[] terrain, byte[] locations)
	{
		MapSquare s = new MapSquare();
		try
		{
			s.decodeTerrain(new CacheBuffer(terrain));
		}
		catch (RuntimeException e)
		{
			throw new IllegalArgumentException("bad map square", e);
		}
		if (locations != null)
		{
			try
			{
				s.decodeLocs(new CacheBuffer(locations));
			}
			catch (RuntimeException e)
			{
				// keep the scenery read so far
			}
		}
		return s;
	}

	private void decodeTerrain(CacheBuffer b)
	{
		for (int level = 0; level < LEVELS; level++)
		{
			for (int x = 0; x < SIZE; x++)
			{
				for (int y = 0; y < SIZE; y++)
				{
					int i = index(level, x, y);
					height[i] = NO_HEIGHT;
					while (true)
					{
						int a = b.u16();
						if (a == 0)
						{
							break;
						}
						if (a == 1)
						{
							height[i] = b.u8();
							break;
						}
						if (a <= 49)
						{
							overlay[i] = (short) b.s16();
							shape[i] = (byte) ((a - 2) / 4);
							rotation[i] = (byte) (a - 2 & 3);
						}
						else if (a <= 81)
						{
							flags[i] = (byte) (a - 49);
						}
						else
						{
							underlay[i] = (short) (a - 81);
						}
					}
				}
			}
		}
	}

	private void decodeLocs(CacheBuffer b)
	{
		int id = -1;
		while (true)
		{
			int inc = b.smart();
			while (inc == 32767)
			{
				id += 32767;
				inc = b.smart();
			}
			if (inc == 0)
			{
				return;
			}
			id += inc;
			int pos = 0;
			while (true)
			{
				int d = b.smart();
				if (d == 0)
				{
					break;
				}
				pos += d - 1;
				int attributes = b.u8();
				locs.add(new Loc(id, pos >> 12 & 3, pos >> 6 & 63, pos & 63, attributes >> 2, attributes & 3));
			}
		}
	}
}
