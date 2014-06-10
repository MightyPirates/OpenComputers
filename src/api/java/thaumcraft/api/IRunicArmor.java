package thaumcraft.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author Azanor
 * 
 * Armor or bauble slot items that implement this interface can provide runic shielding.
 * Recharging, hardening, etc. is handled internally by thaumcraft. 
 *
 */

public interface IRunicArmor {
	
	/**
	 * returns how much charge this item can provide. This is the base shielding value - any hardening is stored and calculated internally. 
	 */
	public int getRunicCharge(ItemStack itemstack);
	

}
