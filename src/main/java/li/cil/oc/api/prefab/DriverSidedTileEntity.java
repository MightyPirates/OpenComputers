package li.cil.oc.api.prefab;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * To limit sidedness, I recommend overriding {@link #worksWith(World, int, int, int, ForgeDirection)}
 * and calling <code>super.worksWith</code> in addition to the side check.
 */
public abstract class DriverSidedTileEntity implements li.cil.oc.api.driver.SidedBlock {
    public abstract Class<?> getTileEntityClass();

    @Override
    public boolean worksWith(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        final Class<?> filter = getTileEntityClass();
        if (filter == null) {
            // This can happen if filter classes are deduced by reflection and
            // the class in question is not present.
            return false;
        }
        final TileEntity tileEntity = world.getTileEntity(x, y, z);
        return tileEntity != null && filter.isAssignableFrom(tileEntity.getClass());
    }
}
