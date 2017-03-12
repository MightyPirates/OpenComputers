package li.cil.oc.api.prefab.driver;

import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.network.NodeContainerItem;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

/**
 * If you wish to create a block component for a third-party block, i.e. a block
 * for which you do not control the tile entity, such as vanilla blocks, you
 * will need a block driver.
 * <p/>
 * This prefab allows creating a driver that works for a specified list of item
 * stacks (to support different blocks with the same id but different metadata
 * values).
 * <p/>
 * You still have to provide the implementation for creating its environment, if
 * any.
 * <p/>
 * To limit sidedness, I recommend overriding {@link #worksWith(World, BlockPos, EnumFacing)}
 * and calling <code>super.worksWith</code> in addition to the side check.
 *
 * @see NodeContainerItem
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class AbstractDriverBlock implements DriverBlock {
    protected final ItemStack[] blocks;

    protected AbstractDriverBlock(final ItemStack... blocks) {
        this.blocks = blocks.clone();
    }

    @Override
    public boolean worksWith(final World world, final BlockPos pos, final EnumFacing side) {
        final IBlockState state = world.getBlockState(pos);
        final Block block = state.getBlock();
        return worksWith(block, block.getMetaFromState(state));
    }

    protected boolean worksWith(final Block referenceBlock, final int referenceMetadata) {
        for (ItemStack stack : blocks) {
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                final ItemBlock item = (ItemBlock) stack.getItem();
                final Block supportedBlock = item.getBlock();
                final int supportedMetadata = item.getMetadata(stack.getItemDamage());
                if (referenceBlock == supportedBlock && (referenceMetadata == supportedMetadata || stack.getItemDamage() == OreDictionary.WILDCARD_VALUE)) {
                    return true;
                }
            }
        }
        return false;
    }
}
