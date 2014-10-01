package li.cil.occ.mods.vanilla;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.world.World;

public final class DriverBrewingStand extends DriverTileEntity implements NamedBlock {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityBrewingStand.class;
    }

    @Override
    public String preferredName() {
        return "brewing_stand";
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityBrewingStand) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityBrewingStand> {
        public Environment(final TileEntityBrewingStand tileEntity) {
            super(tileEntity, "brewing_stand");
        }

        @Callback(doc = "function():number -- Get the number of ticks remaining of the current brewing operation.")
        public Object[] getBrewTime(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getBrewTime()};
        }
    }
}
