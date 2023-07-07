package li.cil.oc.integration.cofh.tileentity;

import cofh.api.tileentity.IRedstoneControl;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DriverRedstoneControl extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IRedstoneControl.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos, final EnumFacing side) {
        return new Environment((IRedstoneControl) world.getTileEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IRedstoneControl> {
        public Environment(final IRedstoneControl tileEntity) {
            super(tileEntity, "redstone_control");
        }

        @Callback(doc = "function():boolean --  Returns whether the control is disabled.")
        public Object[] getControlDisable(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getControl() == IRedstoneControl.ControlMode.DISABLED};
        }

        @Callback(doc = "function():int --  Returns the control status.")
        public Object[] getControlSetting(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getControl().ordinal()};

        }

        @Callback(doc = "function():string --  Returns the control status.")
        public Object[] getControlSettingName(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getControl().name()};
        }

        @Callback(doc = "function(int):string --  Returns the name of the given control")
        public Object[] getControlName(final Context context, final Arguments args) {
            IRedstoneControl.ControlMode m = IRedstoneControl.ControlMode.values()[args.checkInteger(0)];
            return new Object[]{m.name()};
        }

        @Callback(doc = "function():boolean --  Returns whether the component is powered.")
        public Object[] isPowered(final Context context, final Arguments args) {
            return new Object[]{tileEntity.isPowered()};
        }

        @Callback(doc = "function():boolean --  Sets whether the control tp disabled.")
        public Object[] setControlDisable(final Context context, final Arguments args) {
            tileEntity.setControl(IRedstoneControl.ControlMode.DISABLED);
            return new Object[]{true};
        }

        @Callback(doc = "function(state:int):boolean --  Sets the control status to the given value.")
        public Object[] setControlSetting(final Context context, final Arguments args) {
            if (args.isInteger(0)) {
                tileEntity.setControl(IRedstoneControl.ControlMode.values()[args.checkInteger(0)]);
                return new Object[]{true};
            } else {
                tileEntity.setControl(IRedstoneControl.ControlMode.valueOf(args.checkString(0)));
                return new Object[]{true};
            }

        }
    }
}
