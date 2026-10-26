package com.osrssurvivors.render;

/**
 * Converts the game's packed 16-bit HSL colours (6 bits hue, 3 saturation, 7 luminance) to RGB,
 * using the same HSL-to-RGB and gamma steps as the classic software renderer.
 */
public final class JagexPalette
{
	private JagexPalette()
	{
	}

	/**
	 * @param brightness gamma exponent; the game's setting ranges from 0.9 (dark) to 0.6 (bright)
	 * @return RGB for every packed HSL value
	 */
	public static int[] build(double brightness)
	{
		int[] palette = new int[65536];
		int index = 0;
		for (int hueSat = 0; hueSat < 512; hueSat++)
		{
			double hue = (hueSat >> 3) / 64.0 + 0.0078125;
			double sat = (hueSat & 7) / 8.0 + 0.0625;
			for (int lumIndex = 0; lumIndex < 128; lumIndex++)
			{
				double lum = lumIndex / 128.0;
				double q = lum < 0.5 ? lum * (1 + sat) : lum + sat - lum * sat;
				double p = 2 * lum - q;
				double r = hueToRgb(p, q, hue + 1 / 3.0);
				double g = hueToRgb(p, q, hue);
				double b = hueToRgb(p, q, hue - 1 / 3.0);
				palette[index++] = channel(r, brightness) << 16 | channel(g, brightness) << 8 | channel(b, brightness);
			}
		}
		return palette;
	}

	private static double hueToRgb(double p, double q, double t)
	{
		if (t < 0)
		{
			t += 1;
		}
		if (t > 1)
		{
			t -= 1;
		}
		if (6 * t < 1)
		{
			return p + (q - p) * 6 * t;
		}
		if (2 * t < 1)
		{
			return q;
		}
		if (3 * t < 2)
		{
			return p + (q - p) * (2 / 3.0 - t) * 6;
		}
		return p;
	}

	private static int channel(double value, double brightness)
	{
		int c = (int) (Math.pow(Math.max(0, Math.min(1, value)), brightness) * 256);
		return Math.min(255, c);
	}
}
