package li.cil.oc.integration.botania;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import vazkii.botania.common.block.tile.mana.TileSpreader;

public class DriverManaSpreader extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileSpreader.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
        return new Environment((TileSpreader) world.getTileEntity(x, y, z));
    }

    public static class Environment extends ManagedTileEntityEnvironment<TileSpreader> implements NamedBlock {
        public Environment(TileSpreader tileEntity) {
            super(tileEntity, "mana_spreader");
        }

        @Override
        public String preferredName() {
            return "mana_spreader";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():boolean -- Get whether this spreader is a redstone mana spreader.")
        public Object[] isRedstone(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.isRedstone()};
        }

        @Callback(doc = "function():boolean -- Get whether this spreader is an elven mana spreader.")
        public Object[] isElven(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.isDreamwood() && !tileEntity.isULTRA_SPREADER()};
        }

        @Callback(doc = "function():boolean -- Get whether this spreader is a gaia mana spreader.")
        public Object[] isGaia(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.isULTRA_SPREADER()};
        }
    }
}
