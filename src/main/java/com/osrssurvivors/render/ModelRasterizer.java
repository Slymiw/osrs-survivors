package com.osrssurvivors.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Flat-shaded software rendering of a {@link MeshSnapshot} into a sprite, seen from a fixed
 * camera to the south, tilted down by {@code pitch}. Orthographic, painter's algorithm, no
 * antialiasing, which keeps the chunky look of the game's own models.
 */
public final class ModelRasterizer
{
	/** Largest sprite dimension; protects against degenerate meshes. */
	static final int MAX_SIZE = 512;

	private ModelRasterizer()
	{
	}

	/**
	 * The model rotation that makes a south-facing model face {@code facing}.
	 *
	 * @param facing direction in world space, radians (0 = east, PI/2 = south)
	 */
	public static double yawFor(double facing)
	{
		return Math.atan2(-Math.cos(facing), Math.sin(facing));
	}

	/**
	 * @param yaw model rotation about the vertical axis, radians
	 * @param pitch camera tilt below horizontal, radians
	 * @param pixelsPerModelUnit output scale
	 * @return the sprite, or null if nothing is visible
	 */
	public static ActorSprite render(MeshSnapshot mesh, double yaw, double pitch, double pixelsPerModelUnit)
	{
		int vertices = mesh.x.length;
		float[] sx = new float[vertices];
		float[] sy = new float[vertices];
		float[] depth = new float[vertices];
		double cosYaw = Math.cos(yaw);
		double sinYaw = Math.sin(yaw);
		double cosPitch = Math.cos(pitch);
		double sinPitch = Math.sin(pitch);
		for (int i = 0; i < vertices; i++)
		{
			double x = mesh.x[i];
			double y = mesh.y[i];
			double z = mesh.z[i];
			double rx = x * cosYaw + z * sinYaw;
			double rz = -x * sinYaw + z * cosYaw;
			sx[i] = (float) (rx * pixelsPerModelUnit);
			sy[i] = (float) ((y * cosPitch - rz * sinPitch) * pixelsPerModelUnit);
			// larger = further from the camera (north, or lower down)
			depth[i] = (float) (rz * cosPitch + y * sinPitch);
		}

		int faces = mesh.faceCount();
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		float[] faceDepth = new float[faces];
		Integer[] order = new Integer[faces];
		int visible = 0;
		for (int f = 0; f < faces; f++)
		{
			if ((mesh.argb[f] >>> 24) == 0 || !valid(mesh, f, vertices))
			{
				continue;
			}
			for (int v : new int[]{mesh.a[f], mesh.b[f], mesh.c[f]})
			{
				minX = Math.min(minX, sx[v]);
				maxX = Math.max(maxX, sx[v]);
				minY = Math.min(minY, sy[v]);
				maxY = Math.max(maxY, sy[v]);
			}
			faceDepth[f] = depth[mesh.a[f]] + depth[mesh.b[f]] + depth[mesh.c[f]];
			order[visible++] = f;
		}
		if (visible == 0)
		{
			return null;
		}
		order = Arrays.copyOf(order, visible);
		// painter's algorithm: far faces first
		Arrays.sort(order, (p, q) -> Float.compare(faceDepth[q], faceDepth[p]));

		int originX = (int) Math.floor(-minX) + 1;
		int originY = (int) Math.floor(-minY) + 1;
		int width = Math.min(MAX_SIZE, (int) Math.ceil(maxX - minX) + 3);
		int height = Math.min(MAX_SIZE, (int) Math.ceil(maxY - minY) + 3);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		int[] px = new int[3];
		int[] py = new int[3];
		for (int f : order)
		{
			int[] v = {mesh.a[f], mesh.b[f], mesh.c[f]};
			for (int i = 0; i < 3; i++)
			{
				px[i] = Math.round(sx[v[i]]) + originX;
				py[i] = Math.round(sy[v[i]]) + originY;
			}
			g.setColor(new Color(mesh.argb[f], true));
			g.fillPolygon(px, py, 3);
			// outline in the same colour closes the hairline gaps between neighbouring faces
			g.drawPolygon(px, py, 3);
		}
		g.dispose();
		return new ActorSprite(image, originX, originY);
	}

	private static boolean valid(MeshSnapshot mesh, int f, int vertices)
	{
		return mesh.a[f] >= 0 && mesh.a[f] < vertices
			&& mesh.b[f] >= 0 && mesh.b[f] < vertices
			&& mesh.c[f] >= 0 && mesh.c[f] < vertices;
	}
}
