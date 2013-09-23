package ic2.api.info;

import net.minecraft.item.ItemStack;

public interface IEnergyValueProvider {
	/**
	 * Determine the energy value for a single item in the supplied stack.
	 * The value is used by most machines in the discharge slot.
	 * 
	 * @param itemStack ItemStack containing the item to evaluate.
	 * @return energy in EU
	 */
	int getEnergyValue(ItemStack itemStack);
}
