package appeng.api;

import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * Lets you manipulate Grinder Recipes, by adding or editing existing ones.
 */
public interface IGrinderRecipeManager {
	
	/**
	 * Current list of registered recipes, you can modify this if you want too.
	 * @return currentlyRegistredRecipes
	 */
	public List<IAppEngGrinderRecipe> getRecipes();
	
	/**
	 * add a new recipe the easy way, in -> out, how many turns.
	 * @param in
	 * @param out
	 * @param cost
	 */
	public void addRecipe( ItemStack in, ItemStack out, int cost );
	
	/**
	 * Searches for a recipe for a given input, and returns it.
	 * @param input
	 * @return identified recipe, or null
	 */
	public IAppEngGrinderRecipe getRecipeForInput(ItemStack input);
}
