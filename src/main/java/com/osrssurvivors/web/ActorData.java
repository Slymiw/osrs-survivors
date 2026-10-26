package com.osrssurvivors.web;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The exported animations and lit models (WebExport.actorBlob): sequences by id and models by
 * name ("npc.RAT", "gear.RUNE_SCIMITAR", "kit.TORSO").
 */
final class ActorData
{
	/** Must match WebExport.MODELS_VERSION. */
	private static final int VERSION = 1;

	final Map<Integer, Sequence> sequences = new HashMap<>();
	final Map<String, AnimModel> models = new HashMap<>();

	static ActorData decode(byte[] blob) throws IOException
	{
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(blob));
		if (in.readInt() != VERSION)
		{
			throw new IOException("Asset data is from another version; run the export again");
		}
		ActorData data = new ActorData();

		// frames name their framemap by id; the framemaps come after the sequences
		int sequenceCount = in.readInt();
		int[] ids = new int[sequenceCount];
		int[][] lengths = new int[sequenceCount][];
		int[][] framemapIds = new int[sequenceCount][];
		int[][][] frameData = new int[sequenceCount][][];
		for (int s = 0; s < sequenceCount; s++)
		{
			ids[s] = in.readInt();
			int frames = in.readInt();
			lengths[s] = new int[frames];
			framemapIds[s] = new int[frames];
			frameData[s] = new int[frames][];
			for (int f = 0; f < frames; f++)
			{
				lengths[s][f] = in.readInt();
				framemapIds[s][f] = in.readInt();
				int transforms = in.readInt();
				int[] d = new int[transforms * 4];
				for (int t = 0; t < transforms; t++)
				{
					d[t * 4] = in.readShort();
					d[t * 4 + 1] = in.readInt();
					d[t * 4 + 2] = in.readInt();
					d[t * 4 + 3] = in.readInt();
				}
				frameData[s][f] = d;
			}
		}

		Map<Integer, Sequence.Framemap> framemaps = new HashMap<>();
		int framemapCount = in.readInt();
		for (int m = 0; m < framemapCount; m++)
		{
			int id = in.readInt();
			int length = in.readInt();
			int[] types = new int[length];
			int[][] labels = new int[length][];
			for (int i = 0; i < length; i++)
			{
				types[i] = in.readUnsignedByte();
				int n = in.readUnsignedShort();
				labels[i] = new int[n];
				for (int k = 0; k < n; k++)
				{
					labels[i][k] = in.readShort();
				}
			}
			framemaps.put(id, new Sequence.Framemap(types, labels));
		}

		for (int s = 0; s < sequenceCount; s++)
		{
			int frames = lengths[s].length;
			Sequence.Frame[] list = new Sequence.Frame[frames];
			for (int f = 0; f < frames; f++)
			{
				int[] d = frameData[s][f];
				int n = d.length / 4;
				int[] slots = new int[n];
				int[] dx = new int[n];
				int[] dy = new int[n];
				int[] dz = new int[n];
				for (int t = 0; t < n; t++)
				{
					slots[t] = d[t * 4];
					dx[t] = d[t * 4 + 1];
					dy[t] = d[t * 4 + 2];
					dz[t] = d[t * 4 + 3];
				}
				list[f] = new Sequence.Frame(framemaps.get(framemapIds[s][f]), slots, dx, dy, dz);
			}
			data.sequences.put(ids[s], new Sequence(ids[s], list, lengths[s]));
		}

		int modelCount = in.readInt();
		for (int m = 0; m < modelCount; m++)
		{
			String name = in.readUTF();
			int vertices = in.readInt();
			int[] x = new int[vertices];
			int[] y = new int[vertices];
			int[] z = new int[vertices];
			for (int v = 0; v < vertices; v++)
			{
				x[v] = in.readInt();
				y[v] = in.readInt();
				z[v] = in.readInt();
			}
			int[] labels = new int[vertices];
			for (int v = 0; v < vertices; v++)
			{
				labels[v] = in.readShort();
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
			data.models.put(name, new AnimModel(x, y, z, labels, a, b, c, argb));
		}
		return data;
	}
}
