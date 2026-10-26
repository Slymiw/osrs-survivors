package org.teavm.classlib.java.awt;

import java.util.HashMap;
import java.util.Map;
import org.teavm.classlib.java.awt.geom.TAffineTransform;
import org.teavm.classlib.java.awt.image.TImageObserver;
import org.teavm.jso.JSBody;
import org.teavm.jso.canvas.CanvasImageSource;
import org.teavm.jso.canvas.CanvasRenderingContext2D;

/**
 * Graphics2D drawing into a canvas. Follows AWT's pixel conventions closely enough for the game:
 * fills cover whole pixels, one-pixel outlines sit on pixel centres, angles run anticlockwise
 * from three o'clock. Clips are kept as device-space rectangles (the game only clips to
 * rectangles under translation and scaling). Shapes and text are antialiased by the canvas.
 */
final class TCanvasGraphics extends TGraphics2D
{
	/** A clip in device pixels; immutable, so graphics can share it and the surface can compare by identity. */
	static final class TClipRect implements TShape
	{
		final double x;
		final double y;
		final double w;
		final double h;

		TClipRect(double x, double y, double w, double h)
		{
			this.x = x;
			this.y = y;
			this.w = Math.max(0, w);
			this.h = Math.max(0, h);
		}

		@Override
		public TRectangle getBounds()
		{
			return new TRectangle((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(w), (int) Math.ceil(h));
		}

		TClipRect intersect(TClipRect o)
		{
			double x1 = Math.max(x, o.x);
			double y1 = Math.max(y, o.y);
			double x2 = Math.min(x + w, o.x + o.w);
			double y2 = Math.min(y + h, o.y + o.h);
			return new TClipRect(x1, y1, x2 - x1, y2 - y1);
		}
	}

	private static final TBasicStroke DEFAULT_STROKE = new TBasicStroke(1f);
	private static final TFont DEFAULT_FONT = new TFont(TFont.DIALOG, TFont.PLAIN, 12);
	private static final Map<Integer, String> CSS = new HashMap<>();

	private final TCanvasSurface s;
	private TAffineTransform transform;
	private TClipRect clip;
	private TColor color = TColor.WHITE;
	private TColor background = TColor.BLACK;
	private TFont font = DEFAULT_FONT;
	private TStroke stroke = DEFAULT_STROKE;
	private TComposite composite = TAlphaComposite.SrcOver;
	private boolean smooth;

	TCanvasGraphics(TCanvasSurface surface)
	{
		this.s = surface;
		this.transform = new TAffineTransform();
	}

	private TCanvasGraphics(TCanvasGraphics g)
	{
		this.s = g.s;
		this.transform = new TAffineTransform(g.transform);
		this.clip = g.clip;
		this.color = g.color;
		this.background = g.background;
		this.font = g.font;
		this.stroke = g.stroke;
		this.composite = g.composite;
		this.smooth = g.smooth;
	}

	// ---------------------------------------------------------------- state

	@Override
	public TGraphics create()
	{
		return new TCanvasGraphics(this);
	}

	@Override
	public void dispose()
	{
	}

	@Override
	public TColor getColor()
	{
		return color;
	}

	@Override
	public void setColor(TColor c)
	{
		if (c != null)
		{
			color = c;
		}
	}

	@Override
	public TColor getBackground()
	{
		return background;
	}

	@Override
	public void setBackground(TColor color)
	{
		background = color;
	}

	@Override
	public TFont getFont()
	{
		return font;
	}

	@Override
	public void setFont(TFont font)
	{
		if (font != null)
		{
			this.font = font;
		}
	}

	@Override
	public TComposite getComposite()
	{
		return composite;
	}

	@Override
	public void setComposite(TComposite comp)
	{
		if (comp != null)
		{
			composite = comp;
		}
	}

	@Override
	public TStroke getStroke()
	{
		return stroke;
	}

	@Override
	public void setStroke(TStroke s)
	{
		if (s != null)
		{
			stroke = s;
		}
	}

	@Override
	public void setRenderingHint(TRenderingHints.Key hintKey, Object hintValue)
	{
		if (hintKey.id() == TRenderingHints.INTERPOLATION)
		{
			smooth = hintValue != TRenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
		}
	}

