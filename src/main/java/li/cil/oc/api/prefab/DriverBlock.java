package li.cil.oc.api.prefab;

import li.cil.oc.api.Driver;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

/**
 * If you wish to create a block component for a third-party block, i.e. a block
 * for which you do not control the tile entity, such as vanially blocks, you
 * will need a block driver.
 * <p/>
 * This prefab allows creating a driver that works for a specified list of item
 * stacks (to support different blocks with the same id but different metadata
 * values).
 * <p/>
 * Note that if you use this prefab you <em>must instantiate your driver in the
 * init phase</em>, since it automatically registers itself with OpenComputers.
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

        // Make the driver known with OpenComputers. This is required, otherwise
        // the mod won't know this driver exists. It must be called in the init
        // phase.
        Driver.add(this);
    }

    @Override
    public boolean worksWith(final World world, final int x, final int y, final int z) {
        return worksWith(world.getBlockId(x, y, z), world.getBlockMetadata(x, y, z));
    }

    @Override
    public boolean worksWith(final World world, final ItemStack stack) {
        if (stack != null) {
            for (ItemStack supportedBlock : blocks) {
                if (stack.isItemEqual(supportedBlock)) {
                    return true;
                }
            }
            if (stack.getItem() instanceof ItemBlock) {
                final ItemBlock reference = (ItemBlock) stack.getItem();
                return worksWith(reference.getBlockID(), reference.getMetadata(stack.getItemDamage()));
            }
        }
        return false;
    }

    protected boolean worksWith(final int referenceId, final int referenceMetadata) {
        for (ItemStack supportedBlock : blocks) {
            if (supportedBlock != null && supportedBlock.getItem() instanceof ItemBlock) {
                final ItemBlock supportedItemBlock = (ItemBlock) supportedBlock.getItem();
                final int supportedId = supportedItemBlock.getBlockID();
                final int supportedMetadata = supportedItemBlock.getMetadata(supportedBlock.getItemDamage());
                if (referenceId == supportedId && (referenceMetadata == supportedMetadata || supportedBlock.getItemDamage() == OreDictionary.WILDCARD_VALUE)) {
                    return true;
                }
            }
        }
        return false;
    }
}
