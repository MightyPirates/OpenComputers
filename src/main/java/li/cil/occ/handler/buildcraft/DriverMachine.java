package li.cil.occ.handler.buildcraft;


import buildcraft.core.IMachine;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.handler.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverMachine extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IMachine.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
        return new Environment((IMachine) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IMachine> {
        public Environment(final IMachine tileEntity) {
            super(tileEntity, "machine");
        }

        @Callback
        public Object[] isActive(final Context context, final Arguments args) {
            return new Object[]{tileEntity.isActive()};
        }

        @Callback
        public Object[] manageFluids(final Context context, final Arguments args) {
            return new Object[]{tileEntity.manageFluids()};
        }

        @Callback
        public Object[] manageSolids(final Context context, final Arguments args) {
            return new Object[]{tileEntity.manageSolids()};
        }


    }
}
