package com.osrssurvivors.map;

import com.osrssurvivors.game.Arena;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a square arena around a world tile from cache map data: collision from the tiles' flags
 * and the scenery's footprints, a picture of the ground, and where each piece of scenery stands.
 * Only the ground level is used (bridges included); roofs and upper floors are left out, like
 * the game with roofs hidden. Doors and gates are left open.
 */
public final class AreaBuilder
{
	/** Ground picture resolution. */
	public static final int PIXELS_PER_TILE = 16;
	// placement types
	static final int WALL_STRAIGHT = 0;
	static final int WALL_L = 2;
	static final int WALL_DIAGONAL = 9;
	static final int CENTREPIECE = 10;
	static final int CENTREPIECE_DIAGONAL = 11;
	static final int ROOF_FIRST = 12;
	static final int ROOF_LAST = 21;
	static final int GROUND_DECORATION = 22;

	/** One piece of scenery to draw, at an arena position. */
	public static final class Placement
	{
		public final int objectId;
		public final int type;
		/** Quarter turns, 0-3. */
		public final int orientation;
		public final double x;
		public final double y;

		Placement(int objectId, int type, int orientation, double x, double y)
		{
			this.objectId = objectId;
			this.type = type;
			this.orientation = orientation;
			this.x = x;
			this.y = y;
		}
	}

	public static final class BuiltArea
	{
		public final Arena arena;
		public final BufferedImage ground;
		public final List<Placement> placements;

		BuiltArea(Arena arena, BufferedImage ground, List<Placement> placements)
		{
			this.arena = arena;
			this.ground = ground;
			this.placements = placements;
		}
	}

	private final MapSource source;
	private final int size;
	/** World tile at the arena's south-west corner. */
	private final int baseX;
	private final int baseY;
	private final Map<Integer, MapSquare> squares = new HashMap<>();
	private final Map<Integer, ObjectDefinition> objects = new HashMap<>();

	private AreaBuilder(MapSource source, int centreX, int centreY, int size)
	{
		this.source = source;
		this.size = size;
		this.baseX = centreX - size / 2;
		this.baseY = centreY - size / 2;
	}

	/** The map squares (region ids) needed for an area, including the margin used for ground blending. */
	public static List<Integer> regionsFor(int centreX, int centreY, int size)
	{
		int x0 = centreX - size / 2 - TerrainPainter.MARGIN;
		int y0 = centreY - size / 2 - TerrainPainter.MARGIN;
		int x1 = x0 + size + 2 * TerrainPainter.MARGIN;
		int y1 = y0 + size + 2 * TerrainPainter.MARGIN;
		List<Integer> regions = new ArrayList<>();
		for (int rx = x0 >> 6; rx <= x1 >> 6; rx++)
		{
			for (int ry = y0 >> 6; ry <= y1 >> 6; ry++)
			{
				regions.add(rx << 8 | ry);
			}
		}
		return regions;
	}

	/**
	 * @param size arena edge in tiles
	 * @return the area, or null if the centre isn't on usable ground
	 */
	public static BuiltArea build(MapSource source, int centreX, int centreY, int size)
	{
		return new AreaBuilder(source, centreX, centreY, size).build();
	}

