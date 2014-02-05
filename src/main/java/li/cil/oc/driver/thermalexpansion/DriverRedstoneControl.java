package li.cil.oc.driver.thermalexpansion;

import cofh.api.tileentity.IRedstoneControl;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import li.cil.oc.driver.TileEntityDriver;
import net.minecraft.world.World;

public final class DriverRedstoneControl extends TileEntityDriver {
    @Override
    public Class<?> getFilterClass() {
        return IRedstoneControl.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IRedstoneControl) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IRedstoneControl> {
        public Environment(final IRedstoneControl tileEntity) {
            super(tileEntity, "redstone_control");
        }

        @Callback
        public Object[] getControlDisable(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getControlDisable()};
        }

        @Callback
        public Object[] getControlSetting(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getControlSetting()};
        }

        @Callback
        public Object[] isPowered(final Context context, final Arguments args) {
            return new Object[]{tileEntity.isPowered()};
        }

        @Callback
        public Object[] setControlDisable(final Context context, final Arguments args) {
            return new Object[]{tileEntity.setControlDisable(args.checkBoolean(0))};
        }

        @Callback
        public Object[] setControlSetting(final Context context, final Arguments args) {
            return new Object[]{tileEntity.setControlSetting(args.checkBoolean(0))};
        }
    }
}
