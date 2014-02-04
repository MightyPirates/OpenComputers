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
    protected ItemStack[] blocks;

    protected DriverBlock(ItemStack... blocks) {
        this.blocks = blocks.clone();

        // Make the driver known with OpenComputers. This is required, otherwise
        // the mod won't know this driver exists. It must be called in the init
        // phase.
        Driver.add(this);
    }

    @Override
    public boolean worksWith(World world, int x, int y, int z) {
        return worksWith(world.getBlockId(x, y, z), world.getBlockMetadata(x, y, z));
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ItemBlock) {
            ItemBlock reference = (ItemBlock) stack.getItem();
            return worksWith(reference.getBlockID(), reference.getMetadata(stack.getItemDamage()));
        }
        return false;
    }

    protected boolean worksWith(int referenceId, int referenceMetadata) {
        for (ItemStack supportedBlock : blocks) {
            if (supportedBlock != null && supportedBlock.getItem() instanceof ItemBlock) {
                ItemBlock supportedItemBlock = (ItemBlock) supportedBlock.getItem();
                int supportedId = supportedItemBlock.getBlockID();
                int supportedMetadata = supportedItemBlock.getMetadata(supportedBlock.getItemDamage());
                if (referenceId == supportedId && (referenceMetadata == supportedMetadata || supportedBlock.getItemDamage() == OreDictionary.WILDCARD_VALUE)) {
                    return true;
                }
            }
        }
        return false;
    }
}
