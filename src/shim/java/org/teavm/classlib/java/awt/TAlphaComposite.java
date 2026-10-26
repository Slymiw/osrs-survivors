package org.teavm.classlib.java.awt;

/** java.awt.AlphaComposite for the browser: source-over with an alpha (the canvas's global alpha). */
public final class TAlphaComposite implements TComposite
{
	public static final int CLEAR = 1;
	public static final int SRC = 2;
	public static final int SRC_OVER = 3;
	public static final int DST_OVER = 4;
	public static final TAlphaComposite SrcOver = new TAlphaComposite(SRC_OVER, 1f);
	public static final TAlphaComposite Src = new TAlphaComposite(SRC, 1f);
	public static final TAlphaComposite Clear = new TAlphaComposite(CLEAR, 1f);

	private final int rule;
	private final float alpha;

	private TAlphaComposite(int rule, float alpha)
	{
		this.rule = rule;
		this.alpha = alpha;
	}

	public static TAlphaComposite getInstance(int rule)
	{
		return getInstance(rule, 1f);
	}

	public static TAlphaComposite getInstance(int rule, float alpha)
	{
		if (rule == SRC_OVER && alpha == 1f)
		{
			return SrcOver;
		}
		return new TAlphaComposite(rule, Math.max(0f, Math.min(1f, alpha)));
	}

	public int getRule()
	{
		return rule;
	}

	public float getAlpha()
	{
		return alpha;
	}

	/** The canvas compositing operation for the rule. */
	String operation()
	{
		switch (rule)
		{
			case SRC:
				return "copy";
			case DST_OVER:
				return "destination-over";
			case CLEAR:
				return "destination-out";
			default:
				return "source-over";
		}
	}
}
