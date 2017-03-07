package li.cil.oc.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public final class CapabilityUtils {
    @Nullable
    public static <T> T getCapability(final IBlockAccess world, final BlockPos pos, final Capability<T> capability, @Nullable final EnumFacing side) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity == null) {
            return null;
        }
        return tileEntity.getCapability(capability, side);
    }

    @Nullable
    public static <T> T getCapability(final World world, final BlockPos pos, final Capability<T> capability, @Nullable final EnumFacing side) {
        if (world.isBlockLoaded(pos)) {
            return getCapability((IBlockAccess) world, pos, capability, side);
        } else {
            return null;
        }
    }

    // ----------------------------------------------------------------------- //

    private CapabilityUtils() {
    }
}
