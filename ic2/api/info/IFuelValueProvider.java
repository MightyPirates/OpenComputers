package ic2.api.info;

import net.minecraft.item.ItemStack;

public interface IFuelValueProvider {
	/**
	 * Determine the fuel value for a single item in the supplied stack.
	 * The information currently applies to Generators and the Iron Furnace.
	 * 
	 * @param itemStack ItemStack containing the item to evaluate.
	 * @param allowLava Determine if lava has a fuel value, currently only true for the Iron Furnace.
	 * @return fuel value
	 */
	int getFuelValue(ItemStack itemStack, boolean allowLava);
}