	@Override
	public Object getRenderingHint(TRenderingHints.Key hintKey)
	{
		if (hintKey.id() == TRenderingHints.INTERPOLATION)
		{
			return smooth ? TRenderingHints.VALUE_INTERPOLATION_BILINEAR : TRenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
		}
		return null;
	}

	@Override
	public void translate(int x, int y)
	{
		transform.translate(x, y);
	}

	@Override
	public void translate(double tx, double ty)
	{
		transform.translate(tx, ty);
	}

	@Override
	public void scale(double sx, double sy)
	{
		transform.scale(sx, sy);
	}

	@Override
	public void rotate(double theta)
	{
		transform.rotate(theta);
	}

	@Override
	public void rotate(double theta, double x, double y)
	{
		transform.rotate(theta, x, y);
	}

	@Override
	public TAffineTransform getTransform()
	{
		return new TAffineTransform(transform);
	}

	@Override
	public void setTransform(TAffineTransform tx)
	{
		transform = new TAffineTransform(tx);
	}

	@Override
	public void transform(TAffineTransform tx)
	{
		transform.concatenate(tx);
	}

	// ---------------------------------------------------------------- clipping

	/** A user-space rectangle in device pixels (its bounding box under the current transform). */
	private TClipRect device(double x, double y, double w, double h)
	{
		double ax = transform.mapX(x, y);
		double ay = transform.mapY(x, y);
		double bx = transform.mapX(x + w, y + h);
		double by = transform.mapY(x + w, y + h);
		double cx = transform.mapX(x + w, y);
		double cy = transform.mapY(x + w, y);
		double dx = transform.mapX(x, y + h);
		double dy = transform.mapY(x, y + h);
		double minX = Math.min(Math.min(ax, bx), Math.min(cx, dx));
		double minY = Math.min(Math.min(ay, by), Math.min(cy, dy));
		double maxX = Math.max(Math.max(ax, bx), Math.max(cx, dx));
		double maxY = Math.max(Math.max(ay, by), Math.max(cy, dy));
		return new TClipRect(minX, minY, maxX - minX, maxY - minY);
	}

	private TClipRect device(TShape shape)
	{
		if (shape instanceof TClipRect)
		{
			return (TClipRect) shape;
		}
		TRectangle r = shape.getBounds();
		return device(r.x, r.y, r.width, r.height);
	}

	@Override
	public TShape getClip()
	{
		return clip;
	}

	@Override
	public void setClip(TShape shape)
	{
		clip = shape == null ? null : device(shape);
	}

	@Override
	public void setClip(int x, int y, int width, int height)
	{
		clip = device(x, y, width, height);
	}

	@Override
	public void clip(TShape shape)
	{
		if (shape == null)
		{
			return;
		}
		TClipRect r = device(shape);
		clip = clip == null ? r : clip.intersect(r);
	}

	@Override
	public void clipRect(int x, int y, int width, int height)
	{
		TClipRect r = device(x, y, width, height);
		clip = clip == null ? r : clip.intersect(r);
	}

	@Override
	public TRectangle getClipBounds()
	{
		if (clip == null)
		{
			return null;
		}
		TAffineTransform inverse = transform.createInverse();
		double x1 = inverse.mapX(clip.x, clip.y);
		double y1 = inverse.mapY(clip.x, clip.y);
		double x2 = inverse.mapX(clip.x + clip.w, clip.y + clip.h);
		double y2 = inverse.mapY(clip.x + clip.w, clip.y + clip.h);
		int x = (int) Math.floor(Math.min(x1, x2));
		int y = (int) Math.floor(Math.min(y1, y2));
		return new TRectangle(x, y, (int) Math.ceil(Math.max(x1, x2)) - x, (int) Math.ceil(Math.max(y1, y2)) - y);
	}

	// ---------------------------------------------------------------- applying state to the context

	private static String css(TColor c)
	{
		int argb = c.getRGB();
		String css = CSS.get(argb);
		if (css == null)
		{
			css = c.css();
			if (CSS.size() > 20000)
			{
				CSS.clear();
			}
			CSS.put(argb, css);
		}
		return css;
	}

