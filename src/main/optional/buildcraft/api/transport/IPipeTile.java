/**
 * Copyright (c) SpaceToad, 2011 http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License
 * 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.transport;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;

public interface IPipeTile extends ISolidSideTile, IFluidHandler {

	public enum PipeType {

		ITEM, FLUID, POWER, STRUCTURE;
	}

	@Deprecated
	IPipe getPipe();

	PipeType getPipeType();

	/**
	 * Offers an ItemStack for addition to the pipe. Will be rejected if the
	 * pipe doesn't accept items from that side.
	 *
	 * @param stack ItemStack offered for addition. Do not manipulate this!
	 * @param doAdd If false no actual addition should take place. Implementors
	 * should simulate.
	 * @param from Orientation the ItemStack is offered from.
	 * @return Amount of items used from the passed stack.
	 */
	int injectItem(ItemStack stack, boolean doAdd, ForgeDirection from);
	
	boolean isPipeConnected(ForgeDirection with);
}
