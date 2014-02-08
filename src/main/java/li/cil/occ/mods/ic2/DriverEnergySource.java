package li.cil.occ.mods.ic2;

import ic2.api.energy.tile.IEnergySource;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverEnergySource extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
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
