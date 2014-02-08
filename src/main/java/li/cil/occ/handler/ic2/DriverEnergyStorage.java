package li.cil.occ.handler.ic2;

import ic2.api.tile.IEnergyStorage;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.handler.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverEnergyStorage extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnergyStorage.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IEnergyStorage) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IEnergyStorage> {
        public Environment(final IEnergyStorage tileEntity) {
            super(tileEntity, "energy_storage");
        }

        @Callback
        public Object[] getCapacity(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getCapacity()};
        }

        @Callback
        public Object[] getOutput(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOutput()};
        }

        @Callback
        public Object[] getStored(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getStored()};
        }
    }
}
