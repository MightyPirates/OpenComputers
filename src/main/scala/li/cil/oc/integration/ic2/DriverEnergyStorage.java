package li.cil.oc.integration.ic2;

import ic2.api.tile.IEnergyStorage;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverEnergyStorage extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnergyStorage.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IEnergyStorage) world.getTileEntity(x, y, z));
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
