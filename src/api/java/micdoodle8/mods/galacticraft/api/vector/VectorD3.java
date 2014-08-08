package micdoodle8.mods.galacticraft.api.vector;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

public class VectorD3
{
	public Vector3 position;
	public int dimensionID;

	public VectorD3(double x, double y, double z, int dimID)
	{
		this.position = new Vector3(x, y, z);
		this.dimensionID = dimID;
	}

	public VectorD3()
	{
		this(0, 0, 0, 0);
	}

	public VectorD3(VectorD3 vector)
	{
		this(vector.position.x, vector.position.y, vector.position.z, vector.dimensionID);
	}

	public VectorD3(Vector3 vector, int dimID)
	{
		this(vector.x, vector.y, vector.z, dimID);
	}

	public VectorD3(double amount)
	{
		this(amount, amount, amount, 0);
	}

	public VectorD3(Entity par1)
	{
		this(par1.posX, par1.posY, par1.posZ, 0);
	}

	public VectorD3(TileEntity par1)
	{
		this(par1.xCoord, par1.yCoord, par1.zCoord, 0);
	}

	public VectorD3(Vec3 par1)
	{
		this(par1.xCoord, par1.yCoord, par1.zCoord, 0);

	}

	public VectorD3(MovingObjectPosition par1)
	{
		this(par1.blockX, par1.blockY, par1.blockZ, 0);
	}

	public VectorD3(ChunkCoordinates par1)
	{
		this(par1.posX, par1.posY, par1.posZ, 0);
	}

	public VectorD3(ForgeDirection direction)
	{
		this(direction.offsetX, direction.offsetY, direction.offsetZ, 0);
	}

	public VectorD3(NBTTagCompound nbt)
	{
		this(nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"), nbt.getInteger("dimID"));
	}

	public VectorD3(float rotationYaw, float rotationPitch)
	{
		this(Math.cos(Math.toRadians(rotationYaw + 90)), Math.sin(Math.toRadians(-rotationPitch)), Math.sin(Math.toRadians(rotationYaw + 90)), 0);
	}

	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		this.position.writeToNBT(nbt);
		nbt.setInteger("dimID", this.dimensionID);
		return nbt;
	}

	@Override
	public final VectorD3 clone()
	{
		return new VectorD3(this);
	}
}
