package com.osrssurvivors.web;

import com.osrssurvivors.render.MeshSnapshot;
import java.util.List;

/**
 * A lit model as exported: vertices with animation labels, faces with their final colours.
 * {@link #pose} applies an animation frame the way the game client does (Model.transform), on a
 * copy of the vertices.
 */
final class AnimModel
{
	private static final int[] SINE = new int[2048];
	private static final int[] COSINE = new int[2048];

	static
	{
		for (int i = 0; i < 2048; i++)
		{
			SINE[i] = (int) (65536.0 * Math.sin(i * 0.0030679615));
			COSINE[i] = (int) (65536.0 * Math.cos(i * 0.0030679615));
		}
	}

	final int[] x;
	final int[] y;
	final int[] z;
	final int[] labels;
	final int[] a;
	final int[] b;
	final int[] c;
	final int[] argb;
	/** Vertex indices per label, built on first use. */
	private int[][] groups;

	AnimModel(int[] x, int[] y, int[] z, int[] labels, int[] a, int[] b, int[] c, int[] argb)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.labels = labels;
		this.a = a;
		this.b = b;
		this.c = c;
		this.argb = argb;
	}

	/** Several models as one, so an animation moves them together (as the client merges a player). */
	static AnimModel concat(List<AnimModel> parts)
	{
		int nv = 0;
		int nf = 0;
		for (AnimModel p : parts)
		{
			nv += p.x.length;
			nf += p.a.length;
		}
		int[] x = new int[nv];
		int[] y = new int[nv];
		int[] z = new int[nv];
		int[] labels = new int[nv];
		int[] a = new int[nf];
		int[] b = new int[nf];
		int[] c = new int[nf];
		int[] argb = new int[nf];
		int vo = 0;
		int fo = 0;
		for (AnimModel p : parts)
		{
			int pv = p.x.length;
			System.arraycopy(p.x, 0, x, vo, pv);
			System.arraycopy(p.y, 0, y, vo, pv);
			System.arraycopy(p.z, 0, z, vo, pv);
			System.arraycopy(p.labels, 0, labels, vo, pv);
			for (int f = 0; f < p.a.length; f++)
			{
				a[fo + f] = p.a[f] + vo;
				b[fo + f] = p.b[f] + vo;
				c[fo + f] = p.c[f] + vo;
				argb[fo + f] = p.argb[f];
			}
			vo += pv;
			fo += p.a.length;
		}
		return new AnimModel(x, y, z, labels, a, b, c, argb);
	}

	private int[][] groups()
	{
		if (groups == null)
		{
			int max = -1;
			for (int l : labels)
			{
				max = Math.max(max, l);
			}
			int[] counts = new int[max + 1];
			for (int l : labels)
			{
				if (l >= 0)
				{
					counts[l]++;
				}
			}
			int[][] g = new int[max + 1][];
			for (int i = 0; i <= max; i++)
			{
				g[i] = new int[counts[i]];
				counts[i] = 0;
			}
			for (int v = 0; v < labels.length; v++)
			{
				int l = labels[v];
				if (l >= 0)
				{
					g[l][counts[l]++] = v;
				}
			}
			groups = g;
		}
		return groups;
	}

	/** The mesh in an animation frame (or unanimated, for null), scaled by {@code scale}. */
	MeshSnapshot pose(Sequence.Frame frame, double scale)
	{
		int n = x.length;
		int[] vx = x.clone();
		int[] vy = y.clone();
		int[] vz = z.clone();
		if (frame != null)
		{
			animate(frame, vx, vy, vz);
		}
		float[] fx = new float[n];
		float[] fy = new float[n];
		float[] fz = new float[n];
		for (int i = 0; i < n; i++)
		{
			fx[i] = (float) (vx[i] * scale);
			fy[i] = (float) (vy[i] * scale);
			fz[i] = (float) (vz[i] * scale);
		}
		return new MeshSnapshot(fx, fy, fz, a, b, c, argb);
	}

	private void animate(Sequence.Frame frame, int[] vx, int[] vy, int[] vz)
	{
		int[][] groups = groups();
		Sequence.Framemap map = frame.framemap;
		// the pivot set by the latest origin transform
		int ox = 0;
		int oy = 0;
		int oz = 0;
		for (int t = 0; t < frame.slots.length; t++)
		{
			int slot = frame.slots[t];
			if (slot < 0 || slot >= map.types.length)
			{
				continue;
			}
			int type = map.types[slot];
			int[] labels = map.labels[slot];
			int dx = frame.dx[t];
			int dy = frame.dy[t];
			int dz = frame.dz[t];
			switch (type)
			{
				case 0:
				{
					int count = 0;
					long sx = 0;
					long sy = 0;
					long sz = 0;
					for (int label : labels)
					{
						if (label < groups.length)
						{
							for (int v : groups[label])
							{
								sx += vx[v];
								sy += vy[v];
								sz += vz[v];
								count++;
							}
						}
					}
					if (count > 0)
					{
						ox = (int) (sx / count) + dx;
						oy = (int) (sy / count) + dy;
						oz = (int) (sz / count) + dz;
					}
					else
					{
						ox = dx;
						oy = dy;
						oz = dz;
					}
					break;
				}
				case 1:
					for (int label : labels)
					{
						if (label < groups.length)
						{
							for (int v : groups[label])
							{
								vx[v] += dx;
								vy[v] += dy;
								vz[v] += dz;
							}
						}
					}
					break;
				case 2:
				{
					int rx = (dx & 255) * 8;
					int ry = (dy & 255) * 8;
					int rz = (dz & 255) * 8;
					for (int label : labels)
					{
						if (label >= groups.length)
						{
							continue;
						}
						for (int v : groups[label])
						{
							int px = vx[v] - ox;
							int py = vy[v] - oy;
							int pz = vz[v] - oz;
							if (rz != 0)
							{
								int sin = SINE[rz];
								int cos = COSINE[rz];
								int t2 = sin * py + cos * px >> 16;
								py = cos * py - sin * px >> 16;
								px = t2;
							}
							if (rx != 0)
							{
								int sin = SINE[rx];
								int cos = COSINE[rx];
								int t2 = cos * py - sin * pz >> 16;
								pz = sin * py + cos * pz >> 16;
								py = t2;
							}
							if (ry != 0)
							{
								int sin = SINE[ry];
								int cos = COSINE[ry];
								int t2 = sin * pz + cos * px >> 16;
								pz = cos * pz - sin * px >> 16;
								px = t2;
							}
							vx[v] = px + ox;
							vy[v] = py + oy;
							vz[v] = pz + oz;
						}
					}
					break;
				}
				case 3:
					for (int label : labels)
					{
						if (label >= groups.length)
						{
							continue;
						}
						for (int v : groups[label])
						{
							vx[v] = (vx[v] - ox) * dx / 128 + ox;
							vy[v] = (vy[v] - oy) * dy / 128 + oy;
							vz[v] = (vz[v] - oz) * dz / 128 + oz;
						}
					}
					break;
				default:
					// 5 changes transparency, which the sprites don't show
					break;
			}
		}
	}
}
