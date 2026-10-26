package org.teavm.classlib.java.awt;

import org.teavm.jso.JSBody;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

/**
 * A canvas and its 2D context, shared by every graphics drawing into it. It remembers which
 * clip, transform and styles are currently set on the context, so graphics only change what
 * differs. The context is saved once in its plain state; changing the clip restores that state
 * and clips afresh (canvas clips can only shrink otherwise).
 */
public final class TCanvasSurface
{
	private final HTMLCanvasElement canvas;
	private final CanvasRenderingContext2D ctx;

	/** What is set on the context now; null or NaN when unknown. */
	Object clip;
	double t00;
	double t10;
	double t01;
	double t11;
	double t02;
	double t12;
	boolean transformKnown;
	double alpha;
	String operation;
	String fill;
	String strokeStyle;
	double lineWidth;
	String font;
	int smoothing;

	private TCanvasSurface(HTMLCanvasElement canvas)
	{
		this.canvas = canvas;
		this.ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
		ctx.save();
		forget();
	}

	public static TCanvasSurface create(int width, int height)
	{
		HTMLCanvasElement canvas = (HTMLCanvasElement) HTMLDocument.current().createElement("canvas");
		canvas.setWidth(width);
		canvas.setHeight(height);
		return new TCanvasSurface(canvas);
	}

	public static TCanvasSurface adopt(HTMLCanvasElement canvas)
	{
		return new TCanvasSurface(canvas);
	}

	public HTMLCanvasElement canvas()
	{
		return canvas;
	}

	public CanvasRenderingContext2D context()
	{
		return ctx;
	}

	public TGraphics2D createGraphics()
	{
		return new TCanvasGraphics(this);
	}

	/** Resizing a canvas resets its context completely. */
	public void resize(int width, int height)
	{
		canvas.setWidth(width);
		canvas.setHeight(height);
		ctx.save();
		clip = null;
		forget();
	}

	/** Forgets what is set on the context (after a restore). */
	void forget()
	{
		transformKnown = false;
		alpha = Double.NaN;
		operation = null;
		fill = null;
		strokeStyle = null;
		lineWidth = Double.NaN;
		font = null;
		smoothing = -1;
	}

	@JSBody(params = {"ctx", "on"}, script = "ctx.imageSmoothingEnabled = on;")
	static native void setSmoothing(CanvasRenderingContext2D ctx, boolean on);

	@JSBody(params = {"ctx", "op"}, script = "ctx.globalCompositeOperation = op;")
	static native void setOperation(CanvasRenderingContext2D ctx, String op);

	@JSBody(params = {"ctx", "x", "y", "w", "h"}, script = "ctx.beginPath(); ctx.rect(x, y, w, h); ctx.clip();")
	static native void clipRect(CanvasRenderingContext2D ctx, double x, double y, double w, double h);
}
