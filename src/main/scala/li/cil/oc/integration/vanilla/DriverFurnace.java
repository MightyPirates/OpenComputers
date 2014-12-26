package li.cil.oc.integration.vanilla;

import li.cil.oc.api.driver.EnvironmentAware;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public final class DriverFurnace extends DriverTileEntity implements EnvironmentAware {
    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityFurnace.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos) {
        return new Environment((TileEntityFurnace) world.getTileEntity(pos));
    }

    @Override
    public Class<? extends li.cil.oc.api.network.Environment> providedEnvironment(ItemStack stack) {
        if (stack != null && Block.getBlockFromItem(stack.getItem()) == Blocks.furnace)
            return Environment.class;
        return null;
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityFurnace> implements NamedBlock {
        public Environment(final TileEntityFurnace tileEntity) {
            super(tileEntity, "furnace");
        }

        @Override
        public String preferredName() {
            return "furnace";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- The number of ticks that the furnace will keep burning from the last consumed fuel.")
        public Object[] getBurnTime(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getField(0)};
        }

        @Callback(doc = "function():number -- The number of ticks that the current item has been cooking for.")
        public Object[] getCookTime(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getField(2)};
        }

        @Callback(doc = "function():number -- The number of ticks that the currently burning fuel lasts in total.")
        public Object[] getCurrentItemBurnTime(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getField(1)};
        }

        @Callback(doc = "function():boolean -- Get whether the furnace is currently active.")
        public Object[] isBurning(final Context context, final Arguments args) {
            return new Object[]{tileEntity.isBurning()};
        }
    }
}
