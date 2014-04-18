package appeng.api.features;

import net.minecraft.item.ItemStack;

/**
 * A Registry of any special comparison handlers for AE To use.
 * 
 */
public interface ISpecialComparisonRegistry
{

	/**
	 * return TheHandler or null.
	 * 
	 * @param stack
	 * @return a handler it found for a specific item
	 */
	IItemComparison getSpecialComparion(ItemStack stack);

	/**
	 * Register a new special comparison function with AE.
	 * 
	 * @param prov
	 */
	public void addComparisonProvider(IItemComparisionProvider prov);

}