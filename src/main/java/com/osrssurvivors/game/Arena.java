package com.osrssurvivors.game;

import java.util.ArrayDeque;

/**
 * The playfield: a grid of {@link #TILE}-sized tiles, some blocked, some with walls along their
 * edges, all in world units with the origin at the top-left (y grows southwards). Either an open
 * field, or built from a real game area. Characters collide with it as axis-aligned boxes
 * against a finer grid of solid cells, so walls can be thin. Scenery tiles (trees, rocks) are
 * solid only in their middle, so it's easy to weave between them.
 */
public final class Arena
{
	/** World units per tile; a game tile. */
	public static final double TILE = 32;
	/** Collision cells per tile edge. */
	static final int SUB = 4;
	static final double CELL = TILE / SUB;
	private static final double EPS = 1e-6;
	/** Cells trimmed off each open side of a scenery tile. */
	static final int PROP_INSET = 1;
	/** How far a character is nudged sideways to get round a corner it walks into. */
	static final double CORNER_SLIP = 10;

	public static final int WALL_NORTH = 1;
	public static final int WALL_EAST = 2;
	public static final int WALL_SOUTH = 4;
	public static final int WALL_WEST = 8;

	private final int tilesX;
	private final int tilesY;
	/** Tiles nothing can stand on (including everything not reachable from the start). */
	private final boolean[] blocked;
	private final byte[] walls;
	/** Blocked tiles that hold scenery rather than water or void; null if none. */
	private final boolean[] props;
	private final boolean[] solid;
	private final int startTileX;
	private final int startTileY;
	private final boolean open;
	private final int walkableTiles;

	private Arena(int tilesX, int tilesY, boolean[] blocked, byte[] walls, boolean[] props, int startTileX, int startTileY, boolean open)
	{
		this.tilesX = tilesX;
		this.tilesY = tilesY;
		this.walls = walls;
		this.props = props;
		this.startTileX = startTileX;
		this.startTileY = startTileY;
		this.open = open;
		this.blocked = open ? blocked : reachableOnly(tilesX, tilesY, blocked, walls, startTileX, startTileY);
		int walkable = 0;
		for (boolean b : this.blocked)
		{
			walkable += b ? 0 : 1;
		}
		this.walkableTiles = walkable;
		this.solid = buildSolid();
	}

	/** An empty field of grass with the start in the middle. */
	public static Arena open(int tilesX, int tilesY)
	{
		return new Arena(tilesX, tilesY, new boolean[tilesX * tilesY], new byte[tilesX * tilesY], null, tilesX / 2, tilesY / 2, true);
	}

	/**
	 * An arena from a real area. Tiles that can't be reached from the start tile are blocked too,
	 * so nothing spawns inside closed buildings.
	 *
	 * @param blocked per tile, row-major, row 0 = north
	 * @param walls per tile, {@link #WALL_NORTH} etc.
	 */
	public static Arena of(int tilesX, int tilesY, boolean[] blocked, byte[] walls, int startTileX, int startTileY)
	{
		return of(tilesX, tilesY, blocked, walls, null, startTileX, startTileY);
	}

	/**
	 * @param props per tile, whether its blocking comes from scenery (a tree, a rock); such tiles
	 * collide with a smaller box. May be null.
	 */
	public static Arena of(int tilesX, int tilesY, boolean[] blocked, byte[] walls, boolean[] props, int startTileX, int startTileY)
	{
		boolean[] b = blocked.clone();
		b[startTileY * tilesX + startTileX] = false;
		return new Arena(tilesX, tilesY, b, walls.clone(), props == null ? null : props.clone(), startTileX, startTileY, false);
	}

	public int getTilesX()
	{
		return tilesX;
	}

	public int getTilesY()
	{
		return tilesY;
	}

	public double getWidth()
	{
		return tilesX * TILE;
	}

	public double getHeight()
	{
		return tilesY * TILE;
	}

	/** True for the plain field, where nothing ever blocks the way. */
	public boolean isOpen()
	{
		return open;
	}

	public int getWalkableTiles()
	{
		return walkableTiles;
	}

	public double getStartX()
	{
		return (startTileX + 0.5) * TILE;
	}

	public double getStartY()
	{
		return (startTileY + 0.5) * TILE;
	}

	public boolean isWalkable(int tx, int ty)
	{
		return tx >= 0 && ty >= 0 && tx < tilesX && ty < tilesY && !blocked[ty * tilesX + tx];
	}

	public int getStartTileX()
	{
		return startTileX;
	}

	public int getStartTileY()
	{
		return startTileY;
	}

