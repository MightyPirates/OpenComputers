package li.cil.oc.driver.buildcraft;

import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerHandler;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import li.cil.oc.driver.TileEntityDriver;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

public final class DriverPowerReceptor extends TileEntityDriver {
    @Override
    public Class<?> getFilterClass() {
        return IPowerReceptor.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IPowerReceptor) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IPowerReceptor> {
        public Environment(IPowerReceptor tileEntity) {
            super(tileEntity, "power_receptor");
        }

        @Callback
        public Object[] getActivationEnergy(final Context context, final Arguments args) {
            final ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            final PowerHandler.PowerReceiver powerReceiver = tileEntity.getPowerReceiver(side);
            return new Object[]{powerReceiver.getActivationEnergy()};
        }

        @Callback
        public Object[] getEnergyStored(final Context context, final Arguments args) {
            final ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            final PowerHandler.PowerReceiver powerReceiver = tileEntity.getPowerReceiver(side);
            return new Object[]{powerReceiver.getEnergyStored()};
        }

        @Callback
        public Object[] getMaxEnergyReceived(final Context context, final Arguments args) {
            final ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            final PowerHandler.PowerReceiver powerReceiver = tileEntity.getPowerReceiver(side);
            return new Object[]{powerReceiver.getMaxEnergyReceived()};
        }

        @Callback
        public Object[] getMaxEnergyStored(final Context context, final Arguments args) {
            final ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            final PowerHandler.PowerReceiver powerReceiver = tileEntity.getPowerReceiver(side);
            return new Object[]{powerReceiver.getMaxEnergyStored()};
        }

        @Callback
        public Object[] getMinEnergyReceived(final Context context, final Arguments args) {
            final ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            final PowerHandler.PowerReceiver powerReceiver = tileEntity.getPowerReceiver(side);
            return new Object[]{powerReceiver.getMinEnergyReceived()};
        }
    }
}
