package universalelectricity.api.item;

import net.minecraft.item.ItemStack;

public interface IEnergyItem
{
	/**
	 * Adds energy to an item. Returns the quantity of energy that was accepted. This should always
	 * return 0 if the item cannot be externally charged.
	 * 
	 * @param itemStack ItemStack to be charged.
	 * @param energy Maximum amount of energy to be sent into the item.
	 * @param doRecharge If false, the charge will only be simulated.
	 * @return Amount of energy that was accepted by the item.
	 */
	public long recharge(ItemStack itemStack, long energy, boolean doRecharge);

	/**
	 * Removes energy from an item. Returns the quantity of energy that was removed. This should
	 * always return 0 if the item cannot be externally discharged.
	 * 
	 * @param itemStack ItemStack to be discharged.
	 * @param energy Maximum amount of energy to be removed from the item.
	 * @param doDischarge If false, the discharge will only be simulated.
	 * @return Amount of energy that was removed from the item.
	 */
	public long discharge(ItemStack itemStack, long energy, boolean doDischarge);

	/**
	 * Get the amount of energy currently stored in the item.
	 */
	public long getEnergy(ItemStack theItem);

	/**
	 * Get the max amount of energy that can be stored in the item.
	 */
	public long getEnergyCapacity(ItemStack theItem);

	/**
	 * Sets the amount of energy in the ItemStack. Use recharge or discharge instead of calling this
	 * to be safer!
	 * 
	 * @param itemStack - the ItemStack.
	 * @param energy - Amount of electrical energy.
	 */
	public void setEnergy(ItemStack itemStack, long energy);
}
