package li.cil.oc.integration.enderio;

import crazypants.enderio.machine.power.TileCapacitorBank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverCapacitor extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileCapacitorBank.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileCapacitorBank) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileCapacitorBank> {
        public Environment(final TileCapacitorBank tileEntity) {
            super(tileEntity, "enderio_capacitor");
        }

        @Callback(doc = "function():number -- Returns the amount of energy stored in the capacitor bank.")
        public Object[] getEnergyStored(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getEnergyStored()};
        }

        @Callback(doc = "function():number -- Returns the maximum amount of energy the capacitor bank can store.")
        public Object[] getMaxEnergyStored(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMaxEnergyStored()};
        }
    }
}
