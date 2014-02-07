package li.cil.oc.driver.thermalexpansion;

import cofh.api.transport.IEnderAttuned;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
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

        @Callback
        public Object[] canReceiveEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canReceiveEnergy()};
        }

        @Callback
        public Object[] canReceiveFluid(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canReceiveFluid()};
        }

        @Callback
        public Object[] canReceiveItems(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canReceiveItems()};
        }

        @Callback
        public Object[] canSendEnergy(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canSendEnergy()};
        }

        @Callback
        public Object[] canSendFluid(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canSendFluid()};
        }

        @Callback
        public Object[] canSendItems(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canSendItems()};
        }

        @Callback
        public Object[] getFrequency(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getFrequency()};
        }

        @Callback
        public Object[] getOwnerString(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOwnerString()};
        }

        @Callback
        public Object[] setFrequency(final Context context, final Arguments args) {
            return new Object[]{tileEntity.setFrequency(args.checkInteger(0))};
        }
    }
}
