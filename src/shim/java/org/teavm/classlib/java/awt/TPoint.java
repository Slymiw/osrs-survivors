package org.teavm.classlib.java.awt;

/** java.awt.Point for the browser. */
public class TPoint
{
	public int x;
	public int y;

	public TPoint()
	{
	}

	public TPoint(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	public TPoint(TPoint p)
	{
		this(p.x, p.y);
	}

	public double getX()
	{
		return x;
	}

	public double getY()
	{
		return y;
	}

	public void setLocation(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	public void translate(int dx, int dy)
	{
		x += dx;
		y += dy;
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof TPoint && ((TPoint) o).x == x && ((TPoint) o).y == y;
	}

	@Override
	public int hashCode()
	{
		return x * 31 + y;
	}

	@Override
	public String toString()
	{
		return "java.awt.Point[x=" + x + ",y=" + y + "]";
	}
}
