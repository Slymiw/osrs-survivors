package com.osrssurvivors.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Flat-shaded software rendering of a {@link MeshSnapshot} into a sprite, seen from a fixed
 * camera to the south, tilted down by {@code pitch}. Orthographic, painter's algorithm, no
 * antialiasing, which keeps the chunky look of the game's own models. Meshes with textured faces
 * (worn capes) are filled pixel by pixel instead, so their textures show and flow.
 */
public final class ModelRasterizer
{
	/** Faces wound this way round on screen face away and aren't drawn, as in the game's renderer. */
	static final float CULL_SIGN = 1;
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
		if (mesh.textures != null)
		{
			fill(image, mesh, order, sx, sy, originX, originY);
			return new ActorSprite(image, originX, originY);
		}
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

	/**
	 * Fills the faces far to near into the image's pixels: coloured faces in their colour,
	 * textured ones with their texture (scrolled, and lit at each corner). Pixels within a little
	 * of a face's edge count as inside, as the outline does for the plain path.
	 */
	private static void fill(BufferedImage image, MeshSnapshot mesh, Integer[] order, float[] sx, float[] sy, int originX, int originY)
	{
		int w = image.getWidth();
		int h = image.getHeight();
		int[] pixels = new int[w * h];
		int[] textured = new int[mesh.faceCount()];
		Arrays.fill(textured, -1);
		FaceTextures tex = mesh.textures;
		for (int i = 0; i < tex.faces.length; i++)
		{
			if (tex.faces[i] >= 0 && tex.faces[i] < textured.length && tex.textures[i] != null)
			{
				textured[tex.faces[i]] = i;
			}
		}
		for (int f : order)
		{
			float x0 = Math.round(sx[mesh.a[f]]) + originX;
			float y0 = Math.round(sy[mesh.a[f]]) + originY;
			float x1 = Math.round(sx[mesh.b[f]]) + originX;
			float y1 = Math.round(sy[mesh.b[f]]) + originY;
			float x2 = Math.round(sx[mesh.c[f]]) + originX;
			float y2 = Math.round(sy[mesh.c[f]]) + originY;
			float area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
			if (area * CULL_SIGN >= 0)
			{
				continue;
			}
			// edge lengths, for distances in pixels
			float l0 = (float) Math.hypot(x2 - x1, y2 - y1);
			float l1 = (float) Math.hypot(x0 - x2, y0 - y2);
			float l2 = (float) Math.hypot(x1 - x0, y1 - y0);
			int minX = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2)) - 1));
			int maxX = Math.min(w - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2)) + 1));
			int minY = Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2)) - 1));
			int maxY = Math.min(h - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2)) + 1));
			int argb = mesh.argb[f];
			int alpha = argb >>> 24;
			int t = textured[f];
			Texture texture = t < 0 ? null : tex.textures[t];
			for (int py = minY; py <= maxY; py++)
			{
				for (int px = minX; px <= maxX; px++)
				{
					float cx = px + 0.5f;
					float cy = py + 0.5f;
					// barycentric weights of the pixel centre
					float b0 = ((x1 - cx) * (y2 - cy) - (x2 - cx) * (y1 - cy)) / area;
					float b1 = ((x2 - cx) * (y0 - cy) - (x0 - cx) * (y2 - cy)) / area;
					float b2 = 1 - b0 - b1;
					float d = Math.min(b0 * Math.abs(area) / Math.max(l0, 1e-3f),
						Math.min(b1 * Math.abs(area) / Math.max(l1, 1e-3f), b2 * Math.abs(area) / Math.max(l2, 1e-3f)));
					if (d < -0.75f)
					{
						continue;
					}
					int colour = argb & 0xffffff;
					if (texture != null)
					{
						float c0 = Math.max(0, Math.min(1, b0));
						float c1 = Math.max(0, Math.min(1, b1));
						float c2 = Math.max(0, Math.min(1, b2));
						float sum = c0 + c1 + c2;
						c0 /= sum;
						c1 /= sum;
						c2 /= sum;
						float u = tex.uv[t * 6] * c0 + tex.uv[t * 6 + 2] * c1 + tex.uv[t * 6 + 4] * c2;
						float v = tex.uv[t * 6 + 1] * c0 + tex.uv[t * 6 + 3] * c1 + tex.uv[t * 6 + 5] * c2;
						if (texture.alongV())
						{
							v += tex.scroll[t];
						}
						else
						{
							u += tex.scroll[t];
						}
						int n = texture.size;
						int texel = texture.texels[Math.floorMod((int) Math.floor(v * n), n) * n + Math.floorMod((int) Math.floor(u * n), n)];
						if (texel == 0)
						{
							// see-through
							continue;
						}
						float light = tex.light[t * 3] * c0 + tex.light[t * 3 + 1] * c1 + tex.light[t * 3 + 2] * c2;
						colour = shade(texel, light);
					}
					int i = py * w + px;
					pixels[i] = alpha >= 255 || pixels[i] >>> 24 == 0 ? alpha << 24 | colour : blend(pixels[i], colour, alpha);
				}
			}
		}
		image.setRGB(0, 0, w, h, pixels, 0, w);
	}

	/** A colour lit by {@code factor} (1 is unchanged), kept from going black or white, as the export does. */
	private static int shade(int rgb, float factor)
	{
		float k = Math.max(0.3f, Math.min(1.3f, factor));
		int r = (int) Math.min(255, (rgb >> 16 & 0xff) * k);
		int g = (int) Math.min(255, (rgb >> 8 & 0xff) * k);
		int b = (int) Math.min(255, (rgb & 0xff) * k);
		return r << 16 | g << 8 | b;
	}

	private static int blend(int under, int colour, int alpha)
	{
		int r = ((colour >> 16 & 0xff) * alpha + (under >> 16 & 0xff) * (255 - alpha)) / 255;
		int g = ((colour >> 8 & 0xff) * alpha + (under >> 8 & 0xff) * (255 - alpha)) / 255;
		int b = ((colour & 0xff) * alpha + (under & 0xff) * (255 - alpha)) / 255;
		return 0xff000000 | r << 16 | g << 8 | b;
	}

	private static boolean valid(MeshSnapshot mesh, int f, int vertices)
	{
		return mesh.a[f] >= 0 && mesh.a[f] < vertices
			&& mesh.b[f] >= 0 && mesh.b[f] < vertices
			&& mesh.c[f] >= 0 && mesh.c[f] < vertices;
	}
}
