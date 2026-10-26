package com.osrssurvivors.map;

import java.awt.image.BufferedImage;

/**
 * Paints the ground of an area from above, following the game's own recipe closely enough to
 * look like it: underlay colours blended over neighbouring tiles, overlays (paths, floors,
 * water) cut into the game's tile shapes, and light and shade from the terrain's heights.
 */
final class TerrainPainter
{
	/** Tiles beyond the arena read for blending colours and lighting the edge. */
	static final int MARGIN = 6;
	private static final int BLEND = 5;
	private static final int VOID = 0x0c0e0a;
	/** For a textured floor whose texture can't be read and whose own colour is a black placeholder; nearly always water. */
	private static final int UNKNOWN_TEXTURE = 0x34557f;
	private static final int[] NO_COLOUR = new int[0];
	/** Overall brightness: the definitions' colours are brighter than the game shows them. */
	private static final double TONE = 0.86;

	/** Which quarter-tile cells of a shape are covered by the overlay (rows north to south), from the game. */
	static final int[][] SHAPES = {
		{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 1, 1},
		{1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1},
		{0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0},
		{0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0},
		{1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, 1},
		{1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
		{0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1},
		{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1},
	};
	static final int[][] ROTATIONS = {
		{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
		{12, 8, 4, 0, 13, 9, 5, 1, 14, 10, 6, 2, 15, 11, 7, 3},
		{15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0},
		{3, 7, 11, 15, 2, 6, 10, 14, 1, 5, 9, 13, 0, 4, 8, 12},
	};
	private static final int[] COSINE = new int[2048];

	static
	{
		for (int i = 0; i < COSINE.length; i++)
		{
			COSINE[i] = (int) (65536.0 * Math.cos(i * (2 * Math.PI / 2048)));
		}
	}

	private final AreaBuilder area;
	private final int size;
	/** Grid covering the arena plus one tile each side: blended underlay RGB (-1 none) and light. */
	private final int span;
	private final int[] blended;
	private final double[] light;

	TerrainPainter(AreaBuilder area)
	{
		this.area = area;
		this.size = area.getSize();
		this.span = size + 2;
		this.blended = new int[span * span];
		this.light = new double[span * span];
	}

	BufferedImage paint()
	{
		computeBlend();
		computeLight();
		int p = AreaBuilder.PIXELS_PER_TILE;
		BufferedImage image = new BufferedImage(size * p, size * p, BufferedImage.TYPE_INT_RGB);
		int[] row = new int[size * p];
		for (int ly = size - 1; ly >= 0; ly--)
		{
			int[][] tileRows = new int[p][];
			for (int v = 0; v < p; v++)
			{
				tileRows[v] = row.clone();
			}
			for (int lx = 0; lx < size; lx++)
			{
				paintTile(lx, ly, tileRows);
			}
			int top = (size - 1 - ly) * p;
			for (int v = 0; v < p; v++)
			{
				image.setRGB(0, top + v, size * p, 1, tileRows[v], 0, size * p);
			}
		}
		return image;
	}

	private void paintTile(int lx, int ly, int[][] rows)
	{
		int p = AreaBuilder.PIXELS_PER_TILE;
		int wx = area.getBaseX() + lx;
		int wy = area.getBaseY() + ly;
		MapSquare s = area.square(wx, wy);
		int level = area.paintLevel(wx, wy);
		int under = s == null ? 0 : s.underlay(level, wx & 63, wy & 63);
		int over = s == null ? 0 : s.overlay(level, wx & 63, wy & 63);
		// corners: south-west, south-east, north-east, north-west
		int sw = grid(lx, ly);
		int se = grid(lx + 1, ly);
		int ne = grid(lx + 1, ly + 1);
		int nw = grid(lx, ly + 1);
		int[] underColour = under == 0 || blended[sw] < 0 ? NO_COLOUR
			: new int[]{blended[sw], or(blended[se], blended[sw]), or(blended[ne], blended[sw]), or(blended[nw], blended[sw])};
		int overColour = over == 0 ? -1 : overlayColour(over - 1);
		int[] mask = null;
		if (overColour >= 0)
		{
			int shape = s.shape(level, wx & 63, wy & 63) + 1;
			mask = shape < SHAPES.length ? SHAPES[shape] : SHAPES[1];
		}
		int[] rotation = s == null ? ROTATIONS[0] : ROTATIONS[s.rotation(level, wx & 63, wy & 63) & 3];
		for (int v = 0; v < p; v++)
		{
			// v counts down from the north edge
			double fy = 1 - (v + 0.5) / p;
			int cellRow = v * 4 / p;
			for (int u = 0; u < p; u++)
			{
				double fx = (u + 0.5) / p;
				double l = bilinear(light[sw], light[se], light[ne], light[nw], fx, fy);
				int rgb;
				boolean overlay = mask != null && mask[rotation[cellRow * 4 + u * 4 / p]] == 1;
				if (overlay)
				{
					rgb = shade(overColour, l);
				}
				else if (underColour.length > 0)
				{
					rgb = shade(mix(underColour, fx, fy), l);
				}
				else
				{
					rgb = VOID;
				}
				rows[v][lx * p + u] = rgb;
			}
		}
	}

	private static int or(int colour, int fallback)
	{
		return colour < 0 ? fallback : colour;
	}

	private int overlayColour(int id)
	{
		FloorDefinition def = area.getSource().overlay(id);
		if (def == null)
		{
			return -1;
		}
		if (def.texture >= 0)
		{
			int t = area.getSource().textureColour(def.texture);
			if (t >= 0)
			{
				return t;
			}
			if (def.rgb == 0)
			{
				return UNKNOWN_TEXTURE;
			}
		}
		return def.rgb == FloorDefinition.INVISIBLE ? -1 : def.rgb;
	}

	private int grid(int lx, int ly)
	{
		return (ly + 1) * span + lx + 1;
	}

	/** Underlay colours averaged over the surrounding tiles, as the game blends its ground. */
	private void computeBlend()
	{
		int n = span + 2 * BLEND;
		int[] r = new int[n * n];
		int[] g = new int[n * n];
		int[] b = new int[n * n];
		int[] c = new int[n * n];
		for (int j = 0; j < n; j++)
		{
			for (int i = 0; i < n; i++)
			{
				int wx = area.getBaseX() - 1 - BLEND + i;
				int wy = area.getBaseY() - 1 - BLEND + j;
				MapSquare s = area.square(wx, wy);
				if (s == null)
				{
					continue;
				}
				int id = s.underlay(area.paintLevel(wx, wy), wx & 63, wy & 63);
				FloorDefinition def = id == 0 ? null : area.getSource().underlay(id - 1);
				if (def != null)
				{
					r[j * n + i] = def.rgb >> 16 & 0xff;
					g[j * n + i] = def.rgb >> 8 & 0xff;
					b[j * n + i] = def.rgb & 0xff;
					c[j * n + i] = 1;
				}
			}
		}
		// box sums: horizontal then vertical
		int[][] channels = {r, g, b, c};
		for (int[] ch : channels)
		{
			boxSum(ch, n);
		}
		for (int j = 0; j < span; j++)
		{
			for (int i = 0; i < span; i++)
			{
				int k = (j + BLEND) * n + i + BLEND;
				blended[j * span + i] = c[k] == 0 ? -1 : (r[k] / c[k]) << 16 | (g[k] / c[k]) << 8 | b[k] / c[k];
			}
		}
	}

	private static void boxSum(int[] ch, int n)
	{
		int[] tmp = new int[n * n];
		for (int j = 0; j < n; j++)
		{
			for (int i = 0; i < n; i++)
			{
				int sum = 0;
				for (int k = Math.max(0, i - BLEND); k <= Math.min(n - 1, i + BLEND); k++)
				{
					sum += ch[j * n + k];
				}
				tmp[j * n + i] = sum;
			}
		}
		for (int j = 0; j < n; j++)
		{
			for (int i = 0; i < n; i++)
			{
				int sum = 0;
				for (int k = Math.max(0, j - BLEND); k <= Math.min(n - 1, j + BLEND); k++)
				{
					sum += tmp[k * n + i];
				}
				ch[j * n + i] = sum;
			}
		}
	}

	/** Brightness per tile corner from the slope of the ground, relative to flat ground. */
	private void computeLight()
	{
		int n = span + 2;
		int[] h = new int[n * n];
		for (int j = 0; j < n; j++)
		{
			for (int i = 0; i < n; i++)
			{
				h[j * n + i] = height(area.getBaseX() - 2 + i, area.getBaseY() - 2 + j);
			}
		}
		double flat = 96 - 10 * 256 / lightDivisor();
		for (int j = 0; j < span; j++)
		{
			for (int i = 0; i < span; i++)
			{
				int k = (j + 1) * n + i + 1;
				int dx = h[k + 1] - h[k - 1];
				int dy = h[k + n] - h[k - n];
				double len = Math.sqrt(dx * dx + 65536 + dy * dy);
				double nx = dx * 256 / len;
				double ny = 65536 / len;
				double nz = dy * 256 / len;
				double l = 96 + (-50 * nx - 10 * ny - 50 * nz) / lightDivisor();
				// softened a little: seen from above, the game's slopes read gentler than its raw light
				light[j * span + i] = TONE * Math.max(0.7, Math.min(1.3, 1 + (l / flat - 1) * 0.75));
			}
		}
	}

	private static double lightDivisor()
	{
		return Math.sqrt(50 * 50 + 10 * 10 + 50 * 50) * 768 / 256;
	}

	/** Ground height at a tile corner, in the game's units (negative is up). */
	private int height(int wx, int wy)
	{
		MapSquare s = area.square(wx, wy);
		int stored = s == null ? MapSquare.NO_HEIGHT : s.height(0, wx & 63, wy & 63);
		if (stored == MapSquare.NO_HEIGHT)
		{
			return -noiseHeight(wx + 932731, wy + 556238) * 8;
		}
		return -(stored == 1 ? 0 : stored) * 8;
	}

	// the game's height noise for tiles without a stored height

	static int noiseHeight(int x, int y)
	{
		int n = interpolatedNoise(x + 45365, y + 91923, 4) - 128
			+ (interpolatedNoise(x + 10294, y + 37821, 2) - 128 >> 1)
			+ (interpolatedNoise(x, y, 1) - 128 >> 2);
		n = (int) (n * 0.3) + 35;
		return Math.max(10, Math.min(60, n));
	}

	private static int interpolatedNoise(int x, int y, int scale)
	{
		int xi = x / scale;
		int xf = x & scale - 1;
		int yi = y / scale;
		int yf = y & scale - 1;
		int a = smoothNoise(xi, yi);
		int b = smoothNoise(xi + 1, yi);
		int c = smoothNoise(xi, yi + 1);
		int d = smoothNoise(xi + 1, yi + 1);
		int i1 = interpolate(a, b, xf, scale);
		int i2 = interpolate(c, d, xf, scale);
		return interpolate(i1, i2, yf, scale);
	}

	private static int interpolate(int a, int b, int t, int scale)
	{
		int f = 65536 - COSINE[t * 1024 / scale] >> 1;
		return (f * b >> 16) + (a * (65536 - f) >> 16);
	}

	private static int smoothNoise(int x, int y)
	{
		int corners = noise(x - 1, y - 1) + noise(x + 1, y - 1) + noise(x - 1, y + 1) + noise(x + 1, y + 1);
		int sides = noise(x - 1, y) + noise(x + 1, y) + noise(x, y - 1) + noise(x, y + 1);
		return noise(x, y) / 4 + sides / 8 + corners / 16;
	}

	private static int noise(int x, int y)
	{
		int n = x + y * 57;
		n ^= n << 13;
		int v = n * (n * n * 15731 + 789221) + 1376312589 & Integer.MAX_VALUE;
		return v >> 19 & 0xff;
	}

	// colour helpers

	private static double bilinear(double sw, double se, double ne, double nw, double fx, double fy)
	{
		double south = sw + (se - sw) * fx;
		double north = nw + (ne - nw) * fx;
		return south + (north - south) * fy;
	}

	/** Bilinear mix of corner colours (south-west, south-east, north-east, north-west). */
	private static int mix(int[] c, double fx, double fy)
	{
		int r = (int) bilinear(c[0] >> 16 & 0xff, c[1] >> 16 & 0xff, c[2] >> 16 & 0xff, c[3] >> 16 & 0xff, fx, fy);
		int g = (int) bilinear(c[0] >> 8 & 0xff, c[1] >> 8 & 0xff, c[2] >> 8 & 0xff, c[3] >> 8 & 0xff, fx, fy);
		int b = (int) bilinear(c[0] & 0xff, c[1] & 0xff, c[2] & 0xff, c[3] & 0xff, fx, fy);
		return r << 16 | g << 8 | b;
	}

	private static int shade(int rgb, double light)
	{
		int r = (int) Math.min(255, (rgb >> 16 & 0xff) * light);
		int g = (int) Math.min(255, (rgb >> 8 & 0xff) * light);
		int b = (int) Math.min(255, (rgb & 0xff) * light);
		return r << 16 | g << 8 | b;
	}
}
