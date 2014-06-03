package thaumcraft.api.wands;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 *  
 * @author azanor
 * 
 * Add this to a tile entity that you wish wands to interact with in some way. 
 *
 */

public interface IWandable {

	public int onWandRightClick(World world, ItemStack wandstack, EntityPlayer player, int x, int y, int z, int side, int md);
	
	public ItemStack onWandRightClick(World world, ItemStack wandstack, EntityPlayer player);
	
	public void onUsingWandTick(ItemStack wandstack, EntityPlayer player, int count);
	
	public void onWandStoppedUsing(ItemStack wandstack, World world, EntityPlayer player, int count);
	
}
