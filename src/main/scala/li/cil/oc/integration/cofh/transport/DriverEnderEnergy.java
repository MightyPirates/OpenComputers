package li.cil.oc.integration.cofh.transport;

import cofh.api.transport.IEnderEnergyHandler;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public final class DriverEnderEnergy extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnderEnergyHandler.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final EnumFacing side) {
        return new Environment((IEnderEnergyHandler) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IEnderEnergyHandler> {
        public Environment(final IEnderEnergyHandler tileEntity) {
            super(tileEntity, "ender_energy");
        }

        @Callback(doc = "function():boolean --  Returns whether the tile entity can receive energy.")
        public Object[] canReceiveEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canReceiveEnergy()};
        }

        @Callback(doc = "function():boolean --  Returns whether the tile entity can send energy.")
        public Object[] canSendEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canSendEnergy()};
        }

        @Callback(doc = "function():number --  Returns the frequency.")
        public Object[] getFrequency(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getFrequency()};
        }

        @Callback(doc = "function(frequency:number):boolean --  Sets the frequency to the given value. Returns whether the frequency change was successful")
        public Object[] setFrequency(final Context context, final Arguments args) {
            return new Object[]{tileEntity.setFrequency(args.checkInteger(0))};
        }

        @Callback(doc = "function():string --  Returns the name of the channel.")
        public Object[] getChannelString(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getChannelString()};
        }
    }
}
