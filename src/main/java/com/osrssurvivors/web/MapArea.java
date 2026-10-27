package com.osrssurvivors.web;

import com.osrssurvivors.game.Arena;
import com.osrssurvivors.render.MeshSnapshot;
import com.osrssurvivors.render.Terrain;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * A map location as exported (WebExport.exportLocations): collision, the painted ground and the
 * scenery, each prop pointing at a lit mesh already turned to its orientation.
 */
final class MapArea
{
	/** Must match WebExport.MODELS_VERSION. */
	private static final int VERSION = 2;

	final Arena arena;
	final Terrain terrain;
	final MeshSnapshot[] meshes;
	final float[] propX;
	final float[] propY;
	final int[] propMesh;

	private MapArea(Arena arena, Terrain terrain, MeshSnapshot[] meshes, float[] propX, float[] propY, int[] propMesh)
	{
		this.arena = arena;
		this.terrain = terrain;
		this.meshes = meshes;
		this.propX = propX;
		this.propY = propY;
		this.propMesh = propMesh;
	}

	static MapArea decode(byte[] blob, BufferedImage ground) throws IOException
	{
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(blob));
		if (in.readInt() != VERSION)
		{
			throw new IOException("Location data is from another version; run the export again");
		}
		int pixelsPerTile = in.readInt();
		int tilesX = in.readInt();
		int tilesY = in.readInt();
		int startX = in.readInt();
		int startY = in.readInt();
		boolean[] blocked = new boolean[tilesX * tilesY];
		boolean[] props = new boolean[tilesX * tilesY];
		byte[] walls = new byte[tilesX * tilesY];
		for (int i = 0; i < blocked.length; i++)
		{
			int flags = in.readUnsignedByte();
			blocked[i] = (flags & 1) != 0;
			props[i] = (flags & 2) != 0;
			walls[i] = in.readByte();
		}
		Arena arena = Arena.of(tilesX, tilesY, blocked, walls, props, startX, startY);

		MeshSnapshot[] meshes = new MeshSnapshot[in.readInt()];
		for (int m = 0; m < meshes.length; m++)
		{
			int vertices = in.readInt();
			float[] x = new float[vertices];
			float[] y = new float[vertices];
			float[] z = new float[vertices];
			for (int v = 0; v < vertices; v++)
			{
				x[v] = in.readInt();
				y[v] = in.readInt();
				z[v] = in.readInt();
			}
			int faces = in.readInt();
			int[] a = new int[faces];
			int[] b = new int[faces];
			int[] c = new int[faces];
			int[] argb = new int[faces];
			for (int f = 0; f < faces; f++)
			{
				a[f] = in.readInt();
				b[f] = in.readInt();
				c[f] = in.readInt();
				argb[f] = in.readInt();
			}
			meshes[m] = new MeshSnapshot(x, y, z, a, b, c, argb);
		}

		int count = in.readInt();
		float[] px = new float[count];
		float[] py = new float[count];
		int[] mesh = new int[count];
		for (int p = 0; p < count; p++)
		{
			px[p] = in.readFloat();
			py[p] = in.readFloat();
			mesh[p] = in.readInt();
		}
		return new MapArea(arena, new Terrain(ground, Arena.TILE / pixelsPerTile), meshes, px, py, mesh);
	}
}
