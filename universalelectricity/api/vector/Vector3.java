package universalelectricity.api.vector;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

/**
 * Vector3 Class is used for defining objects in a 3D space.
 * 
 * @author Calclavia
 */

public class Vector3 implements Cloneable
{
	public double x;
	public double y;
	public double z;

	public Vector3(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3()
	{
		this(0, 0, 0);
	}

	public Vector3(Vector3 vector)
	{
		this(vector.x, vector.y, vector.z);
	}

	public Vector3(double amount)
	{
		this(amount, amount, amount);
	}

	public Vector3(Entity par1)
	{
		this(par1.posX, par1.posY, par1.posZ);
	}

	public Vector3(TileEntity par1)
	{
		this(par1.xCoord, par1.yCoord, par1.zCoord);
	}

	public Vector3(Vec3 par1)
	{
		this(par1.xCoord, par1.yCoord, par1.zCoord);

	}

	public Vector3(MovingObjectPosition par1)
	{
		this(par1.blockX, par1.blockY, par1.blockZ);
	}

	public Vector3(ChunkCoordinates par1)
	{
		this(par1.posX, par1.posY, par1.posZ);
	}

	public Vector3(ForgeDirection direction)
	{
		this(direction.offsetX, direction.offsetY, direction.offsetZ);
	}

	/** Loads a Vector3 from an NBT compound. */
	public Vector3(NBTTagCompound nbt)
	{
		this(nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"));
	}

	/**
	 * Get a Vector3 based on the rotationYaw and rotationPitch.
	 * 
	 * @param rotationYaw - Degree
	 * @param rotationPitch- Degree
	 */
	public Vector3(float rotationYaw, float rotationPitch)
	{
		this(Math.cos(Math.toRadians(rotationYaw + 90)), Math.sin(Math.toRadians(-rotationPitch)), Math.sin(Math.toRadians(rotationYaw + 90)));
	}

	public static Vector3 fromCenter(Entity e)
	{
		return new Vector3(e.posX, e.posY - e.yOffset + e.height / 2, e.posZ);
	}

	public static Vector3 fromCenter(TileEntity e)
	{
		return new Vector3(e.xCoord + 0.5, e.yCoord + 0.5, e.zCoord + 0.5);
	}

	/** Returns the coordinates as integers, ideal for block placement. */
	public int intX()
	{
		return (int) Math.floor(this.x);
	}

	public int intY()
	{
		return (int) Math.floor(this.y);
	}

	public int intZ()
	{
		return (int) Math.floor(this.z);
	}

	public float floatX()
	{
		return (float) this.x;
	}

	public float floatY()
	{
		return (float) this.y;
	}

	public float floatZ()
	{
		return (float) this.z;
	}

	/** Makes a new copy of this Vector. Prevents variable referencing problems. */
	@Override
	public Vector3 clone()
	{
		return new Vector3(this);
	}

	/**
	 * Easy block access functions.
	 * 
	 * @param world
	 * @return
	 */
	public int getBlockID(IBlockAccess world)
	{
		return world.getBlockId(this.intX(), this.intY(), this.intZ());
	}

	public int getBlockMetadata(IBlockAccess world)
	{
		return world.getBlockMetadata(this.intX(), this.intY(), this.intZ());
	}

	public TileEntity getTileEntity(IBlockAccess world)
	{
		return world.getBlockTileEntity(this.intX(), this.intY(), this.intZ());
	}

	public boolean setBlock(World world, int id, int metadata, int notify)
	{
		return world.setBlock(this.intX(), this.intY(), this.intZ(), id, metadata, notify);
	}

	public boolean setBlock(World world, int id, int metadata)
	{
		return this.setBlock(world, id, metadata, 3);
	}

	public boolean setBlock(World world, int id)
	{
		return this.setBlock(world, id, 0);
	}

	/** ---------------------- CONVERSION FUNCTIONS ---------------------------- */
	/** Converts this Vector3 into a Vector2 by dropping the Y axis. */
	public Vector2 toVector2()
	{
		return new Vector2(this.x, this.z);
	}

	/** Converts this vector three into a Minecraft Vec3 object */
	public Vec3 toVec3()
	{
		return Vec3.createVectorHelper(this.x, this.y, this.z);
	}

	/**
	 * Saves this Vector3 to disk
	 * 
	 * @param prefix - The prefix of this save. Use some unique string.
	 * @param nbt - The NBT compound object to save the data in
	 */
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		nbt.setDouble("x", this.x);
		nbt.setDouble("y", this.y);
		nbt.setDouble("z", this.z);
		return nbt;
	}

	/** Converts Vector3 into a ForgeDirection. */
	public ForgeDirection toForgeDirection()
	{
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS)
		{
			if (this.x == direction.offsetX && this.y == direction.offsetY && this.z == direction.offsetZ)
			{
				return direction;
			}
		}

		return ForgeDirection.UNKNOWN;
	}

