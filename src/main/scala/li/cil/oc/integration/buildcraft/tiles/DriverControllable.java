package li.cil.oc.integration.buildcraft.tiles;

import buildcraft.api.tiles.IControllable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverControllable extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IControllable.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IControllable) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IControllable> {
        public Environment(final IControllable tileEntity) {
            super(tileEntity, "bc_controllable");
        }

        @Callback(doc = "function():string -- Get the current control mode.")
        public Object[] getControlMode(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getControlMode().name()};
        }

        @Callback(doc = "function(mode:string):boolean -- Test whether the specified control mode is acceptable.")
        public Object[] acceptsControlMode(final Context context, final Arguments args) {
            return new Object[]{tileEntity.acceptsControlMode(IControllable.Mode.valueOf(args.checkString(0)))};
        }

        @Callback(doc = "function(mode:string):boolean -- Sets the control mode to the specified value.")
        public Object[] setControlMode(final Context context, final Arguments args) {
            tileEntity.setControlMode(IControllable.Mode.valueOf(args.checkString(0)));
            return null;
        }
    }
}
