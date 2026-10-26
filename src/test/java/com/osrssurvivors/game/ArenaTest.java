package com.osrssurvivors.game;

import static com.osrssurvivors.game.TestSessions.EPS;
import static com.osrssurvivors.game.TestSessions.run;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.Random;
import org.junit.Test;

/** Real-area arenas: blocked tiles, walls, reachability, and enemies finding their way. */
public class ArenaTest
{
	private static final double T = Arena.TILE;

	/**
	 * Builds an arena from a picture: '#' blocked, '.' open, 'S' start. Walls are added
	 * separately.
	 */
	private static Arena map(String... rows)
	{
		int w = rows[0].length();
		int h = rows.length;
		boolean[] blocked = new boolean[w * h];
		int sx = 0;
		int sy = 0;
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				char c = rows[y].charAt(x);
				blocked[y * w + x] = c == '#';
				if (c == 'S')
				{
					sx = x;
					sy = y;
				}
			}
		}
		return Arena.of(w, h, blocked, new byte[w * h], sx, sy);
	}

	@Test
	public void startsOnTheStartTile()
	{
		Arena a = map(
			".....",
			"..S..",
			".....");
		assertEquals(2.5 * T, a.getStartX(), EPS);
		assertEquals(1.5 * T, a.getStartY(), EPS);
		assertEquals(15, a.getWalkableTiles());
		assertFalse(a.isOpen());
	}

	@Test
	public void blockedTilesStopMovement()
	{
		Arena a = map(
			"S.#..",
			".....");
		double r = Player.COLLISION_RADIUS;
		double x = 0.5 * T;
		double y = 0.5 * T;
		// walking east stops at the face of the blocked tile
		double nx = a.moveX(x, y, 100, r);
		assertEquals(2 * T - r, nx, EPS);
		// a row below is clear
		assertEquals(x + 100, a.moveX(x, 1.5 * T, 100, r), EPS);
		// the arena edge stops too
		assertEquals(r, a.moveX(x, y, -100, r), EPS);
		assertEquals(r, a.moveY(x, y, -100, r), EPS);
	}

	@Test
	public void wallsBlockOnlyTheirEdge()
	{
		int w = 3;
		boolean[] blocked = new boolean[w * 2];
		byte[] walls = new byte[w * 2];
		walls[0] = Arena.WALL_EAST;
		Arena a = Arena.of(w, 2, blocked, walls, 0, 0);
		double r = 6;
		double x = 0.5 * T;
		double y = 0.5 * T;
		double nx = a.moveX(x, y, 50, r);
		assertTrue("the wall stops it inside the tile", nx < T);
		assertTrue("but not before the wall", nx > x);
		assertEquals("no wall in the row below", x + 50, a.moveX(x, 1.5 * T, 50, r), EPS);
		assertTrue(a.canStep(0, 1, 1, 0));
		assertFalse(a.canStep(0, 0, 1, 0));
		assertFalse("walls block from either side", a.canStep(1, 0, -1, 0));
	}

	@Test
	public void unreachableTilesCountAsBlocked()
	{
		Arena a = map(
			"S.#..",
			"..#..",
			"###..");
		assertTrue(a.isWalkable(1, 1));
		assertFalse("cut off by the blocked column", a.isWalkable(3, 0));
		assertEquals(4, a.getWalkableTiles());
		assertTrue(a.isSolidAt(3.5 * T, 0.5 * T));
	}

	@Test
	public void lineOfSight()
	{
		Arena a = map(
			"S....",
			"..#..",
			".....");
		assertTrue(a.clearLine(0.5 * T, 0.5 * T, 4.5 * T, 0.5 * T));
		assertFalse(a.clearLine(0.5 * T, 1.5 * T, 4.5 * T, 1.5 * T));
	}

	@Test
	public void pathsGoAroundObstacles()
	{
		Arena a = map(
			"S...",
			"###.",
			"....");
		PathField field = new PathField(a);
		field.update(0, 0);
		assertEquals(0, field.distance(0, 0));
		// from the bottom-left the only way is round the right end of the wall (no corner cutting)
		assertEquals(8, field.distance(0, 2));
		double[] next = field.nextStep(0, 2);
		assertNotNull(next);
		assertEquals("first step heads east along the bottom", 1.5 * T, next[0], EPS);
	}

	@Test
	public void diagonalsDontCutCorners()
	{
		Arena a = map(
			"S#",
			"#.");
		// the far corner is sealed off: only diagonal access, which would cut both corners
		assertEquals(1, a.getWalkableTiles());
	}

	@Test
	public void enemiesWalkAroundWallsToReachThePlayer()
	{
		Arena a = map(
			"..........",
			"..........",
			"....S.....",
			"..........",
			"#########.",
			"..........",
			"..........");
		GameSession s = new GameSession(new Random(1), a);
		s.disableSpawning();
		Enemy e = s.spawnEnemyAt(1.5 * T, 6.5 * T, 1e9);
		run(s, 10);
		double dist = Math.hypot(e.x - s.getPlayer().x, e.y - s.getPlayer().y);
		assertTrue("the enemy found the gap and reached the player: " + dist, dist < Enemy.RADIUS + Player.RADIUS + 2);
		assertFalse("and never walked through the wall", a.isSolidAt(e.x, e.y));
	}

	@Test
	public void spawnsOnlyOnOpenReachableGround()
	{
		Arena a = map(
			"....................",
			"....................",
			"....................",
			".........S..........",
			"....................",
			"######...######.....",
			"#....#...#....#.....",
			"#....#####....#.....",
			"######...######.....");
		GameSession s = new GameSession(new Random(2), a);
		s.setVisibleArea(300, 200);
		run(s, 20);
		assertFalse(s.getEnemies().isEmpty());
		for (Enemy e : s.getEnemies())
		{
			assertTrue(a.isWalkable(Arena.tileOf(e.x), Arena.tileOf(e.y)));
		}
	}

	@Test
	public void playerCollidesInPlay()
	{
		Arena a = map(
			"S..#",
			"....");
		GameSession s = new GameSession(new Random(1), a);
		s.disableSpawning();
		s.setMoveDirection(1, 0);
		run(s, 3);
		assertEquals(3 * T - Player.COLLISION_RADIUS, s.getPlayer().getX(), EPS);
	}

	/** Like {@link #map}, but 'T' is a tile blocked by scenery (a tree) rather than the ground. */
	private static Arena scenery(String... rows)
	{
		int w = rows[0].length();
		int h = rows.length;
		boolean[] blocked = new boolean[w * h];
		boolean[] props = new boolean[w * h];
		int sx = 0;
		int sy = 0;
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				char c = rows[y].charAt(x);
				blocked[y * w + x] = c == '#' || c == 'T';
				props[y * w + x] = c == 'T';
				if (c == 'S')
				{
					sx = x;
					sy = y;
				}
			}
		}
		return Arena.of(w, h, blocked, new byte[w * h], props, sx, sy);
	}

	@Test
	public void sceneryCollidesWithASmallerBox()
	{
		Arena a = scenery(
			"S.T..",
			".....");
		double r = Player.COLLISION_RADIUS;
		double inset = Arena.PROP_INSET * Arena.CELL;
		// walking east stops at the tree's trunk, not at the edge of its tile
		assertEquals(2 * T + inset - r, a.moveX(0.5 * T, 0.5 * T, 100, r), EPS);
		assertFalse("the edge of the tile is open", a.isSolidAt(2 * T + 1, 0.5 * T));
		assertTrue("the middle isn't", a.isSolidAt(2.5 * T, 0.5 * T));
		assertFalse("still never walkable for pathing", a.isWalkable(2, 0));
	}

	@Test
	public void neighbouringSceneryStaysJoined()
	{
		Arena a = scenery(
			"S.TT.",
			".....");
		assertTrue("no gap between two trees side by side", a.isSolidAt(3 * T, 0.5 * T));
		// but water, or anything else blocked by the ground, is solid to its edge
		Arena water = scenery(
			"S.#..",
			".....");
		assertTrue(water.isSolidAt(2 * T + 1, 0.5 * T));
	}

	@Test
	public void walkingIntoACornerSlipsRoundIt()
	{
		Arena a = scenery(
			".....",
			"S..T.",
			".....");
		double r = Player.COLLISION_RADIUS;
		double inset = Arena.PROP_INSET * Arena.CELL;
		// just catching the tree's top corner: nudged north, round it
		double y = 1 * T + inset - r + 3;
		double x = 3 * T + inset - r;
		assertTrue(a.isSolidBox(x + 1, y, r));
		double slip = a.cornerSlip(x, y, 1, 0, r);
		assertTrue("slips north: " + slip, slip < 0 && slip >= -Arena.CORNER_SLIP);
		assertFalse(a.isSolidBox(x + 1, y + slip, r));
		// square on, there's nowhere to slip to
		assertEquals(0, a.cornerSlip(x, 1.5 * T, 1, 0, r), EPS);
	}

	@Test
	public void playerSlipsRoundSceneryInPlay()
	{
		Arena a = scenery(
			"......",
			"S..T..",
			"......");
		GameSession s = new GameSession(new Random(1), a);
		s.disableSpawning();
		Player p = s.getPlayer();
		// a little off the tree's centre line, walking straight at it
		p.y = 1.5 * T - 10;
		s.setMoveDirection(1, 0);
		run(s, 3);
		assertTrue("got past the tree: " + p.getX(), p.getX() > 4 * T);
	}
}