	/** ---------------------- MAGNITUDE FUNCTIONS ---------------------------- */
	public double getMagnitude()
	{
		return Math.sqrt(this.getMagnitudeSquared());
	}

	public double getMagnitudeSquared()
	{
		return this.x * this.x + this.y * this.y + this.z * this.z;
	}

	public Vector3 normalize()
	{
		double d = this.getMagnitude();

		if (d != 0)
		{
			this.scale(1 / d);
		}

		return this;
	}

	/** Gets the distance between two vectors */
	public static double distance(Vector3 vec1, Vector3 vec2)
	{
		return vec1.distance(vec2);
	}

	public double distance(double x, double y, double z)
	{
		Vector3 difference = this.clone().difference(x, y, z);
		return difference.getMagnitude();
	}

	public double distance(Vector3 compare)
	{
		return this.distance(compare.x, compare.y, compare.z);
	}

	public double distance(Entity entity)
	{
		return this.distance(entity.posX, entity.posY, entity.posZ);
	}

	/** Multiplies the vector by negative one. */
	public Vector3 invert()
	{
		this.scale(-1);
		return this;
	}

	/**
	 * Gets a position relative to a position's side
	 * 
	 * @param position - The position
	 * @param side - The side. 0-5
	 * @return The position relative to the original position's side
	 */
	public Vector3 translate(ForgeDirection side, double amount)
	{
		return this.translate(new Vector3(side).scale(amount));
	}

	public Vector3 translate(ForgeDirection side)
	{
		return this.translate(side, 1);
	}

	@Deprecated
	public Vector3 modifyPositionFromSide(ForgeDirection side, double amount)
	{
		return this.translate(side, amount);
	}

	@Deprecated
	public Vector3 modifyPositionFromSide(ForgeDirection side)
	{
		return this.translate(side);
	}

	public Vector3 translate(Vector3 addition)
	{
		this.x += addition.x;
		this.y += addition.y;
		this.z += addition.z;
		return this;
	}

	public Vector3 translate(double x, double y, double z)
	{
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}

	public Vector3 translate(double addition)
	{
		this.x += addition;
		this.y += addition;
		this.z += addition;
		return this;
	}

	public static Vector3 translate(Vector3 first, Vector3 second)
	{
		return first.clone().translate(second);
	}

	public static Vector3 translate(Vector3 translate, double addition)
	{
		return translate.clone().translate(addition);
	}

	public Vector3 add(Vector3 amount)
	{
		return translate(amount);
	}

	public Vector3 add(double amount)
	{
		return translate(amount);
	}

	public Vector3 subtract(Vector3 amount)
	{
		return this.translate(amount.clone().invert());
	}

	public Vector3 subtract(double amount)
	{
		return this.translate(-amount);
	}

	public Vector3 subtract(double x, double y, double z)
	{
		return this.difference(x, y, z);
	}

	public Vector3 difference(Vector3 amount)
	{
		return this.translate(amount.clone().invert());
	}

	public Vector3 difference(double amount)
	{
		return this.translate(-amount);
	}

	public Vector3 difference(double x, double y, double z)
	{
		this.x -= x;
		this.y -= y;
		this.z -= z;
		return this;
	}

	public Vector3 scale(double amount)
	{
		this.x *= amount;
		this.y *= amount;
		this.z *= amount;
		return this;
	}

	public Vector3 scale(double x, double y, double z)
	{
		this.x *= x;
		this.y *= y;
		this.z *= z;
		return this;
	}

	public Vector3 scale(Vector3 amount)
	{
		this.x *= amount.x;
		this.y *= amount.y;
		this.z *= amount.z;
		return this;
	}

	public static Vector3 scale(Vector3 vec, double amount)
	{
		return vec.scale(amount);
	}

	public static Vector3 scale(Vector3 vec, Vector3 amount)
	{
		return vec.scale(amount);
	}

	public Vector3 round()
	{
		return new Vector3(Math.round(this.x), Math.round(this.y), Math.round(this.z));
	}

