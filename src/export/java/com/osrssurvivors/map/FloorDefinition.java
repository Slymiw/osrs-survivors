package com.osrssurvivors.map;

/** A ground colour (underlay) or ground cover (overlay) definition from the cache. */
public final class FloorDefinition
{
	/** The colour overlays use for "draw nothing here". */
	public static final int INVISIBLE = 0xff00ff;

	public int rgb;
	public int texture = -1;

	public static FloorDefinition decode(byte[] data, boolean overlay)
	{
		FloorDefinition d = new FloorDefinition();
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
				if (op == 0)
				{
					break;
				}
				switch (op)
				{
					case 1:
						d.rgb = b.u24();
						break;
					case 2:
						d.texture = overlay ? b.u8() : b.u16();
						break;
					case 3:
						d.texture = b.u16();
						break;
					case 4:
					case 9:
					case 15:
						b.u16();
						break;
					case 7:
					case 13:
						b.u24();
						break;
					case 11:
					case 14:
					case 16:
						b.u8();
						break;
					case 5:
					case 8:
					case 10:
					case 12:
						break;
					default:
						return d;
				}
			}
		}
		catch (RuntimeException e)
		{
			// keep what was read
		}
		if (d.texture == 255 && overlay)
		{
			d.texture = -1;
		}
		return d;
	}
}
