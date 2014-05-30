package li.cil.occ.mods.computercraft;

import li.cil.occ.OpenComponents;
import li.cil.occ.util.Reflection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public abstract class DriverPeripheral<TPeripheral> implements li.cil.oc.api.driver.Block {
    protected static final Set<Class<?>> blacklist = new HashSet<Class<?>>();

    static {
        for (String name : OpenComponents.peripheralBlacklist) {
            final Class<?> clazz = Reflection.getClass(name);
            if (clazz != null) {
                blacklist.add(clazz);
            }
        }
    }

    protected boolean isBlacklisted(final Object o) {
        for (Class<?> clazz : blacklist) {
            if (clazz.isInstance(o))
                return true;
        }
        return false;
    }

    protected abstract TPeripheral findPeripheral(World world, int x, int y, int z);

    @Override
    public boolean worksWith(final World world, final int x, final int y, final int z) {
        final TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        return tileEntity != null
                // This ensures we don't get duplicate components, in case the
                // tile entity is natively compatible with OpenComputers.
                && !li.cil.oc.api.network.Environment.class.isAssignableFrom(tileEntity.getClass())
                // The black list is used to avoid peripherals that are known
                // to be incompatible with OpenComputers when used directly.
                && !isBlacklisted(tileEntity)
                // Actual check if it's a peripheral.
                && findPeripheral(world, x, y, z) != null;
    }
}
