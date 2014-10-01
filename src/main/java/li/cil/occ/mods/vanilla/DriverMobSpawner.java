package li.cil.occ.mods.vanilla;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;

public final class DriverMobSpawner extends DriverTileEntity implements NamedBlock {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityMobSpawner.class;
    }

    @Override
    public String preferredName() {
        return "mob_spawner";
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityMobSpawner) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityMobSpawner> {
        public Environment(final TileEntityMobSpawner tileEntity) {
            super(tileEntity, "mob_spawner");
        }

        @Callback(doc = "function():string -- Get the name of the entity that is being spawned by this spawner.")
        public Object[] getSpawningMobName(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getSpawnerLogic().getEntityNameToSpawn()};
        }
    }
}
