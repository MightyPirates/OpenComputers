package universalelectricity.api.vector;

/**
 * @author ChickenBones
 * 
 */
public class Rotation
{
	public double angle;
	public Vector3 axis;
	private Quaternion quat;

	public Rotation(double angle, Vector3 axis)
	{
		this.angle = angle;
		this.axis = axis;
	}

	public Rotation(double angle, double x, double y, double z)
	{
		this(angle, new Vector3(x, y, z));
	}

	public Rotation(Quaternion quat)
	{
		this.quat = quat;

		angle = Math.acos(quat.s) * 2;
		if (angle == 0)
		{
			axis = new Vector3(0, 1, 0);
		}
		else
		{
			double sa = Math.sin(angle * 0.5);
			axis = new Vector3(quat.x / sa, quat.y / sa, quat.z / sa);
		}
	}

	public void apply(Vector3 vec)
	{
		if (quat == null)
			quat = Quaternion.aroundAxis(axis, angle);

		vec.rotate(quat);
	}

	public void applyN(Vector3 normal)
	{
		apply(normal);
	}
}
