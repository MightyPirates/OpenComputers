/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.recipes;

import net.minecraft.item.ItemStack;

import net.minecraftforge.fluids.FluidStack;

/**
 * Provides an interface to the recipe manager of the bottler.
 * 
 * The manager is initialized at the beginning of Forestry's BaseMod.load() cycle. Begin adding recipes in BaseMod.ModsLoaded() and this shouldn't be null even
 * if your mod loads before Forestry.
 * 
 * Accessible via {@link RecipeManagers}
 * 
 * Note that this is untested with anything other than biofuel->fuelcan conversion.
 * 
 * @author SirSengir
 */
public interface IBottlerManager extends ICraftingProvider {
	/**
	 * Add a recipe to the bottler.
	 * The bottler will populate its recipe list dynamically from the LiquidContainerRegistry. Recipes added explicitely will take precedence.
	 * 
	 * @param cyclesPerUnit
	 *            Amount of work cycles required to run through the conversion once.
	 * @param input
	 *            LiquidStack representing the input liquid.
	 * @param can
	 *            ItemStack representing the cans, capsules and/or cells required
	 * @param bottled
	 *            ItemStack representing the finished product
	 */
	@Deprecated
	public void addRecipe(int cyclesPerUnit, FluidStack input, ItemStack can, ItemStack bottled);
}