	/** {@link #WALL_NORTH} etc. on a tile inside the arena. */
	public int getWalls(int tx, int ty)
	{
		return walls[ty * tilesX + tx];
	}

	/** Whether a tile inside the arena is blocked by scenery rather than water or void. */
	public boolean isPropTile(int tx, int ty)
	{
		return props != null && props[ty * tilesX + tx];
	}

	public static int tileOf(double v)
	{
		return (int) Math.floor(v / TILE);
	}

	/** Whether a point is inside a wall, a blocked tile or outside the arena. */
	public boolean isSolidAt(double x, double y)
	{
		return solidCell(cell(x), cell(y));
	}

	/** Whether a box of half-size {@code r} at (x, y) overlaps anything solid. */
	public boolean isSolidBox(double x, double y, double r)
	{
		for (int cy = cell(y - r); cy <= cell(y + r - EPS); cy++)
		{
			for (int cx = cell(x - r); cx <= cell(x + r - EPS); cx++)
			{
				if (solidCell(cx, cy))
				{
					return true;
				}
			}
		}
		return false;
	}

	/** New x after moving a box of half-size {@code r} horizontally by {@code dx}, stopping at walls. */
	public double moveX(double x, double y, double dx, double r)
	{
		if (dx == 0)
		{
			return x;
		}
		double nx = x + dx;
		int cy0 = cell(y - r);
		int cy1 = cell(y + r - EPS);
		if (dx > 0)
		{
			for (int cx = cell(x + r - EPS) + 1; cx <= cell(nx + r - EPS); cx++)
			{
				if (columnSolid(cx, cy0, cy1))
				{
					return Math.max(x, cx * CELL - r);
				}
			}
		}
		else
		{
			for (int cx = cell(x - r) - 1; cx >= cell(nx - r); cx--)
			{
				if (columnSolid(cx, cy0, cy1))
				{
					return Math.min(x, (cx + 1) * CELL + r);
				}
			}
		}
		return nx;
	}

	/** New y after moving a box of half-size {@code r} vertically by {@code dy}, stopping at walls. */
	public double moveY(double x, double y, double dy, double r)
	{
		if (dy == 0)
		{
			return y;
		}
		double ny = y + dy;
		int cx0 = cell(x - r);
		int cx1 = cell(x + r - EPS);
		if (dy > 0)
		{
			for (int cy = cell(y + r - EPS) + 1; cy <= cell(ny + r - EPS); cy++)
			{
				if (rowSolid(cy, cx0, cx1))
				{
					return Math.max(y, cy * CELL - r);
				}
			}
		}
		else
		{
			for (int cy = cell(y - r) - 1; cy >= cell(ny - r); cy--)
			{
				if (rowSolid(cy, cx0, cx1))
				{
					return Math.min(y, (cy + 1) * CELL + r);
				}
			}
		}
		return ny;
	}

	/**
	 * Sideways shift that lets a box of half-size {@code r}, walking along one axis into a corner,
	 * slip past it: the smallest offset up to {@link #CORNER_SLIP} after which a small step forward
	 * is clear, or 0 if there's none (a flat wall, or open ground).
	 *
	 * @param dirX -1, 0 or 1; exactly one of dirX and dirY is non-zero
	 */
	public double cornerSlip(double x, double y, int dirX, int dirY, double r)
	{
		if (open || !isSolidBox(x + dirX, y + dirY, r))
		{
			return 0;
		}
		for (double d = 1; d <= CORNER_SLIP; d++)
		{
			for (int sign = -1; sign <= 1; sign += 2)
			{
				double sx = x + (dirX == 0 ? sign * d : 0);
				double sy = y + (dirY == 0 ? sign * d : 0);
				if (!isSolidBox(sx, sy, r) && !isSolidBox(sx + dirX, sy + dirY, r))
				{
					return sign * d;
				}
			}
		}
		return 0;
	}

	/** Whether a straight line between two points crosses nothing solid (sampled every half cell). */
	public boolean clearLine(double x0, double y0, double x1, double y1)
	{
		if (open)
		{
			return true;
		}
		double len = Math.hypot(x1 - x0, y1 - y0);
		int steps = (int) Math.ceil(len / (CELL / 2));
		for (int i = 1; i < steps; i++)
		{
			double t = i / (double) steps;
			if (isSolidAt(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t))
			{
				return false;
			}
		}
		return true;
	}

