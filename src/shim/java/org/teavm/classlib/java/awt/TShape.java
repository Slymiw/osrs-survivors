package org.teavm.classlib.java.awt;

/** java.awt.Shape for the browser. Only rectangles are drawn as themselves; others by their bounds. */
public interface TShape
{
	TRectangle getBounds();
}
