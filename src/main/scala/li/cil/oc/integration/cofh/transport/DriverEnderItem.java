package li.cil.oc.integration.cofh.transport;

import cofh.api.transport.IEnderItemHandler;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverEnderItem extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnderItemHandler.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IEnderItemHandler) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IEnderItemHandler> {
        public Environment(final IEnderItemHandler tileEntity) {
            super(tileEntity, "ender_item");
        }

        @Callback(doc = "function():boolean --  Returns whether the tile entity can receive items.")
        public Object[] canReceiveItems(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canReceiveItems()};
        }

        @Callback(doc = "function():boolean --  Returns whether the tile entity can send items.")
        public Object[] canSendItems(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canSendItems()};
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
