package li.cil.occ.mods.cofh.tileentity;

import cofh.api.tileentity.IEnergyInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverEnergyInfo extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnergyInfo.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IEnergyInfo) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IEnergyInfo> {
        public Environment(final IEnergyInfo tileEntity) {
            super(tileEntity, "energy_info");
        }

        @Callback(doc = "function():number --  Returns the amount of stored energy.")
        public Object[] getEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getInfoEnergyStored()};
        }

        @Callback(doc = "function():number --  Returns the energy per tick.")
        public Object[] getEnergyPerTick(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getInfoEnergyPerTick()};
        }

        @Callback(doc = "function():number --  Returns the maximum energy.")
        public Object[] getMaxEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getInfoMaxEnergyStored()};
        }

        @Callback(doc = "function():number --  Returns the maximum energy per tick.")
        public Object[] getMaxEnergyPerTick(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getInfoMaxEnergyPerTick()};
        }
    }
}
