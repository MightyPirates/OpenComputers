package li.cil.oc.integration.botania;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaReceiver;

public class DriverManaReceiver extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IManaReceiver.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
        return new Environment((IManaReceiver) world.getTileEntity(x, y, z));
    }

    public static class Environment extends ManagedTileEntityEnvironment<IManaReceiver> implements NamedBlock {
        public Environment(IManaReceiver tileEntity) {
            super(tileEntity, "mana_receiver");
        }

        @Override
        public String preferredName() {
            return "mana_receiver";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():boolean -- Get whether the mana pool is currently full.")
        public Object[] isFull(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.isFull()};
        }

        @Callback(doc = "function():boolean -- Get whether the mana pool can currently receive mana from bursts.")
        public Object[] canReceiveManaFromBursts(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.canRecieveManaFromBursts()};
        }
    }
}