	private BuiltArea build()
	{
		for (int region : regionsFor(baseX + size / 2, baseY + size / 2, size))
		{
			MapSquare s = source.square(region);
			if (s != null)
			{
				squares.put(region, s);
			}
		}
		if (squares.isEmpty())
		{
			return null;
		}

		boolean[] blocked = new boolean[size * size];
		byte[] walls = new byte[size * size];
		for (int ly = 0; ly < size; ly++)
		{
			for (int lx = 0; lx < size; lx++)
			{
				int wx = baseX + lx;
				int wy = baseY + ly;
				int level = groundLevel(wx, wy);
				MapSquare s = square(wx, wy);
				int paint = paintLevel(wx, wy);
				blocked[arenaIndex(lx, ly)] = s == null
					|| (s.flags(level, wx & 63, wy & 63) & MapSquare.FLAG_BLOCKED) != 0
					|| s.underlay(paint, wx & 63, wy & 63) == 0 && s.overlay(paint, wx & 63, wy & 63) == 0;
			}
		}

		List<Placement> placements = new ArrayList<>();
		boolean[] props = new boolean[size * size];
		for (MapSquare s : squares.values())
		{
			int region = regionId(s);
			int ox = (region >> 8) << 6;
			int oy = (region & 0xff) << 6;
			for (MapSquare.Loc loc : s.getLocs())
			{
				int lx = ox + loc.x - baseX;
				int ly = oy + loc.y - baseY;
				if (lx < 0 || ly < 0 || lx >= size || ly >= size || loc.level != groundLevel(ox + loc.x, oy + loc.y))
				{
					continue;
				}
				place(loc, lx, ly, blocked, walls, props, placements);
			}
		}

		int[] start = nearestOpen(blocked, walls);
		if (start == null)
		{
			return null;
		}
		Arena arena = Arena.of(size, size, blocked, walls, props, start[0], start[1]);
		BufferedImage ground = new TerrainPainter(this).paint();
		return new BuiltArea(arena, ground, Collections.unmodifiableList(placements));
	}

	private static void blockByScenery(int i, boolean[] blocked, boolean[] props)
	{
		props[i] |= !blocked[i];
		blocked[i] = true;
	}

	private int regionId(MapSquare s)
	{
		for (Map.Entry<Integer, MapSquare> e : squares.entrySet())
		{
			if (e.getValue() == s)
			{
				return e.getKey();
			}
		}
		throw new IllegalStateException();
	}

	/**
	 * @param props set for tiles blocked by this scenery and not already by the ground
	 */
	private void place(MapSquare.Loc loc, int lx, int ly, boolean[] blocked, byte[] walls, boolean[] props, List<Placement> placements)
	{
		int type = loc.type;
		if (type >= ROOF_FIRST && type <= ROOF_LAST || type > WALL_L && type < WALL_DIAGONAL)
		{
			// roofs, and small decorations hung on walls
			return;
		}
		ObjectDefinition def = definition(loc.id);
		if (def == null)
		{
			return;
		}
		boolean door = def.isDoor();
		boolean solid = def.interactType != ObjectDefinition.INTERACT_NONE && !door;
		int o = loc.orientation;
		if (type == WALL_STRAIGHT || type == WALL_L)
		{
			if (solid)
			{
				addWall(walls, lx, ly, o);
				if (type == WALL_L)
				{
					addWall(walls, lx, ly, o + 1 & 3);
				}
			}
		}
		else if (type == WALL_DIAGONAL || type == CENTREPIECE || type == CENTREPIECE_DIAGONAL)
		{
			if (solid)
			{
				int w = (o & 1) == 0 ? def.sizeX : def.sizeY;
				int h = (o & 1) == 0 ? def.sizeY : def.sizeX;
				for (int dx = 0; dx < w; dx++)
				{
					for (int dy = 0; dy < h; dy++)
					{
						if (lx + dx < size && ly + dy < size)
						{
							blockByScenery(arenaIndex(lx + dx, ly + dy), blocked, props);
						}
					}
				}
			}
		}
		else if (type == GROUND_DECORATION && def.interactType == 1)
		{
			blockByScenery(arenaIndex(lx, ly), blocked, props);
		}

		if (door && def.isClosedDoor() || !def.hasModels())
		{
			return;
		}
		double cx = lx + 0.5;
		double cy = ly + 0.5;
		if (type == CENTREPIECE || type == CENTREPIECE_DIAGONAL)
		{
			cx = lx + ((o & 1) == 0 ? def.sizeX : def.sizeY) / 2.0;
			cy = ly + ((o & 1) == 0 ? def.sizeY : def.sizeX) / 2.0;
		}
		placements.add(new Placement(def.id, type, o, cx * Arena.TILE, (size - cy) * Arena.TILE));
	}