	private CanvasRenderingContext2D prepare()
	{
		TCanvasSurface s = this.s;
		CanvasRenderingContext2D ctx = s.context();
		if (s.clip != clip)
		{
			ctx.restore();
			ctx.save();
			s.forget();
			if (clip != null)
			{
				TCanvasSurface.clipRect(ctx, clip.x, clip.y, clip.w, clip.h);
			}
			s.clip = clip;
		}
		TAffineTransform t = transform;
		if (!s.transformKnown || s.t00 != t.getScaleX() || s.t10 != t.getShearY() || s.t01 != t.getShearX()
			|| s.t11 != t.getScaleY() || s.t02 != t.getTranslateX() || s.t12 != t.getTranslateY())
		{
			s.t00 = t.getScaleX();
			s.t10 = t.getShearY();
			s.t01 = t.getShearX();
			s.t11 = t.getScaleY();
			s.t02 = t.getTranslateX();
			s.t12 = t.getTranslateY();
			s.transformKnown = true;
			ctx.setTransform(s.t00, s.t10, s.t01, s.t11, s.t02, s.t12);
		}
		double alpha = 1;
		String operation = "source-over";
		if (composite instanceof TAlphaComposite)
		{
			TAlphaComposite ac = (TAlphaComposite) composite;
			alpha = ac.getAlpha();
			operation = ac.operation();
		}
		if (s.alpha != alpha)
		{
			s.alpha = alpha;
			ctx.setGlobalAlpha(alpha);
		}
		if (!operation.equals(s.operation))
		{
			s.operation = operation;
			TCanvasSurface.setOperation(ctx, operation);
		}
		int smoothing = smooth ? 1 : 0;
		if (s.smoothing != smoothing)
		{
			s.smoothing = smoothing;
			TCanvasSurface.setSmoothing(ctx, smooth);
		}
		return ctx;
	}

	private CanvasRenderingContext2D prepareFill()
	{
		CanvasRenderingContext2D ctx = prepare();
		String css = css(color);
		if (!css.equals(s.fill))
		{
			s.fill = css;
			ctx.setFillStyle(css);
		}
		return ctx;
	}

	private double lineWidth()
	{
		return stroke instanceof TBasicStroke ? ((TBasicStroke) stroke).getLineWidth() : 1;
	}

	/** Offset that puts odd-width lines on pixel centres, as AWT's stroke normalisation does. */
	private double strokeOffset()
	{
		double w = lineWidth();
		return Math.round(w) % 2 == 1 ? 0.5 : 0;
	}

	private CanvasRenderingContext2D prepareStroke()
	{
		CanvasRenderingContext2D ctx = prepare();
		String css = css(color);
		if (!css.equals(s.strokeStyle))
		{
			s.strokeStyle = css;
			ctx.setStrokeStyle(css);
		}
		double w = Math.max(lineWidth(), 0.01);
		if (s.lineWidth != w)
		{
			s.lineWidth = w;
			ctx.setLineWidth(w);
			ctx.setLineCap("square");
			ctx.setLineJoin("miter");
		}
		return ctx;
	}

	// ---------------------------------------------------------------- drawing

	@Override
	public void fillRect(int x, int y, int width, int height)
	{
		if (width > 0 && height > 0)
		{
			prepareFill().fillRect(x, y, width, height);
		}
	}

	@Override
	public void clearRect(int x, int y, int width, int height)
	{
		TColor saved = color;
		TComposite savedComposite = composite;
		color = background;
		composite = TAlphaComposite.Src;
		fillRect(x, y, width, height);
		color = saved;
		composite = savedComposite;
	}

	@Override
	public void drawRect(int x, int y, int width, int height)
	{
		if (width < 0 || height < 0)
		{
			return;
		}
		CanvasRenderingContext2D ctx = prepareStroke();
		double o = strokeOffset();
		ctx.strokeRect(x + o, y + o, width, height);
	}

	@Override
	public void fill(TShape shape)
	{
		if (shape instanceof TRectangle)
		{
			TRectangle r = (TRectangle) shape;
			fillRect(r.x, r.y, r.width, r.height);
		}
		else if (shape != null)
		{
			TRectangle r = shape.getBounds();
			fillRect(r.x, r.y, r.width, r.height);
		}
	}

