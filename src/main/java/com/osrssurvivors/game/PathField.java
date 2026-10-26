package com.osrssurvivors.game;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Step distances from every tile to the player's tile (8-directional, respecting walls).
 * Enemies that can't see the player walk downhill on it, which takes them around buildings
 * and water. Recomputed only when the player changes tile.
 */
final class PathField
{
	static final int UNREACHABLE = Integer.MAX_VALUE;
	private static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1};
	private static final int[] DY = {0, 0, 1, -1, 1, -1, 1, -1};

	private final Arena arena;
	private final int[] dist;
	private int origin = -1;

	PathField(Arena arena)
	{
		this.arena = arena;
		this.dist = new int[arena.getTilesX() * arena.getTilesY()];
	}

	void update(int tx, int ty)
	{
		int w = arena.getTilesX();
		int o = ty * w + tx;
		if (o == origin || !arena.isWalkable(tx, ty))
		{
			return;
		}
		origin = o;
		Arrays.fill(dist, UNREACHABLE);
		dist[o] = 0;
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		queue.add(o);
		while (!queue.isEmpty())
		{
			int i = queue.poll();
			int x = i % w;
			int y = i / w;
			for (int d = 0; d < 8; d++)
			{
				// steps are symmetric, so walking outwards from the player is the same as towards
				if (arena.canStep(x, y, DX[d], DY[d]))
				{
					int j = (y + DY[d]) * w + x + DX[d];
					if (dist[j] == UNREACHABLE)
					{
						dist[j] = dist[i] + 1;
						queue.add(j);
					}
				}
			}
		}
	}

	int distance(int tx, int ty)
	{
		if (tx < 0 || ty < 0 || tx >= arena.getTilesX() || ty >= arena.getTilesY())
		{
			return UNREACHABLE;
		}
		return dist[ty * arena.getTilesX() + tx];
	}

	/**
	 * Centre of the neighbouring tile that leads closest to the player, or null if this tile has
	 * no way there.
	 */
	double[] nextStep(int tx, int ty)
	{
		int best = distance(tx, ty);
		int bx = 0;
		int by = 0;
		for (int d = 0; d < 8; d++)
		{
			if (arena.canStep(tx, ty, DX[d], DY[d]))
			{
				int nd = distance(tx + DX[d], ty + DY[d]);
				if (nd < best)
				{
					best = nd;
					bx = DX[d];
					by = DY[d];
				}
			}
		}
		if (bx == 0 && by == 0)
		{
			return null;
		}
		return new double[]{(tx + bx + 0.5) * Arena.TILE, (ty + by + 0.5) * Arena.TILE};
	}
}
