package universalelectricity.api.vector;

/**
 * Vector2 Class is used for defining objects in a 2D space.
 * 
 * @author Calclavia
 */

public class Vector2 implements Cloneable
{
	public double x;
	public double y;

	public Vector2()
	{
		this(0, 0);
	}

	public Vector2(double x, double y)
	{
		this.x = x;
		this.y = y;
	}

	/**
	 * Returns the integer floor value.
	 * 
	 * @return
	 */
	public int intX()
	{
		return (int) Math.floor(this.x);
	}

	public int intY()
	{
		return (int) Math.floor(this.y);
	}

	/**
	 * Makes a new copy of this Vector. Prevents variable referencing problems.
	 */
	@Override
	public Vector2 clone()
	{
		return new Vector2(this.x, this.y);
	}

	/** ---------------------- MAGNITUDE FUNCTIONS ---------------------------- */
	public double getMagnitude()
	{
		return Math.sqrt(this.getMagnitudeSquared());
	}

	public double getMagnitudeSquared()
	{
		return this.x * this.x + this.y * this.y;
	}

	public Vector2 normalize()
	{
		double d = this.getMagnitude();

		if (d != 0)
		{
			this.scale(1 / d);
		}

		return this;
	}

	public static double distance(Vector2 point1, Vector2 point2)
	{
		return point1.clone().distance(point2);
	}

	public static double slope(Vector2 point1, Vector2 point2)
	{
		double xDifference = point1.x - point2.x;
		double yDiference = point1.y - point2.y;
		return yDiference / xDifference;
	}

	public double distance(Vector2 target)
	{
		Vector2 difference = this.clone().subtract(target);
		return difference.getMagnitude();
	}

	public Vector2 add(Vector2 par1)
	{
		this.x += par1.x;
		this.y += par1.y;
		return this;
	}

	public Vector2 add(double par1)
	{
		this.x += par1;
		this.y += par1;
		return this;
	}

	public Vector2 subtract(Vector2 par1)
	{
		this.x -= par1.x;
		this.y -= par1.y;
		return this;
	}

	public Vector2 invert()
	{
		this.multiply(-1);
		return this;
	}

	public Vector2 scale(double amount)
	{
		this.x *= amount;
		this.y *= amount;
		return this;
	}

	@Deprecated
	public Vector2 multiply(double amount)
	{
		return this.scale(amount);
	}

	public Vector2 round()
	{
		return new Vector2(Math.round(this.x), Math.round(this.y));
	}

	public Vector2 ceil()
	{
		return new Vector2(Math.ceil(this.x), Math.ceil(this.y));
	}

	public Vector2 floor()
	{
		return new Vector2(Math.floor(this.x), Math.floor(this.y));
	}

	@Override
	public int hashCode()
	{
		return ("X:" + this.x + "Y:" + this.y).hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Vector2)
		{
			Vector2 vector = (Vector2) o;
			return this.x == vector.x && this.y == vector.y;
		}

		return false;
	}

	@Override
	public String toString()
	{
		return "Vector2 [" + this.x + "," + this.y + "]";
	}
}