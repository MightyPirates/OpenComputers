package li.cil.oc.driver.thermalexpansion;

import cofh.api.tileentity.IEnergyInfo;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverEnergyInfo extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnergyInfo.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IEnergyInfo) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IEnergyInfo> {
        public Environment(final IEnergyInfo tileEntity) {
            super(tileEntity, "energy_info");
        }

        @Callback
        public Object[] getEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getEnergy()};
        }

        @Callback
        public Object[] getEnergyPerTick(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getEnergyPerTick()};
        }

        @Callback
        public Object[] getMaxEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMaxEnergy()};
        }

        @Callback
        public Object[] getMaxEnergyPerTick(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getMaxEnergyPerTick()};
        }
    }
}
