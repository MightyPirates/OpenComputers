package li.cil.oc.api.prefab;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public abstract class DriverTileEntity implements li.cil.oc.api.driver.Block {
    public abstract Class<?> getTileEntityClass();

    @Override
    public boolean worksWith(final World world, final int x, final int y, final int z) {
        final Class<?> filter = getTileEntityClass();
        if (filter == null) {
            // This can happen if filter classes are deduced by reflection and
            // the class in question is not present.
            return false;
        }
        final TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        return tileEntity != null && filter.isAssignableFrom(tileEntity.getClass());
    }
}
