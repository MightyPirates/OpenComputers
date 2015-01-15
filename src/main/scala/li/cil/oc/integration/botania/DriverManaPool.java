package li.cil.oc.integration.botania;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaPool;

public class DriverManaPool extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IManaPool.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
        return new Environment((IManaPool) world.getTileEntity(x, y, z));
    }

    public static class Environment extends ManagedTileEntityEnvironment<IManaPool> implements NamedBlock {
        public Environment(IManaPool tileEntity) {
            super(tileEntity, "mana_pool");
        }

        @Override
        public String preferredName() {
            return "mana_pool";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():boolean -- Get whether the mana pool is currently outputting power.")
        public Object[] isOutputtingPower(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.isOutputtingPower()};
        }
    }
}