	@Override
	public void draw(TShape shape)
	{
		if (shape != null)
		{
			TRectangle r = shape.getBounds();
			drawRect(r.x, r.y, r.width, r.height);
		}
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2)
	{
		CanvasRenderingContext2D ctx = prepareStroke();
		double o = strokeOffset();
		ctx.beginPath();
		ctx.moveTo(x1 + o, y1 + o);
		ctx.lineTo(x2 + o, y2 + o);
		ctx.stroke();
	}

	private static void ellipsePath(CanvasRenderingContext2D ctx, double cx, double cy, double rx, double ry)
	{
		ctx.beginPath();
		ctx.ellipse(cx, cy, Math.max(0, rx), Math.max(0, ry), 0, 0, 2 * Math.PI);
	}

	@Override
	public void fillOval(int x, int y, int width, int height)
	{
		if (width <= 0 || height <= 0)
		{
			return;
		}
		CanvasRenderingContext2D ctx = prepareFill();
		ellipsePath(ctx, x + width / 2.0, y + height / 2.0, width / 2.0, height / 2.0);
		ctx.fill();
	}

	@Override
	public void drawOval(int x, int y, int width, int height)
	{
		if (width < 0 || height < 0)
		{
			return;
		}
		CanvasRenderingContext2D ctx = prepareStroke();
		double o = strokeOffset();
		ellipsePath(ctx, x + width / 2.0 + o, y + height / 2.0 + o, width / 2.0, height / 2.0);
		ctx.stroke();
	}

	private static void arcPath(CanvasRenderingContext2D ctx, double cx, double cy, double rx, double ry, int startAngle, int arcAngle)
	{
		double start = -Math.toRadians(startAngle);
		double end = -Math.toRadians(startAngle + arcAngle);
		ellipse(ctx, cx, cy, Math.max(0, rx), Math.max(0, ry), start, end, arcAngle > 0);
	}

	@JSBody(params = {"ctx", "cx", "cy", "rx", "ry", "start", "end", "anticlockwise"},
		script = "ctx.ellipse(cx, cy, rx, ry, 0, start, end, anticlockwise);")
	private static native void ellipse(CanvasRenderingContext2D ctx, double cx, double cy, double rx, double ry, double start,
		double end, boolean anticlockwise);

	@Override
	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle)
	{
		if (width <= 0 || height <= 0 || arcAngle == 0)
		{
			return;
		}
		CanvasRenderingContext2D ctx = prepareFill();
		double cx = x + width / 2.0;
		double cy = y + height / 2.0;
		ctx.beginPath();
		ctx.moveTo(cx, cy);
		arcPath(ctx, cx, cy, width / 2.0, height / 2.0, startAngle, arcAngle);
		ctx.closePath();
		ctx.fill();
	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
	{
		if (width < 0 || height < 0 || arcAngle == 0)
		{
			return;
		}
		CanvasRenderingContext2D ctx = prepareStroke();
		double o = strokeOffset();
		ctx.beginPath();
		arcPath(ctx, x + width / 2.0 + o, y + height / 2.0 + o, width / 2.0, height / 2.0, startAngle, arcAngle);
		ctx.stroke();
	}

	private static void roundRectPath(CanvasRenderingContext2D ctx, double x, double y, double w, double h, double rx, double ry)
	{
		rx = Math.min(rx, w / 2);
		ry = Math.min(ry, h / 2);
		ctx.beginPath();
		ctx.moveTo(x + rx, y);
		ctx.lineTo(x + w - rx, y);
		ctx.ellipse(x + w - rx, y + ry, rx, ry, 0, -Math.PI / 2, 0);
		ctx.lineTo(x + w, y + h - ry);
		ctx.ellipse(x + w - rx, y + h - ry, rx, ry, 0, 0, Math.PI / 2);
		ctx.lineTo(x + rx, y + h);
		ctx.ellipse(x + rx, y + h - ry, rx, ry, 0, Math.PI / 2, Math.PI);
		ctx.lineTo(x, y + ry);
		ctx.ellipse(x + rx, y + ry, rx, ry, 0, Math.PI, Math.PI * 1.5);
		ctx.closePath();
	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
	{
		if (width <= 0 || height <= 0)
		{
			return;
		}
		CanvasRenderingContext2D ctx = prepareFill();
		roundRectPath(ctx, x, y, width, height, arcWidth / 2.0, arcHeight / 2.0);
		ctx.fill();
	}

	@Override
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
	{
		if (width < 0 || height < 0)
		{
			return;
		}
		CanvasRenderingContext2D ctx = prepareStroke();
		double o = strokeOffset();
		roundRectPath(ctx, x + o, y + o, width, height, arcWidth / 2.0, arcHeight / 2.0);
		ctx.stroke();
	}

	private static void polyPath(CanvasRenderingContext2D ctx, int[] xs, int[] ys, int n, double o)
	{
		ctx.beginPath();
		ctx.moveTo(xs[0] + o, ys[0] + o);
		for (int i = 1; i < n; i++)
		{
			ctx.lineTo(xs[i] + o, ys[i] + o);
		}
	}

	@Override
	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints)
	{
		if (nPoints < 3)
		{
			return;
		}
		CanvasRenderingContext2D ctx = prepareFill();
		polyPath(ctx, xPoints, yPoints, nPoints, 0);
		ctx.closePath();
		ctx.fill();
	}

	@Override
	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints)
	{
		if (nPoints < 2)
		{
			return;
		}
		CanvasRenderingContext2D ctx = prepareStroke();
		polyPath(ctx, xPoints, yPoints, nPoints, strokeOffset());
		ctx.closePath();
		ctx.stroke();
	}

