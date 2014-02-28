/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.transport;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

public interface IPipeTile {

	public enum PipeType {

		ITEM, FLUID, POWER, STRUCTURE;
	}

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

	/**
	 * True if the pipe is connected to the block/pipe in the specific direction
	 *
	 * @param wire
	 * @return true if connect
	 */
	boolean isPipeConnected(ForgeDirection with);

	/**
	 * True if the pipe has a powered wire of the specified color.
	 *
	 * @param wire
	 * @return true if powered
	 */
	boolean isWireActive(PipeWire wire);
}
