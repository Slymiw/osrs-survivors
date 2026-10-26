package org.teavm.classlib.java.awt;

import org.teavm.classlib.java.awt.image.TImageObserver;
import org.teavm.jso.canvas.CanvasImageSource;

/** java.awt.Image for the browser. */
public abstract class TImage
{
	public abstract int getWidth(TImageObserver observer);

	public abstract int getHeight(TImageObserver observer);

	public abstract TGraphics getGraphics();

	public void flush()
	{
	}

	/** What the canvas draws for this image, or null when there is nothing to draw yet. */
	public abstract CanvasImageSource canvasSource();
}