	@Override
	public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints)
	{
		if (nPoints < 2)
		{
			return;
		}
		CanvasRenderingContext2D ctx = prepareStroke();
		polyPath(ctx, xPoints, yPoints, nPoints, strokeOffset());
		ctx.stroke();
	}

	@Override
	public void drawString(String str, int x, int y)
	{
		drawString(str, (float) x, (float) y);
	}

	@Override
	public void drawString(String str, float x, float y)
	{
		if (str == null || str.isEmpty())
		{
			return;
		}
		CanvasRenderingContext2D ctx = prepareFill();
		String css = font.css();
		if (!css.equals(s.font))
		{
			s.font = css;
			ctx.setFont(css);
		}
		ctx.fillText(str, x, y);
	}

	@Override
	public boolean drawImage(TImage img, int x, int y, TImageObserver observer)
	{
		CanvasImageSource src = img == null ? null : img.canvasSource();
		if (src != null)
		{
			prepare().drawImage(src, x, y);
		}
		return true;
	}

	@Override
	public boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer)
	{
		CanvasImageSource src = img == null ? null : img.canvasSource();
		if (src != null && width != 0 && height != 0)
		{
			prepare().drawImage(src, x, y, width, height);
		}
		return true;
	}

	@Override
	public boolean drawImage(TImage img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
		TImageObserver observer)
	{
		CanvasImageSource src = img == null ? null : img.canvasSource();
		if (src == null)
		{
			return true;
		}
		int iw = img.getWidth(null);
		int ih = img.getHeight(null);
		// keep the source inside the image, shrinking the destination to match
		double scaleX = (dx2 - dx1) / (double) (sx2 - sx1);
		double scaleY = (dy2 - dy1) / (double) (sy2 - sy1);
		if (sx2 == sx1 || sy2 == sy1 || Double.isNaN(scaleX) || Double.isNaN(scaleY))
		{
			return true;
		}
		double a = Math.max(0, Math.min(sx1, sx2));
		double b = Math.max(0, Math.min(sy1, sy2));
		double c = Math.min(iw, Math.max(sx1, sx2));
		double d = Math.min(ih, Math.max(sy1, sy2));
		if (c <= a || d <= b)
		{
			return true;
		}
		double ox = dx1 + (a - Math.min(sx1, sx2)) * Math.abs(scaleX);
		double oy = dy1 + (b - Math.min(sy1, sy2)) * Math.abs(scaleY);
		prepare().drawImage(src, a, b, c - a, d - b, ox, oy, (c - a) * Math.abs(scaleX), (d - b) * Math.abs(scaleY));
		return true;
	}
}
