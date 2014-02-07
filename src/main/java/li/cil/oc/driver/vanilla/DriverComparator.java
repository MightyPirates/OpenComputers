package li.cil.oc.driver.vanilla;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraft.world.World;

public final class DriverComparator extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityComparator.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityComparator) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityComparator> {
        public Environment(final TileEntityComparator tileEntity) {
            super(tileEntity, "comparator");
        }

        @Callback
        public Object[] getOutputSignal(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOutputSignal()};
        }
    }
}
