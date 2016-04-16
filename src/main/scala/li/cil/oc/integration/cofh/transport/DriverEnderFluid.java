package li.cil.oc.integration.cofh.transport;

import cofh.api.transport.IEnderFluidHandler;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverEnderFluid extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnderFluidHandler.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IEnderFluidHandler) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IEnderFluidHandler> {
        public Environment(final IEnderFluidHandler tileEntity) {
            super(tileEntity, "ender_fluid");
        }

        @Callback(doc = "function():boolean --  Returns whether the tile entity can receive fluids.")
        public Object[] canReceiveFluid(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canReceiveFluid()};
        }

        @Callback(doc = "function():boolean --  Returns whether the tile entity can send fluids.")
        public Object[] canSendFluid(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canSendFluid()};
        }

        @Callback(doc = "function():number --  Returns the frequency.")
        public Object[] getFrequency(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getFrequency()};
        }

        @Callback(doc = "function():string --  Returns the name of the channel.")
        public Object[] getChannelString(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getChannelString()};
        }

        @Callback(doc = "function(frequency:number):boolean --  Sets the frequency to the given value. Returns whether the frequency change was successful")
        public Object[] setFrequency(final Context context, final Arguments args) {
            return new Object[]{tileEntity.setFrequency(args.checkInteger(0))};
        }
    }
}
