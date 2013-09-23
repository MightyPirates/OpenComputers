package ic2.api.item;

import net.minecraft.item.ItemStack;

/**
 * Provides the ability to store energy on the implementing item.
 *
 * This interface is a special version of IElectricItem which delegates the implementation of
 * charge(), discharge() and canUse() to the implementing Item.
 *
 * The default implementation (when not using ICustomElectricItem) does the following:
 * - store and retrieve the charge
 * - handle charging, taking amount, tier, transfer limit, canProvideEnergy and simulate into account
 * - replace item IDs if appropriate (getChargedItemId() and getEmptyItemId())
 * - update and manage the damage value for the visual charge indicator
 *
 * @note ICustomElectricItem must not call the ElectricItem methods charge, discharge or canUse
 * 
 * @deprecated Use ISpecialElectricItem instead.
 */
@Deprecated
public interface ICustomElectricItem extends IElectricItem {
	/**
	 * Charge an item with a specified amount of energy
	 *
	 * @param itemStack electric item's stack
	 * @param amount amount of energy to charge in EU
	 * @param tier tier of the charging device, has to be at least as high as the item to charge
	 * @param ignoreTransferLimit ignore the transfer limit specified by getTransferLimit()
	 * @param simulate don't actually change the item, just determine the return value
	 * @return Energy transferred into the electric item
	 */
	public int charge(ItemStack itemStack, int amount, int tier, boolean ignoreTransferLimit, boolean simulate);

	/**
	 * Discharge an item by a specified amount of energy
	 *
	 * @param itemStack electric item's stack
	 * @param amount amount of energy to charge in EU
	 * @param tier tier of the discharging device, has to be at least as high as the item to discharge
	 * @param ignoreTransferLimit ignore the transfer limit specified by getTransferLimit()
	 * @param simulate don't actually discharge the item, just determine the return value
	 * @return Energy retrieved from the electric item
	 */
	public int discharge(ItemStack itemStack, int amount, int tier, boolean ignoreTransferLimit, boolean simulate);

	/**
	 * Determine if the specified electric item has at least a specific amount of EU.
	 * This is supposed to be used in the item code during operation, for example if you want to implement your own electric item.
	 * BatPacks are not taken into account.
	 *
	 * @param itemStack electric item's stack
	 * @param amount minimum amount of energy required
	 * @return true if there's enough energy
	 */
	public boolean canUse(ItemStack itemStack, int amount);

	/**
	 * Determine whether to show the charge tool tip with NEI or other means.
	 *
	 * Return false if IC2's handler is incompatible, you want to implement your own or you don't
	 * want to display the charge at all.
	 *
	 * @return true to show the tool tip (x/y EU)
	 */
	public boolean canShowChargeToolTip(ItemStack itemStack);
}
