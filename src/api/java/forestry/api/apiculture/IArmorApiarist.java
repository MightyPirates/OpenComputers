/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.apiculture;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * When implemented by armor piece items, allows them to act as apiarist's armor.
 */
public interface IArmorApiarist {
	/**
	 * Called when the apiarist's armor acts as protection against an attack.
	 * 
	 * @param player
	 *            Player being attacked
	 * @param armor
	 *            Armor item
	 * @param cause
	 *            Optional cause of attack, such as a bee effect identifier
	 * @param doProtect
	 *            Whether or not to actually do the side effects of protection
	 * @return Whether or not the armor should protect the player from that attack
	 */
	public boolean protectPlayer(EntityPlayer player, ItemStack armor, String cause, boolean doProtect);
}
