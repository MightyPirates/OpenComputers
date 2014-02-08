package li.cil.occ.mods.buildcraft;

import buildcraft.api.transport.IPipe;
import buildcraft.api.transport.IPipeTile;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
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
        return new Environment((IPipeTile) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IPipeTile> {
        public Environment(final IPipeTile tileEntity) {
            super(tileEntity, "pipete");
        }

        @Callback
        public Object[] hasGate(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getPipe().hasGate()};
        }

        @Callback
        public Object[] isWired(final Context context, final Arguments args) {
            try {
                return new Object[]{tileEntity.getPipe().isWired(IPipe.WireColor.valueOf(args.checkString(0)))};
            } catch (Throwable ignored) {
            }
            return new Object[]{false};
        }
    }
}
