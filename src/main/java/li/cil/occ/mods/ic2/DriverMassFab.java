package li.cil.occ.mods.ic2;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.api.prefab.ManagedEnvironment;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import li.cil.occ.util.Reflection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class DriverMassFab extends DriverTileEntity implements NamedBlock {
    private static final Class<?> TileController = Reflection.getClass("ic2.core.block.machine.tileentity.TileEntityMatter");

    @Override
    public Class<?> getTileEntityClass() {
        return TileController;
    }

    @Override
    public String preferredName() {
        return "mass_fab";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment(world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntity> {
        public Environment(final TileEntity tileEntity) {
            super(tileEntity, "mass_fab");
        }

        @Callback
        public Object[] getProgress(final Context context, final Arguments args) {
            double energy = (Double) Reflection.get(tileEntity, "energy");
            return new Object[]{Math.min(energy / 100000, 100)};
        }

    }
}
