package forestry.api.recipes;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraftforge.fluids.FluidStack;

/**
 * Provides an interface to the recipe manager of the carpenter.
 * 
 * The manager is initialized at the beginning of Forestry's BaseMod.load() cycle. Begin adding recipes in BaseMod.ModsLoaded() and this shouldn't be null even
 * if your mod loads before Forestry.
 * 
 * Accessible via {@link RecipeManagers}
 * 
 * Only shaped recipes can be added currently.
 * 
 * @author SirSengir
 */
public interface ICarpenterManager extends ICraftingProvider {
	/**
	 * Add a shaped recipe to the carpenter.
	 * 
	 * @param box
	 *            ItemStack of one item representing the required box (carton, crate) for this recipe. May be null.
	 * @param product
	 *            Crafting result.
	 * @param materials
	 *            Materials needed in the crafting matrix. This gets passed directly to {@link ShapedRecipes}. Notation is the same.
	 */
	public void addRecipe(ItemStack box, ItemStack product, Object materials[]);

	/**
	 * Add a shaped recipe to the carpenter.
	 * 
	 * @param packagingTime
	 *            Number of work cycles required to craft the recipe once.
	 * @param box
	 *            ItemStack of one item representing the required box (carton, crate) for this recipe. May be null.
	 * @param product
	 *            Crafting result.
	 * @param materials
	 *            Materials needed in the crafting matrix. This gets passed directly to {@link ShapedRecipes}. Notation is the same.
	 */
	public void addRecipe(int packagingTime, ItemStack box, ItemStack product, Object materials[]);

	/**
	 * Add a shaped recipe to the carpenter.
	 * 
	 * @param packagingTime
	 *            Number of work cycles required to craft the recipe once.
	 * @param liquid
	 *            Liquid required in carpenter's tank.
	 * @param box
	 *            ItemStack of one item representing the required box (carton, crate) for this recipe. May be null.
	 * @param product
	 *            Crafting result.
	 * @param materials
	 *            Materials needed in the crafting matrix. This gets passed directly to {@link ShapedRecipes}. Notation is the same.
	 */
	public void addRecipe(int packagingTime, FluidStack liquid, ItemStack box, ItemStack product, Object materials[]);

	public void addCrating(String toCrate, ItemStack unpack, ItemStack crated);

	public void addCrating(ItemStack itemStack);
}
