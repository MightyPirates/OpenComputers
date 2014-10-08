package li.cil.occ.mods.buildcraft;


import buildcraft.core.IMachine;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.world.World;

public final class DriverMachine extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IMachine.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
        return new Environment((IMachine) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IMachine> {
        public Environment(final IMachine tileEntity) {
            super(tileEntity, "machine");
        }

        @Callback(doc = "function():boolean --  Returns whether the machine is active.")
        public Object[] isActive(final Context context, final Arguments args) {
            return new Object[]{tileEntity.isActive()};
        }

        @Callback(doc = "function():boolean --  Returns whether the machine can manage fluids.")
        public Object[] manageFluids(final Context context, final Arguments args) {
            return new Object[]{tileEntity.manageFluids()};
        }

        @Callback(doc = "function():boolean --  Returns whether the machine can manage solids.")
        public Object[] manageSolids(final Context context, final Arguments args) {
            return new Object[]{tileEntity.manageSolids()};
        }


    }
}
