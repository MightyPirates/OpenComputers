package li.cil.occ.mods.vanilla;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
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
