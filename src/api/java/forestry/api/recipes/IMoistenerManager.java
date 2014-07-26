/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.recipes;

import net.minecraft.item.ItemStack;

/**
 * Provides an interface to the recipe manager of the moistener.
 * 
 * The manager is initialized at the beginning of Forestry's BaseMod.load() cycle. Begin adding recipes in BaseMod.ModsLoaded() and this shouldn't be null even
 * if your mod loads before Forestry.
 * 
 * Accessible via {@link RecipeManagers}
 * 
 * @author SirSengir
 */
public interface IMoistenerManager extends ICraftingProvider {

	/**
	 * Add a recipe to the moistener
	 * 
	 * @param resource
	 *            Item required in resource stack. Will be reduced by one per produced item.
	 * @param product
	 *            Item to produce per resource processed.
	 * @param timePerItem
	 *            Moistener runs at 1 - 4 time ticks per ingame tick depending on light level. For mycelium this value is currently 5000.
	 */
	public void addRecipe(ItemStack resource, ItemStack product, int timePerItem);
}