	public Vector3 ceil()
	{
		return new Vector3(Math.ceil(this.x), Math.ceil(this.y), Math.ceil(this.z));
	}

	public Vector3 floor()
	{
		return new Vector3(Math.floor(this.x), Math.floor(this.y), Math.floor(this.z));
	}

	public Vector3 toRound()
	{
		this.x = Math.round(this.x);
		this.y = Math.round(this.y);
		this.z = Math.round(this.z);
		return this;
	}

	public Vector3 toCeil()
	{
		this.x = Math.ceil(this.x);
		this.y = Math.ceil(this.y);
		this.z = Math.ceil(this.z);
		return this;
	}

	public Vector3 toFloor()
	{
		this.x = Math.floor(this.x);
		this.y = Math.floor(this.y);
		this.z = Math.floor(this.z);
		return this;
	}

	/** Gets all entities inside of this position in block space. */
	public List<Entity> getEntitiesWithin(World worldObj, Class<? extends Entity> par1Class)
	{
		return worldObj.getEntitiesWithinAABB(par1Class, AxisAlignedBB.getBoundingBox(this.intX(), this.intY(), this.intZ(), this.intX() + 1, this.intY() + 1, this.intZ() + 1));
	}

	/**
	 * Cross product functions
	 * 
	 * @return The cross product between this vector and another.
	 */
	public Vector3 toCrossProduct(Vector3 compare)
	{
		double newX = this.y * compare.z - this.z * compare.y;
		double newY = this.z * compare.x - this.x * compare.z;
		double newZ = this.x * compare.y - this.y * compare.x;
		this.x = newX;
		this.y = newY;
		this.z = newZ;
		return this;
	}

	public Vector3 crossProduct(Vector3 compare)
	{
		return this.clone().toCrossProduct(compare);
	}

	public Vector3 xCrossProduct()
	{
		return new Vector3(0.0D, this.z, -this.y);
	}

	public Vector3 zCrossProduct()
	{
		return new Vector3(-this.y, this.x, 0.0D);
	}

	public double dotProduct(Vector3 vec2)
	{
		return this.x * vec2.x + this.y * vec2.y + this.z * vec2.z;
	}

	/** @return The perpendicular vector. */
	public Vector3 getPerpendicular()
	{
		if (this.z == 0.0F)
		{
			return this.zCrossProduct();
		}

		return this.xCrossProduct();
	}

	/** @return True if this Vector3 is zero. */
	public boolean isZero()
	{
		return this.equals(ZERO());
	}

	/**
	 * Rotate by a this vector around an axis.
	 * 
	 * @return The new Vector3 rotation.
	 */
	public Vector3 rotate(float angle, Vector3 axis)
	{
		return translateMatrix(getRotationMatrix(angle, axis), this);
	}

	public double[] getRotationMatrix(float angle)
	{
		double[] matrix = new double[16];
		Vector3 axis = this.clone().normalize();
		double x = axis.x;
		double y = axis.y;
		double z = axis.z;
		angle *= 0.0174532925D;
		float cos = (float) Math.cos(angle);
		float ocos = 1.0F - cos;
		float sin = (float) Math.sin(angle);
		matrix[0] = (x * x * ocos + cos);
		matrix[1] = (y * x * ocos + z * sin);
		matrix[2] = (x * z * ocos - y * sin);
		matrix[4] = (x * y * ocos - z * sin);
		matrix[5] = (y * y * ocos + cos);
		matrix[6] = (y * z * ocos + x * sin);
		matrix[8] = (x * z * ocos + y * sin);
		matrix[9] = (y * z * ocos - x * sin);
		matrix[10] = (z * z * ocos + cos);
		matrix[15] = 1.0F;
		return matrix;
	}

	public static Vector3 translateMatrix(double[] matrix, Vector3 translation)
	{
		double x = translation.x * matrix[0] + translation.y * matrix[1] + translation.z * matrix[2] + matrix[3];
		double y = translation.x * matrix[4] + translation.y * matrix[5] + translation.z * matrix[6] + matrix[7];
		double z = translation.x * matrix[8] + translation.y * matrix[9] + translation.z * matrix[10] + matrix[11];
		translation.x = x;
		translation.y = y;
		translation.z = z;
		return translation;
	}

	public static double[] getRotationMatrix(float angle, Vector3 axis)
	{
		return axis.getRotationMatrix(angle);
	}

