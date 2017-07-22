package li.cil.oc.integration.cofh.energy;

import cofh.api.energy.IEnergyHandler;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverEnergyHandler extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnergyHandler.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IEnergyHandler) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IEnergyHandler> {
        public Environment(final IEnergyHandler tileEntity) {
            super(tileEntity, "energy_handler");
        }

        @Callback(doc = "function([direction:number=6]):number --  Returns the amount of stored energy for the given side.")
        public Object[] getEnergyStored(final Context context, final Arguments args) {
            final ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            return new Object[]{tileEntity.getEnergyStored(side)};
        }

        @Callback(doc = "function([direction:number=6]):number --  Returns the maximum amount of stored energy for the given side.")
        public Object[] getMaxEnergyStored(final Context context, final Arguments args) {
            final ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            return new Object[]{tileEntity.getMaxEnergyStored(side)};
        }
    }
}
