/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.recipes;

import java.util.Map;

public interface ICraftingProvider {
	/**
	 * Access to the full list of recipes contained in the crafting provider.
	 * 
	 * @return List of the given format where the first array represents inputs and the second outputs. Objects can be either ItemStack or LiquidStack.
	 */
	public Map<Object[], Object[]> getRecipes();
}
