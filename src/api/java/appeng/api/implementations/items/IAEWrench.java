package appeng.api.implementations.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Implemented on AE's wrench(s) as a substitute for if BC's API is not
 * available.
 */
public interface IAEWrench
{

	/**
	 * Check if the wrench can be used.
	 * 
	 * @param player
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	boolean canWrench(ItemStack wrench, EntityPlayer player, int x, int y, int z);

}
