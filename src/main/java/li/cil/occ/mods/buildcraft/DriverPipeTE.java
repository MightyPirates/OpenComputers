package li.cil.occ.mods.buildcraft;

import buildcraft.api.transport.IPipeTile;
import buildcraft.api.transport.PipeWire;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverPipeTE extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IPipeTile.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IPipeTile) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IPipeTile> {
        public Environment(final IPipeTile tileEntity) {
            super(tileEntity, "pipe");
        }


        @Callback(doc = "function(color:string):boolean --  Returns whether the pipe is wired with the given color")
        public Object[] isWired(final Context context, final Arguments args) {
            try {
                return new Object[]{tileEntity.isWireActive(PipeWire.valueOf(args.checkString(0)))};
            } catch (Throwable ignored) {
            }
            return new Object[]{false};
        }
    }
}
