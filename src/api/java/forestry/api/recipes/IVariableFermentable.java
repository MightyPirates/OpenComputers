/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.recipes;

import net.minecraft.item.ItemStack;

/**
 * Fermenter checks any valid fermentation item for an implementation of this interface.
 * This does not supersede adding a proper recipe to the fermenter!
 */
public interface IVariableFermentable {
	
	/**
	 * @param itemstack
	 * @return Float representing the modification to be applied to the matching recipe's biomass output.
	 */
	float getFermentationModifier(ItemStack itemstack);
}
