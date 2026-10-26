package org.teavm.classlib.java.awt;

/** java.awt.Rectangle for the browser. */
public class TRectangle implements TShape
{
	public int x;
	public int y;
	public int width;
	public int height;

	public TRectangle()
	{
	}

	public TRectangle(int width, int height)
	{
		this(0, 0, width, height);
	}

	public TRectangle(int x, int y, int width, int height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public TRectangle(TRectangle r)
	{
		this(r.x, r.y, r.width, r.height);
	}

	public double getX()
	{
		return x;
	}

	public double getY()
	{
		return y;
	}

	public double getWidth()
	{
		return width;
	}

	public double getHeight()
	{
		return height;
	}

	public double getCenterX()
	{
		return x + width / 2.0;
	}

	public double getCenterY()
	{
		return y + height / 2.0;
	}

	public double getMinX()
	{
		return x;
	}

	public double getMinY()
	{
		return y;
	}

	public double getMaxX()
	{
		return x + (double) width;
	}

	public double getMaxY()
	{
		return y + (double) height;
	}

	public boolean isEmpty()
	{
		return width <= 0 || height <= 0;
	}

	public boolean contains(int px, int py)
	{
		return width > 0 && height > 0 && px >= x && py >= y && px < x + width && py < y + height;
	}

	public boolean contains(double px, double py)
	{
		return width > 0 && height > 0 && px >= x && py >= y && px < x + width && py < y + height;
	}

	public boolean contains(TPoint p)
	{
		return contains(p.x, p.y);
	}

	public boolean contains(TRectangle r)
	{
		return contains(r.x, r.y) && r.x + r.width <= x + width && r.y + r.height <= y + height;
	}

	public boolean intersects(TRectangle r)
	{
		return !isEmpty() && !r.isEmpty() && r.x < x + width && r.x + r.width > x && r.y < y + height && r.y + r.height > y;
	}

	public TRectangle intersection(TRectangle r)
	{
		int x1 = Math.max(x, r.x);
		int y1 = Math.max(y, r.y);
		int x2 = Math.min(x + width, r.x + r.width);
		int y2 = Math.min(y + height, r.y + r.height);
		return new TRectangle(x1, y1, x2 - x1, y2 - y1);
	}

	public TRectangle union(TRectangle r)
	{
		int x1 = Math.min(x, r.x);
		int y1 = Math.min(y, r.y);
		int x2 = Math.max(x + width, r.x + r.width);
		int y2 = Math.max(y + height, r.y + r.height);
		return new TRectangle(x1, y1, x2 - x1, y2 - y1);
	}

	public void setBounds(int x, int y, int width, int height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void setLocation(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	public void setSize(int width, int height)
	{
		this.width = width;
		this.height = height;
	}

	public void translate(int dx, int dy)
	{
		x += dx;
		y += dy;
	}

	public void grow(int h, int v)
	{
		x -= h;
		y -= v;
		width += 2 * h;
		height += 2 * v;
	}

	@Override
	public TRectangle getBounds()
	{
		return new TRectangle(this);
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof TRectangle))
		{
			return false;
		}
		TRectangle r = (TRectangle) o;
		return r.x == x && r.y == y && r.width == width && r.height == height;
	}

	@Override
	public int hashCode()
	{
		return ((x * 31 + y) * 31 + width) * 31 + height;
	}

	@Override
	public String toString()
	{
		return "java.awt.Rectangle[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
	}
}
