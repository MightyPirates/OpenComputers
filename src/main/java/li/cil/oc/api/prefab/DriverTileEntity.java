package li.cil.oc.api.prefab;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public abstract class DriverTileEntity implements li.cil.oc.api.driver.SidedBlock {
    public abstract Class<?> getTileEntityClass();

    /**
     * @deprecated Override {@link #worksWith(World, BlockPos, EnumFacing)} instead.
     */
    @Deprecated
    public boolean worksWith(final World world, final BlockPos pos) {
        return worksWith(world, pos, null);
    }

    /**
     * @deprecated Override {@link #createEnvironment(World, BlockPos, EnumFacing)} instead.
     */
    @Deprecated
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(final World world, final BlockPos pos) {
        return null;
    }

    // TODO Remove in OC 1.7
    // This is only here so we can implement SidedBlock without breaking every class deriving from this one.
    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(final World world, final BlockPos pos, final EnumFacing side) {
        return createEnvironment(world, pos);
    }

    @Override
    public boolean worksWith(final World world, final BlockPos pos, final EnumFacing side) {
        final Class<?> filter = getTileEntityClass();
        if (filter == null) {
            // This can happen if filter classes are deduced by reflection and
            // the class in question is not present.
            return false;
        }
        final TileEntity tileEntity = world.getTileEntity(pos);
        return tileEntity != null && filter.isAssignableFrom(tileEntity.getClass());
    }
}
