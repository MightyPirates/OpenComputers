package li.cil.occ.mods.vanilla;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.world.World;

public final class DriverFurnace extends DriverTileEntity implements NamedBlock {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityFurnace.class;
    }

    @Override
    public String preferredName() {
        return "furnace";
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityFurnace) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityFurnace> {
        public Environment(final TileEntityFurnace tileEntity) {
            super(tileEntity, "furnace");
        }

        @Callback(doc = "function():number -- The number of ticks that the furnace will keep burning from the last consumed fuel.")
        public Object[] getBurnTime(final Context context, final Arguments args) {
            return new Object[]{tileEntity.furnaceBurnTime};
        }

        @Callback(doc = "function():number -- The number of ticks that the current item has been cooking for.")
        public Object[] getCookTime(final Context context, final Arguments args) {
            return new Object[]{tileEntity.furnaceCookTime};
        }

        @Callback(doc = "function():number -- The number of ticks that the currently burning fuel lasts in total.")
        public Object[] getCurrentItemBurnTime(final Context context, final Arguments args) {
            return new Object[]{tileEntity.currentItemBurnTime};
        }

        @Callback(doc = "function():boolean -- Get whether the furnace is currently active.")
        public Object[] isBurning(final Context context, final Arguments args) {
            return new Object[]{tileEntity.isBurning()};
        }
    }
}
