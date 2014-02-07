package li.cil.oc.driver.ic2;

import ic2.api.energy.tile.IEnergySink;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverEnergySink extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnergySink.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IEnergySink) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IEnergySink> {
        public Environment(final IEnergySink tileEntity) {
            super(tileEntity, "energy_sink");
        }

        @Callback
        public Object[] getMaxSafeInput(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMaxSafeInput()};
        }
    }
}
