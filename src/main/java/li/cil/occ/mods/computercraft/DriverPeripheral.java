package li.cil.occ.mods.computercraft;

import dan200.computer.api.IPeripheral;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.api.prefab.ManagedPeripheral;
import li.cil.occ.OpenComponents;
import li.cil.occ.util.Reflection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public class DriverPeripheral extends DriverTileEntity {
    private static final Set<Class<?>> blacklist = new HashSet<Class<?>>();

    static {
        for (String name : OpenComponents.peripheralBlacklist) {
            final Class<?> clazz = Reflection.getClass(name);
            if (clazz != null) {
                blacklist.add(clazz);
            }
        }
    }

    @Override
    public Class<?> getTileEntityClass() {
        return IPeripheral.class;
    }

    @Override
    public boolean worksWith(final World world, final int x, final int y, final int z) {
        final TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        return tileEntity != null && !blacklist.contains(tileEntity.getClass()) && super.worksWith(world, x, y, z);
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new ManagedPeripheral((IPeripheral) world.getBlockTileEntity(x, y, z));
    }
}