	/** Whether one may step from a tile to a neighbouring one (8 directions; diagonals need both sides clear). */
	boolean canStep(int tx, int ty, int dx, int dy)
	{
		if (!isWalkable(tx + dx, ty + dy))
		{
			return false;
		}
		if (dx != 0 && dy != 0)
		{
			return canStep(tx, ty, dx, 0) && canStep(tx, ty, 0, dy)
				&& canStep(tx + dx, ty, 0, dy) && canStep(tx, ty + dy, dx, 0);
		}
		int w = walls[ty * tilesX + tx];
		int nw = walls[(ty + dy) * tilesX + tx + dx];
		if (dx == 1)
		{
			return (w & WALL_EAST) == 0 && (nw & WALL_WEST) == 0;
		}
		if (dx == -1)
		{
			return (w & WALL_WEST) == 0 && (nw & WALL_EAST) == 0;
		}
		if (dy == 1)
		{
			return (w & WALL_SOUTH) == 0 && (nw & WALL_NORTH) == 0;
		}
		return (w & WALL_NORTH) == 0 && (nw & WALL_SOUTH) == 0;
	}

	private static int cell(double v)
	{
		return (int) Math.floor(v / CELL);
	}

	private boolean columnSolid(int cx, int cy0, int cy1)
	{
		for (int cy = cy0; cy <= cy1; cy++)
		{
			if (solidCell(cx, cy))
			{
				return true;
			}
		}
		return false;
	}

	private boolean rowSolid(int cy, int cx0, int cx1)
	{
		for (int cx = cx0; cx <= cx1; cx++)
		{
			if (solidCell(cx, cy))
			{
				return true;
			}
		}
		return false;
	}

	private boolean solidCell(int cx, int cy)
	{
		int w = tilesX * SUB;
		if (cx < 0 || cy < 0 || cx >= w || cy >= tilesY * SUB)
		{
			return true;
		}
		return solid[cy * w + cx];
	}

	private boolean[] buildSolid()
	{
		int w = tilesX * SUB;
		boolean[] s = new boolean[w * tilesY * SUB];
		for (int ty = 0; ty < tilesY; ty++)
		{
			for (int tx = 0; tx < tilesX; tx++)
			{
				int i = ty * tilesX + tx;
				int x0 = tx * SUB;
				int y0 = ty * SUB;
				// scenery is trimmed on the sides facing open ground, so neighbouring solid tiles stay joined
				boolean prop = props != null && props[i] && blocked[i];
				int west = prop && !blockedTile(tx - 1, ty) ? PROP_INSET : 0;
				int east = prop && !blockedTile(tx + 1, ty) ? SUB - PROP_INSET : SUB;
				int north = prop && !blockedTile(tx, ty - 1) ? PROP_INSET : 0;
				int south = prop && !blockedTile(tx, ty + 1) ? SUB - PROP_INSET : SUB;
				for (int sy = 0; sy < SUB; sy++)
				{
					for (int sx = 0; sx < SUB; sx++)
					{
						boolean core = !prop || sx >= west && sx < east && sy >= north && sy < south;
						boolean wall = blocked[i] && core
							|| (walls[i] & WALL_NORTH) != 0 && sy == 0
							|| (walls[i] & WALL_SOUTH) != 0 && sy == SUB - 1
							|| (walls[i] & WALL_WEST) != 0 && sx == 0
							|| (walls[i] & WALL_EAST) != 0 && sx == SUB - 1;
						s[(y0 + sy) * w + x0 + sx] = wall;
					}
				}
			}
		}
		return s;
	}

	private boolean blockedTile(int tx, int ty)
	{
		return tx < 0 || ty < 0 || tx >= tilesX || ty >= tilesY || blocked[ty * tilesX + tx];
	}

	private static boolean[] reachableOnly(int tilesX, int tilesY, boolean[] blocked, byte[] walls, int sx, int sy)
	{
		// flood fill with a temporary open-walls arena to reuse canStep
		Arena probe = new Arena(tilesX, tilesY, blocked, walls, null, sx, sy, true);
		boolean[] seen = new boolean[tilesX * tilesY];
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		seen[sy * tilesX + sx] = true;
		queue.add(sy * tilesX + sx);
		while (!queue.isEmpty())
		{
			int i = queue.poll();
			int tx = i % tilesX;
			int ty = i / tilesX;
			for (int d = 0; d < 4; d++)
			{
				int dx = d == 0 ? 1 : d == 1 ? -1 : 0;
				int dy = d == 2 ? 1 : d == 3 ? -1 : 0;
				int j = (ty + dy) * tilesX + tx + dx;
				if (probe.canStep(tx, ty, dx, dy) && !seen[j])
				{
					seen[j] = true;
					queue.add(j);
				}
			}
		}
		boolean[] result = new boolean[blocked.length];
		for (int i = 0; i < result.length; i++)
		{
			result[i] = !seen[i];
		}
		return result;
	}
}
