package li.cil.oc.api.prefab;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @deprecated Use {@link DriverSidedTileEntity} instead.
 */
@Deprecated // TODO Remove in OC 1.7
public abstract class DriverTileEntity implements li.cil.oc.api.driver.Block {
    public abstract Class<?> getTileEntityClass();

    @Override
    public boolean worksWith(final World world, final BlockPos pos) {
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
