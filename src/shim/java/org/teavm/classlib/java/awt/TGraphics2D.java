package org.teavm.classlib.java.awt;

import org.teavm.classlib.java.awt.geom.TAffineTransform;

/** java.awt.Graphics2D for the browser; {@link TCanvasGraphics} implements it. */
public abstract class TGraphics2D extends TGraphics
{
	public abstract void fill(TShape s);

	public abstract void draw(TShape s);

	public abstract void clip(TShape s);

	public abstract TComposite getComposite();

	public abstract void setComposite(TComposite comp);

	public abstract TStroke getStroke();

	public abstract void setStroke(TStroke s);

	public abstract void setRenderingHint(TRenderingHints.Key hintKey, Object hintValue);

	public abstract Object getRenderingHint(TRenderingHints.Key hintKey);

	public abstract void translate(double tx, double ty);

	public abstract void scale(double sx, double sy);

	public abstract void rotate(double theta);

	public abstract void rotate(double theta, double x, double y);

	public abstract TAffineTransform getTransform();

	public abstract void setTransform(TAffineTransform tx);

	public abstract void transform(TAffineTransform tx);

	public abstract void drawString(String str, float x, float y);

	public abstract TColor getBackground();

	public abstract void setBackground(TColor color);
}
