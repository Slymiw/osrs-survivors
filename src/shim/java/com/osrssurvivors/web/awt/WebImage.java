package com.osrssurvivors.web.awt;

import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLImageElement;

/**
 * What browser code can do with a {@code java.awt.image.BufferedImage} beyond the AWT API. The
 * shim's BufferedImage implements it, so a BufferedImage can be cast to it:
 * {@code ((WebImage) (Object) image).adopt(canvas)}.
 */
public interface WebImage
{
	/** Makes this image draw into (and show) an existing canvas, at the canvas's size. */
	void adopt(HTMLCanvasElement canvas);

	/** Makes this image show a loaded picture; its size becomes the picture's. */
	void load(HTMLImageElement image);

	/** Resizes an adopted canvas; the contents are cleared. */
	void resize(int width, int height);
}
