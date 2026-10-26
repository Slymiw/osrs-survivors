package org.teavm.classlib.java.awt;

import org.teavm.classlib.java.awt.image.TImageObserver;

/** java.awt.Graphics for the browser; {@link TCanvasGraphics} implements it. */
public abstract class TGraphics
{
	public abstract TGraphics create();

	public abstract void dispose();

	public abstract TColor getColor();

	public abstract void setColor(TColor c);

	public abstract TFont getFont();

	public abstract void setFont(TFont font);

	public TFontMetrics getFontMetrics()
	{
		return getFontMetrics(getFont());
	}

	public TFontMetrics getFontMetrics(TFont f)
	{
		return TFontMetrics.of(f);
	}

	public abstract void translate(int x, int y);

	public abstract TShape getClip();

	public abstract void setClip(TShape clip);

	public abstract void setClip(int x, int y, int width, int height);

	public abstract void clipRect(int x, int y, int width, int height);

	public abstract TRectangle getClipBounds();

	public abstract void fillRect(int x, int y, int width, int height);

	public abstract void drawRect(int x, int y, int width, int height);

	public abstract void clearRect(int x, int y, int width, int height);

	public abstract void drawLine(int x1, int y1, int x2, int y2);

	public abstract void fillOval(int x, int y, int width, int height);

	public abstract void drawOval(int x, int y, int width, int height);

	public abstract void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle);

	public abstract void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle);

	public abstract void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight);

	public abstract void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight);

	public abstract void fillPolygon(int[] xPoints, int[] yPoints, int nPoints);

	public abstract void drawPolygon(int[] xPoints, int[] yPoints, int nPoints);

	public abstract void drawPolyline(int[] xPoints, int[] yPoints, int nPoints);

	public abstract void drawString(String str, int x, int y);

	public abstract boolean drawImage(TImage img, int x, int y, TImageObserver observer);

	public abstract boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer);

	public abstract boolean drawImage(TImage img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
		TImageObserver observer);
}
