package li.cil.occ.mods.computercraft;

import dan200.computer.api.IPeripheral;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.api.prefab.ManagedPeripheral;
import net.minecraft.world.World;

public class DriverPeripheral extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IPeripheral.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new ManagedPeripheral((IPeripheral) world.getBlockTileEntity(x, y, z));
    }
}
