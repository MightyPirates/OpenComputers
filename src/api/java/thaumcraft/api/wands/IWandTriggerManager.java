package thaumcraft.api.wands;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface IWandTriggerManager {

	public boolean performTrigger(World world, ItemStack wand, EntityPlayer player, 
			int x, int y, int z, int side, int event);
	
}
