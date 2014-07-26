/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface IArmorNaturalist {

	/**
	 * Called when the naturalist's armor acts as spectacles for seeing pollinated tree leaves/flowers.
	 * 
	 * @param player
	 *            Player doing the viewing
	 * @param armor
	 *            Armor item
	 * @param doSee
	 *            Whether or not to actually do the side effects of viewing
	 * @return true if the armor actually allows the player to see pollination.
	 */
	public boolean canSeePollination(EntityPlayer player, ItemStack armor, boolean doSee);
}