	/** Rotates this Vector by a yaw, pitch and roll value. */
	public void rotate(double yaw, double pitch, double roll)
	{
		double yawRadians = Math.toRadians(yaw);
		double pitchRadians = Math.toRadians(pitch);
		double rollRadians = Math.toRadians(roll);

		double x = this.x;
		double y = this.y;
		double z = this.z;

		this.x = x * Math.cos(yawRadians) * Math.cos(pitchRadians) + z * (Math.cos(yawRadians) * Math.sin(pitchRadians) * Math.sin(rollRadians) - Math.sin(yawRadians) * Math.cos(rollRadians)) + y * (Math.cos(yawRadians) * Math.sin(pitchRadians) * Math.cos(rollRadians) + Math.sin(yawRadians) * Math.sin(rollRadians));
		this.z = x * Math.sin(yawRadians) * Math.cos(pitchRadians) + z * (Math.sin(yawRadians) * Math.sin(pitchRadians) * Math.sin(rollRadians) + Math.cos(yawRadians) * Math.cos(rollRadians)) + y * (Math.sin(yawRadians) * Math.sin(pitchRadians) * Math.cos(rollRadians) - Math.cos(yawRadians) * Math.sin(rollRadians));
		this.y = -x * Math.sin(pitchRadians) + z * Math.cos(pitchRadians) * Math.sin(rollRadians) + y * Math.cos(pitchRadians) * Math.cos(rollRadians);
	}

	/** Rotates a point by a yaw and pitch around the anchor 0,0 by a specific angle. */
	public void rotate(double yaw, double pitch)
	{
		this.rotate(yaw, pitch, 0);
	}

	public void rotate(double yaw)
	{
		double yawRadians = Math.toRadians(yaw);

		double x = this.x;
		double z = this.z;

		if (yaw != 0)
		{
			this.x = x * Math.cos(yawRadians) - z * Math.sin(yawRadians);
			this.z = x * Math.sin(yawRadians) + z * Math.cos(yawRadians);
		}
	}

	public Vector3 rotate(Quaternion rotator)
	{
		rotator.rotate(this);
		return this;
	}

	/**
	 * Gets the delta look position based on the rotation yaw and pitch. Minecraft coordinates are
	 * messed up. Y and Z are flipped. Yaw is displaced by 90 degrees. Pitch is inversed.
	 * 
	 * @param rotationYaw
	 * @param rotationPitch
	 */
	public static Vector3 getDeltaPositionFromRotation(float rotationYaw, float rotationPitch)
	{
		return new Vector3(rotationYaw, rotationPitch);
	}

	/**
	 * Gets the angle between this vector and another vector.
	 * 
	 * @return Angle in degrees
	 */
	public double getAngle(Vector3 vec2)
	{
		return anglePreNorm(this.clone().normalize(), vec2.clone().normalize());
	}

	public static double getAngle(Vector3 vec1, Vector3 vec2)
	{
		return vec1.getAngle(vec2);
	}

	public double anglePreNorm(Vector3 vec2)
	{
		return Math.acos(this.dotProduct(vec2));
	}

	public static double anglePreNorm(Vector3 vec1, Vector3 vec2)
	{
		return Math.acos(vec1.clone().dotProduct(vec2));
	}

	public static Vector3 UP()
	{
		return new Vector3(0, 1, 0);
	}

	public static Vector3 DOWN()
	{
		return new Vector3(0, -1, 0);
	}

	public static Vector3 NORTH()
	{
		return new Vector3(0, 0, -1);
	}

	public static Vector3 SOUTH()
	{
		return new Vector3(0, 0, 1);
	}

	public static Vector3 WEST()
	{
		return new Vector3(-1, 0, 0);
	}

	public static Vector3 EAST()
	{
		return new Vector3(1, 0, 0);
	}

	public static Vector3 ZERO()
	{
		return new Vector3(0, 0, 0);
	}

	public static Vector3 CENTER()
	{
		return new Vector3(0.5, 0.5, 0.5);
	}

	/**
	 * RayTrace Code, retrieved from MachineMuse.
	 * 
	 * @author MachineMuse
	 */
	public MovingObjectPosition rayTrace(World world, float rotationYaw, float rotationPitch, boolean collisionFlag, double reachDistance)
	{
		// Somehow this destroys the playerPosition vector -.-
		MovingObjectPosition pickedBlock = this.rayTraceBlocks(world, rotationYaw, rotationPitch, collisionFlag, reachDistance);
		MovingObjectPosition pickedEntity = this.rayTraceEntities(world, rotationYaw, rotationPitch, reachDistance);

		if (pickedBlock == null)
		{
			return pickedEntity;
		}
		else if (pickedEntity == null)
		{
			return pickedBlock;
		}
		else
		{
			double dBlock = this.distance(new Vector3(pickedBlock.hitVec));
			double dEntity = this.distance(new Vector3(pickedEntity.hitVec));

			if (dEntity < dBlock)
			{
				return pickedEntity;
			}
			else
			{
				return pickedBlock;
			}
		}
	}

