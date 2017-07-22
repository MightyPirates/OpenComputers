package li.cil.oc.integration.buildcraft.transport;

import buildcraft.api.transport.IPipeTile;
import buildcraft.api.transport.PipeWire;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class DriverPipeTile extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IPipeTile.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IPipeTile) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IPipeTile> implements NamedBlock {
        public Environment(final IPipeTile tileEntity) {
            super(tileEntity, "bc_pipe");
        }

        @Override
        public String preferredName() {
            return "bc_pipe";
        }

        @Override
        public int priority() {
            return -10;
        }

        @Callback(doc = "function():string --  Returns the type of the pipe.")
        public Object[] getPipeType(final Context context, final Arguments args) {
            try {
                return new Object[]{tileEntity.getPipeType().name()};
            } catch (Throwable ignored) {
            }
            return new Object[]{null, "none"};
        }

        @Callback(doc = "function(side:number):boolean --  Returns whether the pipe is connected to something on the specified side.")
        public Object[] isPipeConnected(final Context context, final Arguments args) {
            try {
                return new Object[]{tileEntity.isPipeConnected(ForgeDirection.getOrientation(args.checkInteger(0)))};
            } catch (Throwable ignored) {
            }
            return new Object[]{false};
        }

        @Callback(doc = "function(color:string):boolean -- Returns whether the pipe is wired with the given color.")
        public Object[] isWired(final Context context, final Arguments args) {
            try {
                return new Object[]{tileEntity.getPipe().isWired(PipeWire.valueOf(args.checkString(0)))};
            } catch (Throwable ignored) {
            }
            return new Object[]{false};
        }

        @Callback(doc = "function(color:string):boolean -- Returns whether the wired with the given color is active.")
        public Object[] isWireActive(final Context context, final Arguments args) {
            try {
                return new Object[]{tileEntity.getPipe().isWireActive(PipeWire.valueOf(args.checkString(0)))};
            } catch (Throwable ignored) {
            }
            return new Object[]{false};
        }

        @Callback(doc = "function(side:number):boolean -- Returns whether the pipe has a gate on the specified side.")
        public Object[] hasGate(final Context context, final Arguments args) {
            try {
                return new Object[]{tileEntity.getPipe().hasGate(ForgeDirection.getOrientation(args.checkInteger(0)))};
            } catch (Throwable ignored) {
            }
            return new Object[]{false};
        }
    }
}
