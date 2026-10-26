package org.teavm.classlib.java.awt.geom;

/** java.awt.geom.AffineTransform for the browser: the 2x3 matrix, same conventions as AWT. */
public class TAffineTransform
{
	double m00 = 1;
	double m10;
	double m01;
	double m11 = 1;
	double m02;
	double m12;

	public TAffineTransform()
	{
	}

	public TAffineTransform(TAffineTransform t)
	{
		setTransform(t);
	}

	public TAffineTransform(double m00, double m10, double m01, double m11, double m02, double m12)
	{
		setTransform(m00, m10, m01, m11, m02, m12);
	}

	public static TAffineTransform getTranslateInstance(double tx, double ty)
	{
		return new TAffineTransform(1, 0, 0, 1, tx, ty);
	}

	public static TAffineTransform getScaleInstance(double sx, double sy)
	{
		return new TAffineTransform(sx, 0, 0, sy, 0, 0);
	}

	public static TAffineTransform getRotateInstance(double theta)
	{
		TAffineTransform t = new TAffineTransform();
		t.rotate(theta);
		return t;
	}

	public double getScaleX()
	{
		return m00;
	}

	public double getScaleY()
	{
		return m11;
	}

	public double getShearX()
	{
		return m01;
	}

	public double getShearY()
	{
		return m10;
	}

	public double getTranslateX()
	{
		return m02;
	}

	public double getTranslateY()
	{
		return m12;
	}

	public boolean isIdentity()
	{
		return m00 == 1 && m10 == 0 && m01 == 0 && m11 == 1 && m02 == 0 && m12 == 0;
	}

	public void setToIdentity()
	{
		setTransform(1, 0, 0, 1, 0, 0);
	}

	public void setTransform(TAffineTransform t)
	{
		setTransform(t.m00, t.m10, t.m01, t.m11, t.m02, t.m12);
	}

	public void setTransform(double m00, double m10, double m01, double m11, double m02, double m12)
	{
		this.m00 = m00;
		this.m10 = m10;
		this.m01 = m01;
		this.m11 = m11;
		this.m02 = m02;
		this.m12 = m12;
	}

	public void translate(double tx, double ty)
	{
		m02 += tx * m00 + ty * m01;
		m12 += tx * m10 + ty * m11;
	}

	public void scale(double sx, double sy)
	{
		m00 *= sx;
		m10 *= sx;
		m01 *= sy;
		m11 *= sy;
	}

	public void rotate(double theta)
	{
		double sin = Math.sin(theta);
		double cos = Math.cos(theta);
		double n00 = m00 * cos + m01 * sin;
		double n01 = -m00 * sin + m01 * cos;
		double n10 = m10 * cos + m11 * sin;
		double n11 = -m10 * sin + m11 * cos;
		m00 = n00;
		m01 = n01;
		m10 = n10;
		m11 = n11;
	}

	public void rotate(double theta, double anchorX, double anchorY)
	{
		translate(anchorX, anchorY);
		rotate(theta);
		translate(-anchorX, -anchorY);
	}

	public void shear(double shx, double shy)
	{
		double n00 = m00 + m01 * shy;
		double n01 = m00 * shx + m01;
		double n10 = m10 + m11 * shy;
		double n11 = m10 * shx + m11;
		m00 = n00;
		m01 = n01;
		m10 = n10;
		m11 = n11;
	}

	/** this = this x t: t is applied first. */
	public void concatenate(TAffineTransform t)
	{
		double n00 = m00 * t.m00 + m01 * t.m10;
		double n01 = m00 * t.m01 + m01 * t.m11;
		double n02 = m00 * t.m02 + m01 * t.m12 + m02;
		double n10 = m10 * t.m00 + m11 * t.m10;
		double n11 = m10 * t.m01 + m11 * t.m11;
		double n12 = m10 * t.m02 + m11 * t.m12 + m12;
		setTransform(n00, n10, n01, n11, n02, n12);
	}

	/** this = t x this: t is applied last. */
	public void preConcatenate(TAffineTransform t)
	{
		TAffineTransform r = new TAffineTransform(t);
		r.concatenate(this);
		setTransform(r);
	}

	public TAffineTransform createInverse()
	{
		double det = m00 * m11 - m01 * m10;
		if (det == 0)
		{
			return new TAffineTransform();
		}
		return new TAffineTransform(m11 / det, -m10 / det, -m01 / det, m00 / det,
			(m01 * m12 - m11 * m02) / det, (m10 * m02 - m00 * m12) / det);
	}

	/** Device x of a user-space point. */
	public double mapX(double x, double y)
	{
		return m00 * x + m01 * y + m02;
	}

	/** Device y of a user-space point. */
	public double mapY(double x, double y)
	{
		return m10 * x + m11 * y + m12;
	}

	@Override
	public Object clone()
	{
		return new TAffineTransform(this);
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof TAffineTransform))
		{
			return false;
		}
		TAffineTransform t = (TAffineTransform) o;
		return t.m00 == m00 && t.m10 == m10 && t.m01 == m01 && t.m11 == m11 && t.m02 == m02 && t.m12 == m12;
	}

	@Override
	public int hashCode()
	{
		return (int) (m00 * 31 + m11 * 17 + m02 * 7 + m12);
	}
}
