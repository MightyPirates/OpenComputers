package li.cil.occ.mods.ic2;


import cpw.mods.fml.relauncher.ReflectionHelper;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.api.prefab.ManagedEnvironment;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import li.cil.occ.util.Reflection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class DriverMassFab extends DriverTileEntity {
    private static final Class<?> TileController = Reflection.getClass("ic2.core.block.machine.tileentity.TileEntityMatter");

    @Override
    public Class<?> getTileEntityClass() {
        return TileController;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment(world.getBlockTileEntity(x, y, z));
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
