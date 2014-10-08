package li.cil.occ.mods.enderio;

import crazypants.enderio.power.ICapacitor;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverCapacitor extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return ICapacitor.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((ICapacitor) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<ICapacitor> {
        public Environment(final ICapacitor tileEntity) {
            super(tileEntity, "enderio_capacitor");
        }

        @Callback(doc = "function():number -- Returns the minimum amount of energy the capacitor can receive.")
        public Object[] getMinEnergyReceived(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMinEnergyReceived()};
        }

        @Callback(doc = "function():number -- Returns the maximum amount of energy the capacitor can receive.")
        public Object[] getMaxEnergyReceived(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMaxEnergyReceived()};
        }

        @Callback(doc = "function():number -- Returns the maximum amount of energy the capacitor can store.")
        public Object[] getMaxEnergyStored(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMaxEnergyStored()};
        }

        @Callback(doc = "function():number -- Returns the minimal activation energy.")
        public Object[] getMinActivationEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMinActivationEnergy()};
        }

        @Callback(doc = "function():number -- Returns the power loss.")
        public Object[] getPowerLoss(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getPowerLoss()};
        }

        @Callback(doc = "function():number -- Returns the regularity of the power loss.")
        public Object[] getPowerLossRegularity(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getPowerLossRegularity()};
        }

        @Callback(doc = "function():number -- Returns the maximum amount of energy that can be extracted from the capacitor.")
        public Object[] getMaxEnergyExtracted(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMaxEnergyExtracted()};
        }
    }
}
