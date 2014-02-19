package li.cil.occ.mods.thermalexpansion;

import cofh.api.tileentity.IRedstoneControl;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverRedstoneControl extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
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

        @Callback(doc = "function():boolean --  Returns whether the control is disabled.")
        public Object[] getControlDisable(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getControlDisable()};
        }

        @Callback(doc = "function():boolean --  Returns whether the control setting is enabled.")
        public Object[] getControlSetting(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getControlSetting()};
        }

        @Callback(doc = "function():boolean --  Returns whether the component is powered.")
        public Object[] isPowered(final Context context, final Arguments args) {
            return new Object[]{tileEntity.isPowered()};
        }

        @Callback(doc = "function(disable:boolean):boolean --  Sets whether the control is disabled.")
        public Object[] setControlDisable(final Context context, final Arguments args) {
            return new Object[]{tileEntity.setControlDisable(args.checkBoolean(0))};
        }

        @Callback(doc = "function(state:boolean):boolean --  Sets the control status to the given value.")
        public Object[] setControlSetting(final Context context, final Arguments args) {
            return new Object[]{tileEntity.setControlSetting(args.checkBoolean(0))};
        }
    }
}
