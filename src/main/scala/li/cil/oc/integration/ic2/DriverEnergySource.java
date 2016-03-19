package li.cil.oc.integration.ic2;

import ic2.api.energy.tile.IEnergySource;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverEnergySource extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnergySource.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IEnergySource) world.getTileEntity(x, y, z));
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
