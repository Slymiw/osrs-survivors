package com.osrssurvivors.render;

/**
 * A triangle mesh copied out of the game client, with one resolved colour per face. Coordinates
 * use the game's model space: x east, y down (negative is up), z north; unrotated models face
 * south (-z).
 */
public final class MeshSnapshot
{
	final float[] x;
	final float[] y;
	final float[] z;
	final int[] a;
	final int[] b;
	final int[] c;
	/** ARGB per face; alpha 0 means don't draw. */
	final int[] argb;

	public MeshSnapshot(float[] x, float[] y, float[] z, int[] a, int[] b, int[] c, int[] argb)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.a = a;
		this.b = b;
		this.c = c;
		this.argb = argb;
	}

	public int faceCount()
	{
		return argb.length;
	}

	public int vertexCount()
	{
		return x.length;
	}

	public float vertexX(int i)
	{
		return x[i];
	}

	public float vertexZ(int i)
	{
		return z[i];
	}

	public float vertexY(int i)
	{
		return y[i];
	}

	public int faceA(int f)
	{
		return a[f];
	}

	public int faceB(int f)
	{
		return b[f];
	}

	public int faceC(int f)
	{
		return c[f];
	}

	public int faceArgb(int f)
	{
		return argb[f];
	}

	/** The same mesh with new x and z vertex positions (heights and faces unchanged). */
	public MeshSnapshot withPositions(float[] newX, float[] newZ)
	{
		return new MeshSnapshot(newX, y, newZ, a, b, c, argb);
	}

	/** The same mesh made {@code factor} times bigger (or smaller) in every direction. */
	public MeshSnapshot scaled(double factor)
	{
		float[] nx = new float[x.length];
		float[] ny = new float[y.length];
		float[] nz = new float[z.length];
		for (int i = 0; i < x.length; i++)
		{
			nx[i] = (float) (x[i] * factor);
			ny[i] = (float) (y[i] * factor);
			nz[i] = (float) (z[i] * factor);
		}
		return new MeshSnapshot(nx, ny, nz, a, b, c, argb);
	}

	/** Two meshes drawn as one. */
	public static MeshSnapshot concat(MeshSnapshot p, MeshSnapshot q)
	{
		int nv = p.x.length;
		int nf = p.argb.length + q.argb.length;
		float[] x = new float[nv + q.x.length];
		float[] y = new float[x.length];
		float[] z = new float[x.length];
		System.arraycopy(p.x, 0, x, 0, nv);
		System.arraycopy(q.x, 0, x, nv, q.x.length);
		System.arraycopy(p.y, 0, y, 0, nv);
		System.arraycopy(q.y, 0, y, nv, q.y.length);
		System.arraycopy(p.z, 0, z, 0, nv);
		System.arraycopy(q.z, 0, z, nv, q.z.length);
		int[] a = new int[nf];
		int[] b = new int[nf];
		int[] c = new int[nf];
		int[] argb = new int[nf];
		int pf = p.argb.length;
		System.arraycopy(p.a, 0, a, 0, pf);
		System.arraycopy(p.b, 0, b, 0, pf);
		System.arraycopy(p.c, 0, c, 0, pf);
		System.arraycopy(p.argb, 0, argb, 0, pf);
		for (int i = 0; i < q.argb.length; i++)
		{
			a[pf + i] = q.a[i] + nv;
			b[pf + i] = q.b[i] + nv;
			c[pf + i] = q.c[i] + nv;
			argb[pf + i] = q.argb[i];
		}
		return new MeshSnapshot(x, y, z, a, b, c, argb);
	}
}
