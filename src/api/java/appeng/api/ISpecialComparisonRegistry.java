package appeng.api;

import net.minecraft.item.ItemStack;

/**
 * A Registry of any special comparison handlers for AE To use.
 * 
 */
public interface ISpecialComparisonRegistry {
	
	/**
	 * returns a handler it found for a specific item.
	 * @param stack
	 * @return TheHandler or null
	 */
	IItemComparison getSpecialComparion( ItemStack stack );
	
	/**
	 * Register a new cpecial comparison function with AE.
	 * @param prov
	 */
	public void addComparisonProvider( IItemComparisionProvider prov );
	
}
