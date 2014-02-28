/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.inventory;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

@Deprecated
public interface ISpecialInventory extends IInventory {

	/**
	 * Offers an ItemStack for addition to the inventory.
	 * 
	 * @param stack
	 *            ItemStack offered for addition. Do not manipulate this!
	 * @param doAdd
	 *            If false no actual addition should take place. Implementors should simulate.
	 * @param from
	 *            Orientation the ItemStack is offered from.
	 * @return Amount of items used from the passed stack.
	 */
	int addItem(ItemStack stack, boolean doAdd, ForgeDirection from);

	/**
	 * Requests items to be extracted from the inventory
	 * 
	 * @param doRemove
	 *            If false no actual extraction may occur. Implementors should simulate.
	 *            Can be used to "peek" at what the result would be 
	 * @param from
	 *            Orientation the ItemStack is requested from.
	 * @param maxItemCount
	 *            Maximum amount of items to extract (spread over all returned item stacks)
	 * @return Array of item stacks that were/would be extracted from the inventory 
	 */
	ItemStack[] extractItem(boolean doRemove, ForgeDirection from, int maxItemCount);

}
