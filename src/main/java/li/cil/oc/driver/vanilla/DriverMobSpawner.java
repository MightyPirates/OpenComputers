package li.cil.oc.driver.vanilla;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import li.cil.oc.driver.TileEntityDriver;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;

public final class DriverMobSpawner extends TileEntityDriver {
    @Override
    public Class<?> getFilterClass() {
        return TileEntityMobSpawner.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityMobSpawner) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityMobSpawner> {
        public Environment(final TileEntityMobSpawner tileEntity) {
            super(tileEntity, "mob_spawner");
        }

        @Callback
        public Object[] getSpawningMobName(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getSpawnerLogic().getEntityNameToSpawn()};
        }
    }
}
