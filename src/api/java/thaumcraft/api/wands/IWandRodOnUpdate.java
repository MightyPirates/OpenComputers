package thaumcraft.api.wands;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author azanor
 * 
 * Implemented by a class that you wish to be called whenever a wand with this rod performs its
 * update tick. 
 *
 */
public interface IWandRodOnUpdate {
	void onUpdate(ItemStack itemstack, EntityPlayer player);
}
