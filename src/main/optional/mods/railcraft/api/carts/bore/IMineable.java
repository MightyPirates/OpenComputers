package mods.railcraft.api.carts.bore;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * This interface can be implemented by a block class to control whether a block can be
 * mined by the bore without having to force the user to edit the configuration file.
 *
 * If the block is found to implement this class, any setting in the configuration
 * is ignored for that block.
 *
 * Generally, the reason blocks are not minable by default is to prevent you
 * from intentionally or accidentally boring through your base.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface IMineable
{

    /**
     * Called when the Bore attempts to mine the block. If it returns false,
     * the Bore will halt operation.
     *
     * @param world The World
     * @param x x-Coord
     * @param y y-Coord
     * @param z z-Coord
     * @param bore The Bore entity
     * @param head The BoreHead, item implements IBoreHead.
     * @return true if mineable
     * @see IBoreHead
     */
    public boolean canMineBlock(World world, int x, int y, int z, EntityMinecart bore, ItemStack head);
}
