package li.cil.oc.api.prefab;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

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
 *
 * @see li.cil.oc.api.network.ManagedEnvironment
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class DriverBlock implements li.cil.oc.api.driver.Block {
    protected final ItemStack[] blocks;

    protected DriverBlock(final ItemStack... blocks) {
        this.blocks = blocks.clone();
    }

    @Override
    public boolean worksWith(final World world, final int x, final int y, final int z) {
        return worksWith(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
    }

    protected boolean worksWith(final Block block, final int referenceMetadata) {
        throw new RuntimeException("not yet implemented");
//        for (ItemStack supportedBlock : blocks) {
//            if (supportedBlock != null && supportedBlock.getItem() instanceof ItemBlock) {
//                final ItemBlock supportedItemBlock = (ItemBlock) supportedBlock.getItem();
//                final int supportedId = supportedItemBlock.getBlockID();
//                final int supportedMetadata = supportedItemBlock.getMetadata(supportedBlock.getItemDamage());
//                if (referenceId == supportedId && (referenceMetadata == supportedMetadata || supportedBlock.getItemDamage() == OreDictionary.WILDCARD_VALUE)) {
//                    return true;
//                }
//            }
//        }
//        return false;
    }
}
