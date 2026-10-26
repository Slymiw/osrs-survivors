package org.teavm.classlib.java.awt.image;

import com.osrssurvivors.web.awt.WebImage;
import org.teavm.classlib.java.awt.TCanvasSurface;
import org.teavm.classlib.java.awt.TGraphics;
import org.teavm.classlib.java.awt.TGraphics2D;
import org.teavm.classlib.java.awt.TImage;
import org.teavm.jso.canvas.CanvasImageSource;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

/**
 * java.awt.image.BufferedImage for the browser. Backed by a loaded picture until something draws
 * into it, then by a canvas. Pixel access goes through the canvas's image data, which is slow, so
 * drawing should use graphics.
 */
public class TBufferedImage extends TImage implements WebImage
{
	public static final int TYPE_CUSTOM = 0;
	public static final int TYPE_INT_RGB = 1;
	public static final int TYPE_INT_ARGB = 2;
	public static final int TYPE_INT_ARGB_PRE = 3;
	public static final int TYPE_INT_BGR = 4;
	public static final int TYPE_3BYTE_BGR = 5;
	public static final int TYPE_4BYTE_ABGR = 6;
	public static final int TYPE_4BYTE_ABGR_PRE = 7;
	public static final int TYPE_BYTE_GRAY = 10;
	public static final int TYPE_BYTE_INDEXED = 13;

	private int width;
	private int height;
	private final int type;
	private TCanvasSurface surface;
	private HTMLImageElement picture;

	public TBufferedImage(int width, int height, int type)
	{
		this.width = Math.max(1, width);
		this.height = Math.max(1, height);
		this.type = type;
	}

	public int getWidth()
	{
		return width;
	}

	public int getHeight()
	{
		return height;
	}

	@Override
	public int getWidth(TImageObserver observer)
	{
		return width;
	}

	@Override
	public int getHeight(TImageObserver observer)
	{
		return height;
	}

	public int getType()
	{
		return type;
	}

	private TCanvasSurface surface()
	{
		if (surface == null)
		{
			surface = TCanvasSurface.create(width, height);
			if (picture != null)
			{
				surface.context().drawImage(picture, 0, 0);
				picture = null;
			}
		}
		return surface;
	}

	public TGraphics2D createGraphics()
	{
		return surface().createGraphics();
	}

	@Override
	public TGraphics getGraphics()
	{
		return createGraphics();
	}

	@Override
	public CanvasImageSource canvasSource()
	{
		if (surface != null)
		{
			return surface.canvas();
		}
		return picture;
	}

	public int getRGB(int x, int y)
	{
		Uint8ClampedArray d = surface().context().getImageData(x, y, 1, 1).getData();
		return d.get(3) << 24 | d.get(0) << 16 | d.get(1) << 8 | d.get(2);
	}

	public void setRGB(int x, int y, int argb)
	{
		ImageData data = surface().context().createImageData(1, 1);
		Uint8ClampedArray d = data.getData();
		d.set(0, argb >> 16 & 0xff);
		d.set(1, argb >> 8 & 0xff);
		d.set(2, argb & 0xff);
		d.set(3, type == TYPE_INT_RGB ? 255 : argb >>> 24);
		surface.context().putImageData(data, x, y);
	}

	public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize)
	{
		if (rgbArray == null)
		{
			rgbArray = new int[offset + h * scansize];
		}
		Uint8ClampedArray d = surface().context().getImageData(startX, startY, w, h).getData();
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int i = (y * w + x) * 4;
				rgbArray[offset + y * scansize + x] = d.get(i + 3) << 24 | d.get(i) << 16 | d.get(i + 1) << 8 | d.get(i + 2);
			}
		}
		return rgbArray;
	}

	public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize)
	{
		ImageData data = surface().context().createImageData(w, h);
		Uint8ClampedArray d = data.getData();
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int argb = rgbArray[offset + y * scansize + x];
				int i = (y * w + x) * 4;
				d.set(i, argb >> 16 & 0xff);
				d.set(i + 1, argb >> 8 & 0xff);
				d.set(i + 2, argb & 0xff);
				d.set(i + 3, type == TYPE_INT_RGB ? 255 : argb >>> 24);
			}
		}
		surface.context().putImageData(data, startX, startY);
	}

	@Override
	public void adopt(HTMLCanvasElement canvas)
	{
		surface = TCanvasSurface.adopt(canvas);
		picture = null;
		width = canvas.getWidth();
		height = canvas.getHeight();
	}

	@Override
	public void load(HTMLImageElement image)
	{
		picture = image;
		surface = null;
		width = Math.max(1, image.getNaturalWidth());
		height = Math.max(1, image.getNaturalHeight());
	}

	@Override
	public void resize(int width, int height)
	{
		this.width = Math.max(1, width);
		this.height = Math.max(1, height);
		surface().resize(this.width, this.height);
	}
}
