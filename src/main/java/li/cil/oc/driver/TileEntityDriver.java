package li.cil.oc.driver;

import li.cil.oc.util.TileEntityLookup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public abstract class TileEntityDriver implements li.cil.oc.api.driver.Block {
    public abstract Class<?> getFilterClass();

    @Override
    public boolean worksWith(final World world, final ItemStack stack) {
        final Class clazz = TileEntityLookup.get(world, stack);
        return clazz != null && getFilterClass().isAssignableFrom(clazz);
    }

    @Override
    public boolean worksWith(final World world, final int x, final int y, final int z) {
        final TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        return tileEntity != null && getFilterClass().isAssignableFrom(tileEntity.getClass());
    }
}
