package thaumcraft.api.nodes;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author Azanor
 * 
 * Equipped head slot items that extend this class will make nodes visible in world.
 *
 */

public interface IRevealer {
	
	/*
	 * If this method returns true the nodes will be visible.
	 */
	public boolean showNodes(ItemStack itemstack, EntityLivingBase player);
	

}
