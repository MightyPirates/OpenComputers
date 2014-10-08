package li.cil.oc.integration.vanilla;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
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
    public int priority() {
        return 0;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityMobSpawner) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityMobSpawner> {
        public Environment(final TileEntityMobSpawner tileEntity) {
            super(tileEntity, "mob_spawner");
        }

        @Callback(doc = "function():string -- Get the name of the entity that is being spawned by this spawner.")
        public Object[] getSpawningMobName(final Context context, final Arguments args) {
            return new Object[]{tileEntity.func_145881_a().getEntityNameToSpawn()};
        }
    }
}
