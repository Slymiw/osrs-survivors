package org.teavm.classlib.java.awt;

/**
 * java.awt.Font for the browser: a CSS font. Logical names map to CSS generic families; any
 * other name is used as a CSS family (the page registers the RuneScape fonts under their names).
 */
public class TFont
{
	public static final String DIALOG = "Dialog";
	public static final String DIALOG_INPUT = "DialogInput";
	public static final String SANS_SERIF = "SansSerif";
	public static final String SERIF = "Serif";
	public static final String MONOSPACED = "Monospaced";
	public static final int PLAIN = 0;
	public static final int BOLD = 1;
	public static final int ITALIC = 2;

	private final String name;
	private final int style;
	private final float size;
	private String css;

	public TFont(String name, int style, int size)
	{
		this(name, style, (float) size);
	}

	private TFont(String name, int style, float size)
	{
		this.name = name == null ? DIALOG : name;
		this.style = style & 3;
		this.size = size;
	}

	public String getName()
	{
		return name;
	}

	public String getFamily()
	{
		return name;
	}

	public String getFontName()
	{
		return name;
	}

	public int getStyle()
	{
		return style;
	}

	public int getSize()
	{
		return (int) (size + 0.5);
	}

	public float getSize2D()
	{
		return size;
	}

	public boolean isBold()
	{
		return (style & BOLD) != 0;
	}

	public boolean isItalic()
	{
		return (style & ITALIC) != 0;
	}

	public boolean isPlain()
	{
		return style == PLAIN;
	}

	public TFont deriveFont(float newSize)
	{
		return new TFont(name, style, newSize);
	}

	public TFont deriveFont(int newStyle)
	{
		return new TFont(name, newStyle, size);
	}

	public TFont deriveFont(int newStyle, float newSize)
	{
		return new TFont(name, newStyle, newSize);
	}

	/** The font as a CSS font shorthand. */
	public String css()
	{
		if (css == null)
		{
			String family;
			switch (name)
			{
				case DIALOG:
				case SANS_SERIF:
					family = "sans-serif";
					break;
				case SERIF:
					family = "serif";
					break;
				case MONOSPACED:
				case DIALOG_INPUT:
					family = "monospace";
					break;
				default:
					family = "\"" + name + "\", sans-serif";
					break;
			}
			css = (isItalic() ? "italic " : "") + (isBold() ? "bold " : "") + size + "px " + family;
		}
		return css;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof TFont))
		{
			return false;
		}
		TFont f = (TFont) o;
		return f.name.equals(name) && f.style == style && f.size == size;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode() * 31 + style * 7 + (int) size;
	}
}
