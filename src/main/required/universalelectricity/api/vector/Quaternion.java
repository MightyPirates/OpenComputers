package universalelectricity.api.vector;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Quaternion class designed to be used for the rotation of objects.
 * 
 * @author ChickenBones
 */
public class Quaternion implements Cloneable
{
	public double x;
	public double y;
	public double z;
	public double s;
	public static final double SQRT2 = Math.sqrt(2D);

	public Quaternion()
	{
		s = 1;
		x = 0;
		y = 0;
		z = 0;
	}

	public Quaternion(Quaternion Quaternion)
	{
		x = Quaternion.x;
		y = Quaternion.y;
		z = Quaternion.z;
		s = Quaternion.s;
	}

	public Quaternion(double d, double d1, double d2, double d3)
	{
		x = d1;
		y = d2;
		z = d3;
		s = d;
	}

	public Quaternion set(Quaternion Quaternion)
	{
		x = Quaternion.x;
		y = Quaternion.y;
		z = Quaternion.z;
		s = Quaternion.s;

		return this;
	}

	public Quaternion set(double d, double d1, double d2, double d3)
	{
		x = d1;
		y = d2;
		z = d3;
		s = d;

		return this;
	}

	public static Quaternion aroundAxis(double ax, double ay, double az, double angle)
	{
		return new Quaternion().setAroundAxis(ax, ay, az, angle);
	}

	public static Quaternion aroundAxis(Vector3 axis, double angle)
	{
		return aroundAxis(axis.x, axis.y, axis.z, angle);
	}

	public Quaternion setAroundAxis(double ax, double ay, double az, double angle)
	{
		angle *= 0.5;
		double d4 = Math.sin(angle);
		return set(Math.cos(angle), ax * d4, ay * d4, az * d4);
	}

	public Quaternion setAroundAxis(Vector3 axis, double angle)
	{
		return setAroundAxis(axis.x, axis.y, axis.z, angle);
	}

	public Quaternion multiply(Quaternion Quaternion)
	{
		double d = s * Quaternion.s - x * Quaternion.x - y * Quaternion.y - z * Quaternion.z;
		double d1 = s * Quaternion.x + x * Quaternion.s - y * Quaternion.z + z * Quaternion.y;
		double d2 = s * Quaternion.y + x * Quaternion.z + y * Quaternion.s - z * Quaternion.x;
		double d3 = s * Quaternion.z - x * Quaternion.y + y * Quaternion.x + z * Quaternion.s;
		s = d;
		x = d1;
		y = d2;
		z = d3;

		return this;
	}

	public Quaternion rightMultiply(Quaternion Quaternion)
	{
		double d = s * Quaternion.s - x * Quaternion.x - y * Quaternion.y - z * Quaternion.z;
		double d1 = s * Quaternion.x + x * Quaternion.s + y * Quaternion.z - z * Quaternion.y;
		double d2 = s * Quaternion.y - x * Quaternion.z + y * Quaternion.s + z * Quaternion.x;
		double d3 = s * Quaternion.z + x * Quaternion.y - y * Quaternion.x + z * Quaternion.s;
		s = d;
		x = d1;
		y = d2;
		z = d3;

		return this;
	}

	public double mag()
	{
		return Math.sqrt(x * x + y * y + z * z + s * s);
	}

	public Quaternion normalize()
	{
		double d = mag();
		if (d != 0)
		{
			d = 1 / d;
			x *= d;
			y *= d;
			z *= d;
			s *= d;
		}

		return this;
	}

	public Quaternion copy()
	{
		return new Quaternion(this);
	}

	public void rotate(Vector3 vec)
	{
		double d = -x * vec.x - y * vec.y - z * vec.z;
		double d1 = s * vec.x + y * vec.z - z * vec.y;
		double d2 = s * vec.y - x * vec.z + z * vec.x;
		double d3 = s * vec.z + x * vec.y - y * vec.x;
		vec.x = d1 * s - d * x - d2 * z + d3 * y;
		vec.y = d2 * s - d * y + d1 * z - d3 * x;
		vec.z = d3 * s - d * z - d1 * y + d2 * x;
	}

	@Override
	public String toString()
	{
		MathContext cont = new MathContext(4, RoundingMode.HALF_UP);
		return "Quaternion[" + new BigDecimal(s, cont) + ", " + new BigDecimal(x, cont) + ", " + new BigDecimal(y, cont) + ", " + new BigDecimal(z, cont) + "]";
	}

	public Rotation rotation()
	{
		return new Rotation(this);
	}
}
