package thaumcraft.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author Azanor
 * 
 * Equipped head slot items that extend this class will be able to perform most functions that 
 * goggles of revealing can apart from view nodes which is handled by IRevealer.
 *
 */

public interface IGoggles {
	
	/*
	 * If this method returns true things like block essentia contents will be shown.
	 */
	public boolean showIngamePopups(ItemStack itemstack, EntityLivingBase player);

}
