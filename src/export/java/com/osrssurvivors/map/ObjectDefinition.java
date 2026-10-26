package com.osrssurvivors.map;

/**
 * The parts of a scenery object's cache definition needed to place, draw and collide with it.
 * Decoding stops quietly at an opcode it doesn't know, keeping what was read so far.
 */
public final class ObjectDefinition
{
	/** Interaction type: walk through. */
	public static final int INTERACT_NONE = 0;

	public final int id;
	public String name = "null";
	/** Model ids, and the placement type each model is for (null when one set serves type 10). */
	public int[] models = new int[0];
	public int[] modelTypes;
	public int sizeX = 1;
	public int sizeY = 1;
	public int interactType = 2;
	public short[] recolorFrom;
	public short[] recolorTo;
	public int scaleX = 128;
	public int scaleHeight = 128;
	public int scaleY = 128;
	public int offsetX;
	public int offsetHeight;
	public int offsetY;
	public boolean mirrored;
	public int ambient;
	public int contrast;
	public final String[] actions = new String[5];
	/** Ids this object can turn into (varbit-controlled); the last entry is the default, -1 for none. */
	public int[] morphs;

	public ObjectDefinition(int id)
	{
		this.id = id;
	}

	public static ObjectDefinition decode(int id, byte[] data)
	{
		ObjectDefinition d = new ObjectDefinition(id);
		if (data == null)
		{
			return d;
		}
		CacheBuffer b = new CacheBuffer(data);
		try
		{
			while (b.remaining() > 0)
			{
				int op = b.u8();
				if (op == 0 || !d.read(op, b))
				{
					break;
				}
			}
		}
		catch (RuntimeException e)
		{
			// truncated or unexpected data: keep what was read
		}
		return d;
	}

	/** Returns false for an unknown opcode. */
	private boolean read(int op, CacheBuffer b)
	{
		switch (op)
		{
			case 1:
			case 6:
			{
				int n = b.u8();
				models = new int[n];
				modelTypes = new int[n];
				for (int i = 0; i < n; i++)
				{
					models[i] = op == 1 ? b.u16() : b.i32();
					modelTypes[i] = b.u8();
				}
				return true;
			}
			case 5:
			case 7:
			{
				int n = b.u8();
				models = new int[n];
				modelTypes = null;
				for (int i = 0; i < n; i++)
				{
					models[i] = op == 5 ? b.u16() : b.i32();
				}
				return true;
			}
			case 2:
				name = b.string();
				return true;
			case 14:
				sizeX = b.u8();
				return true;
			case 15:
				sizeY = b.u8();
				return true;
			case 17:
				interactType = INTERACT_NONE;
				return true;
			case 27:
				interactType = 1;
				return true;
			case 18:
			case 21:
			case 22:
			case 23:
			case 64:
			case 73:
			case 74:
			case 89:
			case 90:
			case 94:
				return true;
			case 19:
			case 28:
			case 69:
			case 75:
			case 81:
			case 91:
			case 95:
			case 96:
				b.u8();
				return true;
			case 24:
			case 61:
			case 68:
			case 82:
				b.u16();
				return true;
			case 29:
				ambient = b.s8();
				return true;
			case 39:
				contrast = b.s8() * 25;
				return true;
			case 30:
			case 31:
			case 32:
			case 33:
			case 34:
			{
				String s = b.string();
				actions[op - 30] = "hidden".equalsIgnoreCase(s) ? null : s;
				return true;
			}
			case 40:
			{
				int n = b.u8();
				recolorFrom = new short[n];
				recolorTo = new short[n];
				for (int i = 0; i < n; i++)
				{
					recolorFrom[i] = (short) b.u16();
					recolorTo[i] = (short) b.u16();
				}
				return true;
			}
			case 41:
				b.skip(b.u8() * 4);
				return true;
			case 62:
				mirrored = true;
				return true;
			case 65:
				scaleX = b.u16();
				return true;
			case 66:
				scaleHeight = b.u16();
				return true;
			case 67:
				scaleY = b.u16();
				return true;
			case 70:
				offsetX = b.s16();
				return true;
			case 71:
				offsetHeight = b.s16();
				return true;
			case 72:
				offsetY = b.s16();
				return true;
			case 77:
			case 92:
			{
				b.u16();
				b.u16();
				int fallback = -1;
				if (op == 92)
				{
					fallback = b.u16();
					fallback = fallback == 65535 ? -1 : fallback;
				}
				int n = b.u8();
				morphs = new int[n + 2];
				for (int i = 0; i <= n; i++)
				{
					int v = b.u16();
					morphs[i] = v == 65535 ? -1 : v;
				}
				morphs[n + 1] = fallback;
				return true;
			}
			case 78:
				b.u16();
				b.u8();
				b.u8();
				return true;
			case 79:
				b.skip(6);
				b.skip(b.u8() * 2);
				return true;
			case 93:
				b.skip(6);
				return true;
			case 101:
				b.skip(13);
				b.string();
				return true;
			case 249:
				b.skipParams();
				return true;
			default:
				return false;
		}
	}

	/** True for doors and gates, which the arena leaves open. */
	public boolean isDoor()
	{
		for (String a : actions)
		{
			if (a != null && (a.equalsIgnoreCase("open") || a.equalsIgnoreCase("close")))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isClosedDoor()
	{
		for (String a : actions)
		{
			if (a != null && a.equalsIgnoreCase("open"))
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasModels()
	{
		return models.length > 0;
	}

	/** The first id this object can turn into that exists, preferring the default; -1 if none. */
	public int morphTarget()
	{
		if (morphs == null)
		{
			return -1;
		}
		if (morphs[morphs.length - 1] >= 0)
		{
			return morphs[morphs.length - 1];
		}
		for (int m : morphs)
		{
			if (m >= 0)
			{
				return m;
			}
		}
		return -1;
	}
}
