/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.farming;

import java.util.Collection;

import net.minecraft.item.ItemStack;

import net.minecraftforge.common.util.ForgeDirection;

public interface IFarmListener {

	/**
	 * Called before a crop is harvested.
	 * 
	 * @param crop
	 *            ICrop about to be harvested.
	 * @return true to cancel further processing of this crop.
	 */
	boolean beforeCropHarvest(ICrop crop);

	/**
	 * Called after a crop has been harvested, but before harvested items are stowed in the farms inventory.
	 * 
	 * @param harvested
	 *            Collection of harvested stacks. May be manipulated. Ensure removal of stacks with 0 or less items!
	 * @param crop
	 *            Harvested {@link ICrop}
	 */
	void afterCropHarvest(Collection<ItemStack> harvested, ICrop crop);

	/**
	 * Called after the stack of collected items has been returned by the farm logic, but before it is added to the farm's pending queue.
	 * 
	 * @param collected
	 *            Collection of collected stacks. May be manipulated. Ensure removal of stacks with 0 or less items!
	 * @param logic
	 */
	void hasCollected(Collection<ItemStack> collected, IFarmLogic logic);

	/**
	 * Called after farmland has successfully been cultivated by a farm logic.
	 * 
	 * @param logic
	 * @param x
	 * @param y
	 * @param z
	 * @param direction
	 * @param extent
	 */
	void hasCultivated(IFarmLogic logic, int x, int y, int z, ForgeDirection direction, int extent);

	/**
	 * Called after the stack of harvested crops has been returned by the farm logic, but before it is added to the farm's pending queue.
	 * 
	 * @param harvested
	 * @param logic
	 * @param x
	 * @param y
	 * @param z
	 * @param direction
	 * @param extent
	 */
	void hasScheduledHarvest(Collection<ICrop> harvested, IFarmLogic logic, int x, int y, int z, ForgeDirection direction, int extent);

	/**
	 * Can be used to cancel farm task on a per side/{@link IFarmLogic} basis.
	 * 
	 * @param logic
	 * @param direction
	 * @return true to skip any work action on the given logic and direction for this work cycle.
	 */
	boolean cancelTask(IFarmLogic logic, ForgeDirection direction);
}
