package li.cil.oc.integration.buildcraft;

import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerHandler;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverPowerReceptor extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IPowerReceptor.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IPowerReceptor) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IPowerReceptor> {
        public Environment(final IPowerReceptor tileEntity) {
            super(tileEntity, "power_receptor");
        }

        @Callback(doc = "function([direction:number=6]):number --  Returns the activation energy required on the given side of the block.")
        public Object[] getActivationEnergy(final Context context, final Arguments args) {
            final ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            final PowerHandler.PowerReceiver powerReceiver = tileEntity.getPowerReceiver(side);
            return new Object[]{powerReceiver.getActivationEnergy()};
        }

        @Callback(doc = "function([direction:number=6]):number --  Returns the energy stored for the given side of the block.")
        public Object[] getEnergyStored(final Context context, final Arguments args) {
            final ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            final PowerHandler.PowerReceiver powerReceiver = tileEntity.getPowerReceiver(side);
            return new Object[]{powerReceiver.getEnergyStored()};
        }

        @Callback(doc = "function([direction:number=6]):number --  Returns the maximum received energy for the given side of the block.")
        public Object[] getMaxEnergyReceived(final Context context, final Arguments args) {
            final ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            final PowerHandler.PowerReceiver powerReceiver = tileEntity.getPowerReceiver(side);
            return new Object[]{powerReceiver.getMaxEnergyReceived()};
        }

        @Callback(doc = "function([direction:number=6]):number --  Returns the maximum stored energy for the given side of the block.")
        public Object[] getMaxEnergyStored(final Context context, final Arguments args) {
            final ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            final PowerHandler.PowerReceiver powerReceiver = tileEntity.getPowerReceiver(side);
            return new Object[]{powerReceiver.getMaxEnergyStored()};
        }

        @Callback(doc = "function([direction:number=6]):number --  Returns the minimum received energy for the given side of the block.")
        public Object[] getMinEnergyReceived(final Context context, final Arguments args) {
            final ForgeDirection side = args.count() > 0 ? ForgeDirection.getOrientation(args.checkInteger(0)) : ForgeDirection.UNKNOWN;
            final PowerHandler.PowerReceiver powerReceiver = tileEntity.getPowerReceiver(side);
            return new Object[]{powerReceiver.getMinEnergyReceived()};
        }
    }
}
