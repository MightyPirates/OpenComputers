package li.cil.oc.integration.botania;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import vazkii.botania.common.block.tile.TileAltar;

public class DriverAltar extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return TileAltar.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
        return new Environment((TileAltar) world.getTileEntity(x, y, z));
    }

    public static class Environment extends ManagedTileEntityEnvironment<TileAltar> implements NamedBlock {
        public Environment(TileAltar tileEntity) {
            super(tileEntity, "petal_apothecary");
        }

        @Override
        public String preferredName() {
            return "petal_apothecary";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():boolean -- Returns whether the apothecary contains a liquid.")
        public Object[] hasLiquid(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.hasWater() || tileEntity.hasLava()};
        }

        @Callback(doc = "function():boolean -- Returns whether the apothecary is mossy.")
        public Object[] isMossy(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.isMossy};
        }
    }
}
