package li.cil.oc.integration.vanilla;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.world.World;

public final class DriverBrewingStand extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityBrewingStand.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityBrewingStand) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityBrewingStand> implements NamedBlock {
        public Environment(final TileEntityBrewingStand tileEntity) {
            super(tileEntity, "brewing_stand");
        }

        @Override
        public String preferredName() {
            return "brewing_stand";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the number of ticks remaining of the current brewing operation.")
        public Object[] getBrewTime(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getBrewTime()};
        }
    }
}
