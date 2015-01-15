package li.cil.oc.integration.botania;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import vazkii.botania.api.mana.IManaBlock;

public class DriverManaBlock extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IManaBlock.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
        return new Environment((IManaBlock) world.getTileEntity(x, y, z));
    }

    public static class Environment extends ManagedTileEntityEnvironment<IManaBlock> implements NamedBlock {
        public Environment(IManaBlock tileEntity) {
            super(tileEntity, "mana_block");
        }

        @Override
        public String preferredName() {
            return "mana_block";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the amount of mana this mana pool contains.")
        public Object[] getCurrentMana(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.getCurrentMana()};
        }
    }
}
