package li.cil.oc.driver.ic2;

import ic2.api.energy.tile.IEnergySource;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import li.cil.oc.driver.TileEntityDriver;
import net.minecraft.world.World;

public final class DriverEnergySource extends TileEntityDriver {
    @Override
    public Class<?> getFilterClass() {
        return IEnergySource.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IEnergySource) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IEnergySource> {
        public Environment(final IEnergySource tileEntity) {
            super(tileEntity, "energy_source");
        }

        @Callback
        public Object[] getOfferedEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOfferedEnergy()};
        }
    }
}
