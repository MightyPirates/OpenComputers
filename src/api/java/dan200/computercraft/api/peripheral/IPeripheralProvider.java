/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2014. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.peripheral;

import net.minecraft.world.World;

/**
 * This interface is used to create peripheral implementations for blocks
 * @see dan200.computercraft.api.ComputerCraftAPI#registerPeripheralProvider(IPeripheralProvider)
 */
public interface IPeripheralProvider
{
    /**
     * Produce an peripheral implementation from a block location.
     * @see dan200.computercraft.api.ComputerCraftAPI#registerPeripheralProvider(IPeripheralProvider)
     * @return a peripheral, or null if there is not a peripheral here you'd like to handle.
     */
	public IPeripheral getPeripheral( World world, int x, int y, int z, int side );
}
