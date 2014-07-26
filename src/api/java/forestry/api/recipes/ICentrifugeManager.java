/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.recipes;

import java.util.HashMap;

import net.minecraft.item.ItemStack;

/**
 * Provides an interface to the recipe manager of the centrifuge.
 * 
 * The manager is initialized at the beginning of Forestry's BaseMod.load() cycle. Begin adding recipes in BaseMod.ModsLoaded() and this shouldn't be null even
 * if your mod loads before Forestry.
 * 
 * Accessible via {@link RecipeManagers}
 * 
 * @author SirSengir
 */
public interface ICentrifugeManager extends ICraftingProvider {

	/**
	 * Add a recipe to the centrifuge
	 * 
	 * @param timePerItem
	 *            Time to centrifugate one item of the given type
	 * @param resource
	 *            ItemStack containing information on item id and damage. Stack size will be ignored.
	 * @param products
	 *            HashMap<ItemStack, Integer> specifying the possible products and the chances of them resulting from centrifugation.
	 */
	public void addRecipe(int timePerItem, ItemStack resource, HashMap<ItemStack, Integer> products);

	/**
	 * Add a recipe to the centrifuge
	 * 
	 * @param timePerItem
	 *            Time to centrifugate one item of the given type
	 * @param resource
	 *            ItemStack containing information on item id and damage. Stack size will be ignored.
	 * @param produce
	 *            Array of ItemStacks that can be the result of this recipe.
	 * @param chances
	 *            Array of integers corresponding and matching to produce providing the chance (0-100) for the ItemStack at the given index to be
	 *            produced.
	 */
	public void addRecipe(int timePerItem, ItemStack resource, ItemStack[] produce, int[] chances);

	/**
	 * Add a recipe to the centrifuge
	 * 
	 * @param timePerItem
	 *            Time to centrifugate one item of the given type
	 * @param resource
	 *            ItemStack containing information on item id and damage. Stack size will be ignored.
	 * @param primary
	 *            Primary product produced by centrifugating one item. Yield 100 %.
	 * @param secondary
	 *            Secondary product that may be produced when centrifugating the given item. May be null.
	 * @param chance
	 *            Chance (1 - 100) for centrifugation to yield the secondary product.
	 */
	public void addRecipe(int timePerItem, ItemStack resource, ItemStack primary, ItemStack secondary, int chance);

	/**
	 * Add a recipe to the centrifuge
	 * 
	 * @param timePerItem
	 *            Time to centrifugate one item of the given type
	 * @param resource
	 *            ItemStack containing information on item id and damage. Stack size will be ignored.
	 * @param primary
	 *            Primary product produced by centrifugating one item. Yield 100 %.
	 */
	public void addRecipe(int timePerItem, ItemStack resource, ItemStack primary);

}
