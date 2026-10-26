package org.teavm.classlib.java.awt;

import java.util.HashMap;
import java.util.Map;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.TextMetrics;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

/** java.awt.FontMetrics for the browser, measured with a canvas. Widths are cached per string. */
public class TFontMetrics
{
	private static CanvasRenderingContext2D measure;
	private static final Map<String, TFontMetrics> BY_FONT = new HashMap<>();

	protected final TFont font;
	private final int ascent;
	private final int descent;
	private final Map<String, Integer> widths = new HashMap<>();

	private TFontMetrics(TFont font)
	{
		this.font = font;
		CanvasRenderingContext2D ctx = context();
		ctx.setFont(font.css());
		TextMetrics m = ctx.measureText("Hgjy");
		double a = m.getFontBoundingBoxAscent();
		double d = m.getFontBoundingBoxDescent();
		// fall back to proportions of the size where font box metrics aren't available
		ascent = a > 0 ? (int) Math.round(a) : (int) Math.round(font.getSize2D() * 0.8);
		descent = d > 0 ? (int) Math.round(d) : (int) Math.round(font.getSize2D() * 0.2);
	}

	static TFontMetrics of(TFont font)
	{
		String key = font.css();
		TFontMetrics m = BY_FONT.get(key);
		if (m == null)
		{
			m = new TFontMetrics(font);
			BY_FONT.put(key, m);
		}
		return m;
	}

	/** Forgets measurements, e.g. once web fonts have loaded. */
	public static void flush()
	{
		BY_FONT.clear();
	}

	private static CanvasRenderingContext2D context()
	{
		if (measure == null)
		{
			HTMLCanvasElement canvas = (HTMLCanvasElement) HTMLDocument.current().createElement("canvas");
			measure = (CanvasRenderingContext2D) canvas.getContext("2d");
		}
		return measure;
	}

	public TFont getFont()
	{
		return font;
	}

	public int getAscent()
	{
		return ascent;
	}

	public int getDescent()
	{
		return descent;
	}

	public int getLeading()
	{
		return 0;
	}

	public int getHeight()
	{
		return ascent + descent;
	}

	public int getMaxAscent()
	{
		return ascent;
	}

	public int getMaxDescent()
	{
		return descent;
	}

	public int stringWidth(String s)
	{
		if (s == null || s.isEmpty())
		{
			return 0;
		}
		Integer w = widths.get(s);
		if (w == null)
		{
			CanvasRenderingContext2D ctx = context();
			ctx.setFont(font.css());
			w = (int) Math.round(ctx.measureText(s).getWidth());
			if (widths.size() > 4000)
			{
				widths.clear();
			}
			widths.put(s, w);
		}
		return w;
	}

	public int charWidth(char c)
	{
		return stringWidth(String.valueOf(c));
	}
}
