package codechicken.lib.vec;


import net.minecraft.tileentity.TileEntity;

// Fake dummy class (it's not shipped, only used in compilation), because
// I can't get gradle to give a shit about dependencies in the API compilation
// tasks.
public class Vector3 {
    public double x;
    public double y;
    public double z;

    public Vector3(final double x, final double y, final double z) {
    }

    public static Vector3 fromTileEntityCenter(final TileEntity tileEntity) {
        return null;
    }
}
