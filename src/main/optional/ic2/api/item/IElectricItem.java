package ic2.api.item;

import net.minecraft.item.ItemStack;

/**
 * Provides the ability to store energy on the implementing item.
 *
 * The item should have a maximum damage of 13.
 */
public interface IElectricItem {
	/**
	 * Determine if the item can be used in a machine or as an armor part to supply energy.
	 *
	 * @return Whether the item can supply energy
	 */
	boolean canProvideEnergy(ItemStack itemStack);

	/**
	 * Get the item ID to use for a charge energy greater than 0.
	 *
	 * @return Item ID to use
	 */
	int getChargedItemId(ItemStack itemStack);

	/**
	 * Get the item ID to use for a charge energy of 0.
	 *
	 * @return Item ID to use
	 */
	int getEmptyItemId(ItemStack itemStack);

	/**
	 * Get the item's maximum charge energy in EU.
	 *
	 * @return Maximum charge energy
	 */
	int getMaxCharge(ItemStack itemStack);

	/**
	 * Get the item's tier, lower tiers can't send energy to higher ones.
	 * Batteries are Tier 1, Energy Crystals are Tier 2, Lapotron Crystals are Tier 3.
	 *
	 * @return Item's tier
	 */
	int getTier(ItemStack itemStack);

	/**
	 * Get the item's transfer limit in EU per transfer operation.
	 *
	 * @return Transfer limit
	 */
	int getTransferLimit(ItemStack itemStack);
}

