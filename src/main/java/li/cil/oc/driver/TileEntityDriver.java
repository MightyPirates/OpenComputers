package li.cil.oc.driver;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public abstract class TileEntityDriver implements li.cil.oc.api.driver.Block {
    public abstract Class<?> getFilterClass();

    @Override
    public boolean worksWith(final World world, final int x, final int y, final int z) {
        final TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        return tileEntity != null && getFilterClass().isAssignableFrom(tileEntity.getClass());
    }
}
