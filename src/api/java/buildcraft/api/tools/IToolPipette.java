/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.tools;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public interface IToolPipette {

	/**
	 * @param pipette
	 *            ItemStack of the pipette.
	 * @return Capacity of the pipette.
	 */
	int getCapacity(ItemStack pipette);

	/**
	 * @param pipette
	 * @return true if the pipette can pipette.
	 */
	boolean canPipette(ItemStack pipette);

	/**
	 * Fills the pipette with the given liquid stack.
	 *
	 * @param pipette
	 * @param liquid
	 * @param doFill
	 * @return Amount of liquid used in filling the pipette.
	 */
	int fill(ItemStack pipette, FluidStack liquid, boolean doFill);

	/**
	 * Drains liquid from the pipette
	 *
	 * @param pipette
	 * @param maxDrain
	 * @param doDrain
	 * @return Fluid stack representing the liquid and amount drained from the pipette.
	 */
	FluidStack drain(ItemStack pipette, int maxDrain, boolean doDrain);
}
