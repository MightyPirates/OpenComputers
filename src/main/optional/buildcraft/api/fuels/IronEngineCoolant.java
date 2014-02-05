/**
 * Copyright (c) SpaceToad, 2011 http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License
 * 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.fuels;

import buildcraft.api.core.StackWrapper;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public final class IronEngineCoolant {

	public static Map<String, Coolant> liquidCoolants = new HashMap<String, Coolant>();
	public static Map<StackWrapper, FluidStack> solidCoolants = new HashMap<StackWrapper, FluidStack>();

	public static FluidStack getFluidCoolant(ItemStack stack) {
		return solidCoolants.get(new StackWrapper(stack));
	}

	public static Coolant getCoolant(ItemStack stack) {
		return getCoolant(getFluidCoolant(stack));
	}

	public static Coolant getCoolant(FluidStack fluidStack) {
		return fluidStack != null && fluidStack.getFluid() != null ? liquidCoolants.get(fluidStack.getFluid().getName()) : null;
	}

	private IronEngineCoolant() {
	}

	public static interface Coolant {

		float getDegreesCoolingPerMB(float currentHeat);
	}

	public static void addCoolant(final Fluid fluid, final float degreesCoolingPerMB) {
		if (fluid != null) {
			liquidCoolants.put(fluid.getName(), new Coolant() {
				@Override
				public float getDegreesCoolingPerMB(float currentHeat) {
					return degreesCoolingPerMB;
				}
			});
		}
	}

	/**
	 * Adds a solid coolant like Ice Blocks. The FluidStack must contain a registered
	 * Coolant Fluid or nothing will happen. You do not need to call this for
	 * Fluid Containers.
	 *
	 * @param stack
	 * @param coolant
	 */
	public static void addCoolant(final ItemStack stack, final FluidStack coolant) {
		if (stack != null && Item.itemsList[stack.itemID] != null && coolant != null) {
			solidCoolants.put(new StackWrapper(stack), coolant);
		}
	}

	/**
	 * Adds a solid coolant like Ice Blocks. The FluidStack must contain a registered
	 * Coolant Fluid or nothing will happen. You do not need to call this for
	 * Fluid Containers.
	 *
	 * @param stack
	 * @param coolant
	 */
	public static void addCoolant(final int itemId, final int metadata, final FluidStack coolant) {
		addCoolant(new ItemStack(itemId, 1, metadata), coolant);
	}

	public static boolean isCoolant(Fluid fluid) {
		return liquidCoolants.containsKey(fluid.getName());
	}
}
