package li.cil.oc.util;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class BlockPosUtils {
    public static EnumFacing getNeighborSide(final BlockPos pos, final BlockPos neighborPos) {
        final BlockPos relative = neighborPos.subtract(pos);
        assert (Math.abs(relative.getX()) + Math.abs(relative.getY()) + Math.abs(relative.getZ())) == 1 : "neighborPos is not a neighbor of pos.";

        if (relative.getX() < 0) {
            return EnumFacing.WEST;
        }
        if (relative.getX() > 0) {
            return EnumFacing.EAST;
        }
        if (relative.getY() < 0) {
            return EnumFacing.DOWN;
        }
        if (relative.getY() > 0) {
            return EnumFacing.UP;
        }
        if (relative.getZ() < 0) {
            return EnumFacing.NORTH;
        }
        if (relative.getZ() > 0) {
            return EnumFacing.SOUTH;
        }

        throw new IllegalArgumentException(String.format("neighborPos (%s) is not a neighbor of pos (%s).", neighborPos, pos));
    }

    public static Vec3d getCenter(final BlockPos pos) {
        return new Vec3d(pos).addVector(0.5, 0.5, 0.5);
    }

    // ----------------------------------------------------------------------- //

    private BlockPosUtils() {
    }
}
