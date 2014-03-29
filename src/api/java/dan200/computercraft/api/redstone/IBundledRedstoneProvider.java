/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.redstone;

import net.minecraft.world.World;

/**
 * This interface is used to provide bundled redstone output for blocks
 * @see dan200.computercraft.api.ComputerCraftAPI#registerBundledRedstoneProvider(IBundledRedstoneProvider)
 */
public interface IBundledRedstoneProvider
{
    /**
     * Produce an bundled redstone output from a block location.
     * @see dan200.computercraft.api.ComputerCraftAPI#registerBundledRedstoneProvider(IBundledRedstoneProvider)
     * @return a number in the range 0-65535 to indicate this block is providing output, or -1 if you do not wish to handle this block
     */
    public int getBundledRedstoneOutput( World world, int x, int y, int z, int side );
}
