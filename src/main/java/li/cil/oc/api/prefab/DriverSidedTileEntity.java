package li.cil.oc.api.prefab;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/**
 * To limit sidedness, I recommend overriding {@link #worksWith(World, BlockPos, EnumFacing)}
 * and calling <code>super.worksWith</code> in addition to the side check.
 */
public abstract class DriverSidedTileEntity implements li.cil.oc.api.driver.SidedBlock {
    public abstract Class<?> getTileEntityClass();

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
