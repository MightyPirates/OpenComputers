package thaumcraft.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;



/**
 * @author Azanor
 * Items, armor and tools with this interface can receive the Repair enchantment. 
 * Repairs 1 point of durability every 10 seconds (2 for repair II)
 */
public interface IRepairableExtended extends IRepairable {
	
	public boolean doRepair(ItemStack stack, EntityPlayer player, int enchantlevel);

}
