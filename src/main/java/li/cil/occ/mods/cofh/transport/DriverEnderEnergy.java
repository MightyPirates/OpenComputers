package li.cil.occ.mods.cofh.transport;

import cofh.api.transport.IEnderEnergyHandler;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverEnderEnergy extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnderEnergyHandler.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IEnderEnergyHandler) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IEnderEnergyHandler> {
        public Environment(final IEnderEnergyHandler tileEntity) {
            super(tileEntity, "ender_energy");
        }

        @Callback(doc = "function():boolean --  Returns whether the tileentity can receive energy.")
        public Object[] canReceiveEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canReceiveEnergy()};
        }

        @Callback(doc = "function():boolean --  Returns whether the tileentity can send energy.")
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
