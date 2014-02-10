package mods.railcraft.api.core;
// TODO: Add NBT functions

import net.minecraft.nbt.NBTTagCompound;

/**
 * This immutable class represents a point in the Minecraft world, while taking
 * into account the possibility of coordinates in different dimensions.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class WorldCoordinate {

    /**
     * The dimension
     */
    public final int dimension;
    /**
     * x-Coord
     */
    public final int x;
    /**
     * y-Coord
     */
    public final int y;
    /**
     * z-Coord
     */
    public final int z;

    /**
     * Creates a new WorldCoordinate
     *
     * @param dimension
     * @param x
     * @param y
     * @param z
     */
    public WorldCoordinate(int dimension, int x, int y, int z) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void writeToNBT(NBTTagCompound data, String tag) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("dim", dimension);
        nbt.setInteger("x", x);
        nbt.setInteger("y", y);
        nbt.setInteger("z", z);
        data.setCompoundTag(tag, nbt);
    }

    public static WorldCoordinate readFromNBT(NBTTagCompound data, String tag) {
        if (data.hasKey(tag)) {
            NBTTagCompound nbt = data.getCompoundTag(tag);
            int dim = nbt.getInteger("dim");
            int x = nbt.getInteger("x");
            int y = nbt.getInteger("y");
            int z = nbt.getInteger("z");
            return new WorldCoordinate(dim, x, y, z);
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WorldCoordinate other = (WorldCoordinate) obj;
        if (this.dimension != other.dimension) {
            return false;
        }
        if (this.x != other.x) {
            return false;
        }
        if (this.y != other.y) {
            return false;
        }
        if (this.z != other.z) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + this.dimension;
        hash = 13 * hash + this.x;
        hash = 13 * hash + this.y;
        hash = 13 * hash + this.z;
        return hash;
    }

    @Override
    public String toString() {
        return "WorldCoordinate{" + "dimension=" + dimension + ", x=" + x + ", y=" + y + ", z=" + z + '}';
    }



    
}
