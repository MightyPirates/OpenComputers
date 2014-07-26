/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.apiculture;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;

import forestry.api.genetics.IFlowerProvider;

public class FlowerManager {
	/**
	 * ItemStacks representing simple flower blocks. Meta-sensitive, processed by the basic {@link IFlowerProvider}.
	 */
	public static ArrayList<ItemStack> plainFlowers = new ArrayList<ItemStack>();
}