	public MovingObjectPosition rayTraceBlocks(World world, float rotationYaw, float rotationPitch, boolean collisionFlag, double reachDistance)
	{
		Vector3 lookVector = Vector3.getDeltaPositionFromRotation(rotationYaw, rotationPitch);
		Vector3 reachPoint = this.clone().translate(lookVector.clone().scale(reachDistance));
		return world.rayTraceBlocks_do_do(this.toVec3(), reachPoint.toVec3(), collisionFlag, !collisionFlag);
	}

	@Deprecated
	public MovingObjectPosition rayTraceEntities(World world, float rotationYaw, float rotationPitch, boolean collisionFlag, double reachDistance)
	{
		return this.rayTraceEntities(world, rotationYaw, rotationPitch, reachDistance);
	}

	public MovingObjectPosition rayTraceEntities(World world, float rotationYaw, float rotationPitch, double reachDistance)
	{
		return this.rayTraceEntities(world, getDeltaPositionFromRotation(rotationYaw, rotationPitch).scale(reachDistance));
	}

	/**
	 * Does an entity raytrace.
	 * 
	 * @param world - The world object.
	 * @param target - The rotation in terms of Vector3. Convert using
	 * getDeltaPositionFromRotation()
	 * @return The target hit.
	 */
	public MovingObjectPosition rayTraceEntities(World world, Vector3 target)
	{
		MovingObjectPosition pickedEntity = null;
		Vec3 startingPosition = this.toVec3();
		Vec3 look = target.toVec3();
		double reachDistance = this.distance(target);
		Vec3 reachPoint = Vec3.createVectorHelper(startingPosition.xCoord + look.xCoord * reachDistance, startingPosition.yCoord + look.yCoord * reachDistance, startingPosition.zCoord + look.zCoord * reachDistance);

		double checkBorder = 1.1 * reachDistance;
		AxisAlignedBB boxToScan = AxisAlignedBB.getAABBPool().getAABB(-checkBorder, -checkBorder, -checkBorder, checkBorder, checkBorder, checkBorder).offset(this.x, this.y, this.z);

		@SuppressWarnings("unchecked")
		List<Entity> entitiesHit = world.getEntitiesWithinAABBExcludingEntity(null, boxToScan);
		double closestEntity = reachDistance;

		if (entitiesHit == null || entitiesHit.isEmpty())
		{
			return null;
		}
		for (Entity entityHit : entitiesHit)
		{
			if (entityHit != null && entityHit.canBeCollidedWith() && entityHit.boundingBox != null)
			{
				float border = entityHit.getCollisionBorderSize();
				AxisAlignedBB aabb = entityHit.boundingBox.expand(border, border, border);
				MovingObjectPosition hitMOP = aabb.calculateIntercept(startingPosition, reachPoint);

				if (hitMOP != null)
				{
					if (aabb.isVecInside(startingPosition))
					{
						if (0.0D < closestEntity || closestEntity == 0.0D)
						{
							pickedEntity = new MovingObjectPosition(entityHit);
							if (pickedEntity != null)
							{
								pickedEntity.hitVec = hitMOP.hitVec;
								closestEntity = 0.0D;
							}
						}
					}
					else
					{
						double distance = startingPosition.distanceTo(hitMOP.hitVec);

						if (distance < closestEntity || closestEntity == 0.0D)
						{
							pickedEntity = new MovingObjectPosition(entityHit);
							pickedEntity.hitVec = hitMOP.hitVec;
							closestEntity = distance;
						}
					}
				}
			}
		}
		return pickedEntity;
	}

	@Override
	public int hashCode()
	{
		return ("X:" + this.x + "Y:" + this.y + "Z:" + this.z).hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Vector3)
		{
			Vector3 vector3 = (Vector3) o;
			return this.x == vector3.x && this.y == vector3.y && this.z == vector3.z;
		}

		return false;
	}

	@Override
	public String toString()
	{
		return "Vector3 [" + this.x + "," + this.y + "," + this.z + "]";
	}

}