	/** The definition to use for an object id, following its default morph; cached. */
	ObjectDefinition definition(int id)
	{
		if (objects.containsKey(id))
		{
			return objects.get(id);
		}
		ObjectDefinition def = source.object(id);
		if (def != null && !def.hasModels() && def.morphTarget() >= 0)
		{
			ObjectDefinition morph = source.object(def.morphTarget());
			if (morph != null)
			{
				// the morph's look, but keep the base object's footprint rules if the morph has none
				def = morph;
			}
		}
		objects.put(id, def);
		return def;
	}

	/** Adds a wall on one edge of a tile and the matching edge of its neighbour. Edge 0 = west, 1 = north, 2 = east, 3 = south. */
	private void addWall(byte[] walls, int lx, int ly, int edge)
	{
		int[] own = {Arena.WALL_WEST, Arena.WALL_NORTH, Arena.WALL_EAST, Arena.WALL_SOUTH};
		int[] dx = {-1, 0, 1, 0};
		int[] dy = {0, 1, 0, -1};
		walls[arenaIndex(lx, ly)] |= own[edge];
		int nx = lx + dx[edge];
		int ny = ly + dy[edge];
		if (nx >= 0 && ny >= 0 && nx < size && ny < size)
		{
			walls[arenaIndex(nx, ny)] |= own[edge + 2 & 3];
		}
	}

	/** Arena tile index: arena rows run north to south, the map's y runs south to north. */
	private int arenaIndex(int lx, int ly)
	{
		return (size - 1 - ly) * size + lx;
	}

	/** The open tile nearest the centre, as arena (x, row), or null. */
	private int[] nearestOpen(boolean[] blocked, byte[] walls)
	{
		int c = size / 2;
		boolean[] seen = new boolean[size * size];
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		queue.add(new int[]{c, c});
		seen[c * size + c] = true;
		int best = -1;
		int[] bestTile = null;
		while (!queue.isEmpty())
		{
			int[] t = queue.poll();
			int i = t[1] * size + t[0];
			if (!blocked[i])
			{
				// prefer an open spot with room around it over a tile squeezed between walls
				int room = 0;
				for (int dy = -2; dy <= 2; dy++)
				{
					for (int dx = -2; dx <= 2; dx++)
					{
						int x = t[0] + dx;
						int y = t[1] + dy;
						room += x >= 0 && y >= 0 && x < size && y < size && !blocked[y * size + x] && walls[y * size + x] == 0 ? 1 : 0;
					}
				}
				if (room > best)
				{
					best = room;
					bestTile = t;
				}
				if (room >= 20)
				{
					return t;
				}
			}
			if (Math.abs(t[0] - c) > 12 || Math.abs(t[1] - c) > 12)
			{
				continue;
			}
			int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
			for (int[] s : steps)
			{
				int x = t[0] + s[0];
				int y = t[1] + s[1];
				if (x >= 0 && y >= 0 && x < size && y < size && !seen[y * size + x])
				{
					seen[y * size + x] = true;
					queue.add(new int[]{x, y});
				}
			}
		}
		return bestTile;
	}

	// shared with the terrain painter

	int getSize()
	{
		return size;
	}

	int getBaseX()
	{
		return baseX;
	}

	int getBaseY()
	{
		return baseY;
	}

	MapSource getSource()
	{
		return source;
	}

	MapSquare square(int wx, int wy)
	{
		return squares.get(MapSquare.regionOf(wx, wy));
	}

	/**
	 * Level whose ground is drawn at a tile: the bridge's own level when it has ground, else the
	 * ground below (a bridge deck that is only scenery is drawn over the water).
	 */
	int paintLevel(int wx, int wy)
	{
		int level = groundLevel(wx, wy);
		MapSquare s = square(wx, wy);
		if (level == 1 && s.underlay(1, wx & 63, wy & 63) == 0 && s.overlay(1, wx & 63, wy & 63) == 0)
		{
			return 0;
		}
		return level;
	}

	/** Level whose ground is walked on at a tile: 1 under a bridge, else 0. */
	int groundLevel(int wx, int wy)
	{
		MapSquare s = square(wx, wy);
		return s != null && (s.flags(1, wx & 63, wy & 63) & MapSquare.FLAG_BRIDGE) != 0 ? 1 : 0;
	}
}
