package li.cil.oc.integration.vanilla;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraft.world.World;

public final class DriverComparator extends DriverTileEntity implements NamedBlock {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityComparator.class;
    }

    @Override
    public String preferredName() {
        return "comparator";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityComparator) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityComparator> {
        public Environment(final TileEntityComparator tileEntity) {
            super(tileEntity, "comparator");
        }

        @Callback(doc = "function():number -- Get the strength of the comparators output signal.")
        public Object[] getOutputSignal(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOutputSignal()};
        }
    }
}
