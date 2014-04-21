package appeng.api.crafting;

import appeng.api.config.Actionable;
import net.minecraft.item.ItemStack;

/**
 * A place to send Items for crafting purposes, this is considered part of AE's External crafting system.
 */
public interface ICraftingMedium
{

	/**
	 * @return true if this crafting medium cannot take new requests at this time.
	 */
	boolean isBusy();

	/**
	 * Attempt to send an item.
	 * 
	 * @param itemToInject
	 * @return the amount of the item that could not be handled at this time, or null if it was fully injected.
	 */
	ItemStack InjectItem(ItemStack itemToInject, Actionable mode);

}
