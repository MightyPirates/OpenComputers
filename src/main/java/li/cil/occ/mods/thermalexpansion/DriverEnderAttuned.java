package li.cil.occ.mods.thermalexpansion;

import cofh.api.transport.IEnderAttuned;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverEnderAttuned extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IEnderAttuned.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IEnderAttuned) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IEnderAttuned> {
        public Environment(final IEnderAttuned tileEntity) {
            super(tileEntity, "ender_attuned");
        }

        @Callback(doc = "function():boolean --  Returns whether the tileentity can receive energy.")
        public Object[] canReceiveEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canReceiveEnergy()};
        }

        @Callback(doc = "function():boolean --  Returns whether the tileentity can receive fluids.")
        public Object[] canReceiveFluid(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canReceiveFluid()};
        }

        @Callback(doc = "function():boolean --  Returns whether the tileentity can receive items.")
        public Object[] canReceiveItems(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canReceiveItems()};
        }

        @Callback(doc = "function():boolean --  Returns whether the tileentity can send energy.")
        public Object[] canSendEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canSendEnergy()};
        }

        @Callback(doc = "function():boolean --  Returns whether the tileentity can send fluids.")
        public Object[] canSendFluid(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canSendFluid()};
        }

        @Callback(doc = "function():boolean --  Returns whether the tileentity can send items.")
        public Object[] canSendItems(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canSendItems()};
        }

        @Callback(doc = "function():number --  Returns the frequency.")
        public Object[] getFrequency(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getFrequency()};
        }

        @Callback(doc = "function():string --  Returns the name of the owner.")
        public Object[] getOwnerString(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOwnerString()};
        }

        @Callback(doc = "function(frequency:number):boolean --  Sets the frequency to the given value. Returns whether the frequency change was successful")
        public Object[] setFrequency(final Context context, final Arguments args) {
            return new Object[]{tileEntity.setFrequency(args.checkInteger(0))};
        }
    }
}
