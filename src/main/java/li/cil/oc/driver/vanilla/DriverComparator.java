package li.cil.oc.driver.vanilla;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverBlock;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public final class DriverComparator extends DriverBlock {
    DriverComparator() {
        super(new ItemStack(Item.comparator),
                new ItemStack(Block.redstoneComparatorIdle, 1, OreDictionary.WILDCARD_VALUE),
                new ItemStack(Block.redstoneComparatorActive, 1, OreDictionary.WILDCARD_VALUE));
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((TileEntityComparator) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntityComparator> {
        public Environment(final TileEntityComparator tileEntity) {
            super(tileEntity, "comparator");
        }

        @Callback
        public Object[] getOutputSignal(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOutputSignal()};
        }
    }
}
