package org.teavm.classlib.java.awt;

/**
 * java.awt.RenderingHints for the browser. Interpolation chooses image smoothing; the other
 * hints are accepted and ignored (the canvas always antialiases shapes and text).
 */
public class TRenderingHints
{
	public static class Key
	{
		private final int id;

		Key(int id)
		{
			this.id = id;
		}

		int id()
		{
			return id;
		}
	}

	static final int INTERPOLATION = 1;

	public static final Key KEY_ANTIALIASING = new Key(0);
	public static final Key KEY_TEXT_ANTIALIASING = new Key(0);
	public static final Key KEY_RENDERING = new Key(0);
	public static final Key KEY_STROKE_CONTROL = new Key(0);
	public static final Key KEY_FRACTIONALMETRICS = new Key(0);
	public static final Key KEY_ALPHA_INTERPOLATION = new Key(0);
	public static final Key KEY_COLOR_RENDERING = new Key(0);
	public static final Key KEY_INTERPOLATION = new Key(INTERPOLATION);

	public static final Object VALUE_ANTIALIAS_ON = "on";
	public static final Object VALUE_ANTIALIAS_OFF = "off";
	public static final Object VALUE_ANTIALIAS_DEFAULT = "default";
	public static final Object VALUE_TEXT_ANTIALIAS_ON = "on";
	public static final Object VALUE_TEXT_ANTIALIAS_OFF = "off";
	public static final Object VALUE_TEXT_ANTIALIAS_DEFAULT = "default";
	public static final Object VALUE_RENDER_SPEED = "speed";
	public static final Object VALUE_RENDER_QUALITY = "quality";
	public static final Object VALUE_RENDER_DEFAULT = "default";
	public static final Object VALUE_STROKE_PURE = "pure";
	public static final Object VALUE_STROKE_NORMALIZE = "normalize";
	public static final Object VALUE_STROKE_DEFAULT = "default";
	public static final Object VALUE_FRACTIONALMETRICS_ON = "on";
	public static final Object VALUE_FRACTIONALMETRICS_OFF = "off";
	public static final Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR = "nearest";
	public static final Object VALUE_INTERPOLATION_BILINEAR = "bilinear";
	public static final Object VALUE_INTERPOLATION_BICUBIC = "bicubic";
	public static final Object VALUE_ALPHA_INTERPOLATION_SPEED = "speed";
	public static final Object VALUE_ALPHA_INTERPOLATION_QUALITY = "quality";
	public static final Object VALUE_COLOR_RENDER_SPEED = "speed";
	public static final Object VALUE_COLOR_RENDER_QUALITY = "quality";
}
