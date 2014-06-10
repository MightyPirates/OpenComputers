package thaumcraft.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.Aspect;




/**
 * @author Azanor
 * ItemArmor with this interface will grant a discount to the vis cost of actions the wearer performs with casting wands.
 * The amount returned is the percentage by which the cost is discounted. There is a built-int max discount of 50%, but 
 * individual items really shouldn't have a discount more than 5%
 */
public interface IVisDiscountGear {
		
	int getVisDiscount(ItemStack stack, EntityPlayer player, Aspect aspect);

}
