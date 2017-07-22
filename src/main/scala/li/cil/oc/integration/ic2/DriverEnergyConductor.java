package li.cil.oc.integration.ic2;

import ic2.api.energy.tile.IEnergyConductor;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverEnergyConductor extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnergyConductor.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IEnergyConductor) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IEnergyConductor> {
        public Environment(final IEnergyConductor tileEntity) {
            super(tileEntity, "energy_conductor");
        }

        @Callback
        public Object[] getConductionLoss(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getConductionLoss()};
        }

        @Callback
        public Object[] getConductorBreakdownEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getConductorBreakdownEnergy()};
        }

        @Callback
        public Object[] getInsulationBreakdownEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getInsulationBreakdownEnergy()};
        }

        @Callback
        public Object[] getInsulationEnergyAbsorption(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getInsulationEnergyAbsorption()};
        }
    }
}
