package ic2.api.recipe;

import net.minecraft.item.ItemStack;

/**
 * Recipe manager interface for crafting recipes.
 * 
 * @author Richard
 */
public interface ICraftingRecipeManager {
	/**
	 * Adds a shaped crafting recipe.
	 * 
	 * @param output Recipe output
	 * @param input Recipe input format
	 */
	public void addRecipe(ItemStack output, Object... input);
	
	/**
	 * Adds a shapeless crafting recipe.
	 * 
	 * @param output Recipe output
	 * @param input Recipe input
	 */
	public void addShapelessRecipe(ItemStack output, Object... input);
}
