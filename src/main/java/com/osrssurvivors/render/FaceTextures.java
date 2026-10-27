package com.osrssurvivors.render;

/**
 * The textured faces of a mesh: for each, which face, its texture, u and v at its three corners,
 * the light at each corner, and how far a flowing texture has scrolled.
 */
public final class FaceTextures
{
	final int[] faces;
	final Texture[] textures;
	/** u0, v0, u1, v1, u2, v2 per face. */
	final float[] uv;
	/** Light at each corner, per face (1 is the texture's own colour). */
	final float[] light;
	/** Scroll of each face's texture, as a share of its size. */
	final float[] scroll;

	public FaceTextures(int[] faces, Texture[] textures, float[] uv, float[] light, float[] scroll)
	{
		this.faces = faces;
		this.textures = textures;
		this.uv = uv;
		this.light = light;
		this.scroll = scroll;
	}

	public int count()
	{
		return faces.length;
	}

	/** The same faces numbered {@code offset} further on (for a mesh placed after another). */
	FaceTextures shifted(int offset)
	{
		int[] moved = new int[faces.length];
		for (int i = 0; i < faces.length; i++)
		{
			moved[i] = faces[i] + offset;
		}
		return new FaceTextures(moved, textures, uv, light, scroll);
	}

	/** Two lists as one; either may be null. */
	static FaceTextures join(FaceTextures p, FaceTextures q)
	{
		if (p == null || q == null)
		{
			return p == null ? q : p;
		}
		int n = p.faces.length;
		int m = q.faces.length;
		int[] faces = new int[n + m];
		Texture[] textures = new Texture[n + m];
		float[] uv = new float[(n + m) * 6];
		float[] light = new float[(n + m) * 3];
		float[] scroll = new float[n + m];
		System.arraycopy(p.faces, 0, faces, 0, n);
		System.arraycopy(q.faces, 0, faces, n, m);
		System.arraycopy(p.textures, 0, textures, 0, n);
		System.arraycopy(q.textures, 0, textures, n, m);
		System.arraycopy(p.uv, 0, uv, 0, n * 6);
		System.arraycopy(q.uv, 0, uv, n * 6, m * 6);
		System.arraycopy(p.light, 0, light, 0, n * 3);
		System.arraycopy(q.light, 0, light, n * 3, m * 3);
		System.arraycopy(p.scroll, 0, scroll, 0, n);
		System.arraycopy(q.scroll, 0, scroll, n, m);
		return new FaceTextures(faces, textures, uv, light, scroll);
	}
}
