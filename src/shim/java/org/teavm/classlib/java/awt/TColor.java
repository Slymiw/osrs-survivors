package org.teavm.classlib.java.awt;

/** java.awt.Color for the browser: an ARGB value with its CSS form cached. */
public class TColor
{
	public static final TColor white = new TColor(255, 255, 255);
	public static final TColor WHITE = white;
	public static final TColor lightGray = new TColor(192, 192, 192);
	public static final TColor LIGHT_GRAY = lightGray;
	public static final TColor gray = new TColor(128, 128, 128);
	public static final TColor GRAY = gray;
	public static final TColor darkGray = new TColor(64, 64, 64);
	public static final TColor DARK_GRAY = darkGray;
	public static final TColor black = new TColor(0, 0, 0);
	public static final TColor BLACK = black;
	public static final TColor red = new TColor(255, 0, 0);
	public static final TColor RED = red;
	public static final TColor pink = new TColor(255, 175, 175);
	public static final TColor PINK = pink;
	public static final TColor orange = new TColor(255, 200, 0);
	public static final TColor ORANGE = orange;
	public static final TColor yellow = new TColor(255, 255, 0);
	public static final TColor YELLOW = yellow;
	public static final TColor green = new TColor(0, 255, 0);
	public static final TColor GREEN = green;
	public static final TColor magenta = new TColor(255, 0, 255);
	public static final TColor MAGENTA = magenta;
	public static final TColor cyan = new TColor(0, 255, 255);
	public static final TColor CYAN = cyan;
	public static final TColor blue = new TColor(0, 0, 255);
	public static final TColor BLUE = blue;

	private final int value;
	private String css;

	public TColor(int r, int g, int b)
	{
		this(r, g, b, 255);
	}

	public TColor(int r, int g, int b, int a)
	{
		value = (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
	}

	public TColor(int rgb)
	{
		value = 0xff000000 | rgb;
	}

	public TColor(int rgba, boolean hasAlpha)
	{
		value = hasAlpha ? rgba : 0xff000000 | rgba;
	}

	public TColor(float r, float g, float b)
	{
		this(r, g, b, 1f);
	}

	public TColor(float r, float g, float b, float a)
	{
		this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5), (int) (a * 255 + 0.5));
	}

	private static int clamp(int v)
	{
		return v < 0 ? 0 : Math.min(255, v);
	}

	public int getRed()
	{
		return value >> 16 & 0xff;
	}

	public int getGreen()
	{
		return value >> 8 & 0xff;
	}

	public int getBlue()
	{
		return value & 0xff;
	}

	public int getAlpha()
	{
		return value >>> 24;
	}

	public int getRGB()
	{
		return value;
	}

	public TColor brighter()
	{
		int r = getRed();
		int g = getGreen();
		int b = getBlue();
		int i = (int) (1.0 / 0.3);
		if (r == 0 && g == 0 && b == 0)
		{
			return new TColor(i, i, i, getAlpha());
		}
		r = r > 0 && r < i ? i : r;
		g = g > 0 && g < i ? i : g;
		b = b > 0 && b < i ? i : b;
		return new TColor(Math.min((int) (r / 0.7), 255), Math.min((int) (g / 0.7), 255), Math.min((int) (b / 0.7), 255), getAlpha());
	}

	public TColor darker()
	{
		return new TColor((int) (getRed() * 0.7), (int) (getGreen() * 0.7), (int) (getBlue() * 0.7), getAlpha());
	}

	public static TColor getHSBColor(float h, float s, float b)
	{
		return new TColor(HSBtoRGB(h, s, b));
	}

	public static int HSBtoRGB(float hue, float saturation, float brightness)
	{
		int r = 0;
		int g = 0;
		int b = 0;
		if (saturation == 0)
		{
			r = g = b = (int) (brightness * 255.0f + 0.5f);
		}
		else
		{
			float h = (hue - (float) Math.floor(hue)) * 6.0f;
			float f = h - (float) Math.floor(h);
			float p = brightness * (1.0f - saturation);
			float q = brightness * (1.0f - saturation * f);
			float t = brightness * (1.0f - (saturation * (1.0f - f)));
			switch ((int) h)
			{
				case 0:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (t * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 1:
					r = (int) (q * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 2:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (t * 255.0f + 0.5f);
					break;
				case 3:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (q * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 4:
					r = (int) (t * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				default:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (q * 255.0f + 0.5f);
					break;
			}
		}
		return 0xff000000 | (r << 16) | (g << 8) | b;
	}

	/** The colour as a CSS colour string. */
	public String css()
	{
		if (css == null)
		{
			int a = getAlpha();
			css = a == 255 ? "rgb(" + getRed() + "," + getGreen() + "," + getBlue() + ")"
				: "rgba(" + getRed() + "," + getGreen() + "," + getBlue() + "," + (a / 255.0) + ")";
		}
		return css;
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof TColor && ((TColor) o).value == value;
	}

	@Override
	public int hashCode()
	{
		return value;
	}

	@Override
	public String toString()
	{
		return "java.awt.Color[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + "]";
	}
}
