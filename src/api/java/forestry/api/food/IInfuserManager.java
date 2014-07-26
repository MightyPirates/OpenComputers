/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.food;

import net.minecraft.item.ItemStack;

public interface IInfuserManager {

	void addMixture(int meta, ItemStack ingredient, IBeverageEffect effect);

	void addMixture(int meta, ItemStack[] ingredients, IBeverageEffect effect);

	ItemStack getSeasoned(ItemStack base, ItemStack[] ingredients);

	boolean hasMixtures(ItemStack[] ingredients);

	ItemStack[] getRequired(ItemStack[] ingredients);

}
