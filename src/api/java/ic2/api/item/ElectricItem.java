package ic2.api.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Allows for charging, discharging and using electric items (IElectricItem).
 *
 * The charge or remaining capacity of an item can be determined by calling charge/discharge with
 * ignoreTransferLimit and simulate set to true.
 */
public final class ElectricItem {
	/**
	 * IElectricItemManager to use for interacting with IElectricItem ItemStacks.
	 * 
	 * This manager will act as a gateway and delegate the tasks to the final implementation
	 * (rawManager or a custom one) as necessary.
	 */
	public static IElectricItemManager manager;

	/**
	 * Standard IElectricItemManager implementation, only call it directly from another
	 * IElectricItemManager. Use manager instead.
	 */
	public static IElectricItemManager rawManager;

	/**
	 * Charge an item with a specified amount of energy
	 *
	 * @param itemStack electric item's stack
	 * @param amount amount of energy to charge in EU
	 * @param tier tier of the charging device, has to be at least as high as the item to charge
	 * @param ignoreTransferLimit ignore the transfer limit specified by getTransferLimit()
	 * @param simulate don't actually change the item, just determine the return value
	 * @return Energy transferred into the electric item
	 * 
	 * @deprecated use manager.charge() instead
	 */
	@Deprecated
	public static int charge(ItemStack itemStack, int amount, int tier, boolean ignoreTransferLimit, boolean simulate) {
		return manager.charge(itemStack, amount, tier, ignoreTransferLimit, simulate);
	}

	/**
	 * Discharge an item by a specified amount of energy
	 *
	 * @param itemStack electric item's stack
	 * @param amount amount of energy to charge in EU
	 * @param tier tier of the discharging device, has to be at least as high as the item to discharge
	 * @param ignoreTransferLimit ignore the transfer limit specified by getTransferLimit()
	 * @param simulate don't actually discharge the item, just determine the return value
	 * @return Energy retrieved from the electric item
	 * 
	 * @deprecated use manager.discharge() instead
	 */
	@Deprecated
	public static int discharge(ItemStack itemStack, int amount, int tier, boolean ignoreTransferLimit, boolean simulate) {
		return manager.discharge(itemStack, amount, tier, ignoreTransferLimit, simulate);
	}

	/**
	 * Determine if the specified electric item has at least a specific amount of EU.
	 * This is supposed to be used in the item code during operation, for example if you want to implement your own electric item.
	 * BatPacks are not taken into account.
	 *
	 * @param itemStack electric item's stack
	 * @param amount minimum amount of energy required
	 * @return true if there's enough energy
	 * 
	 * @deprecated use manager.canUse() instead
	 */
	@Deprecated
	public static boolean canUse(ItemStack itemStack, int amount) {
		return manager.canUse(itemStack, amount);
	}

	/**
	 * Try to retrieve a specific amount of energy from an Item, and if applicable, a BatPack.
	 * This is supposed to be used in the item code during operation, for example if you want to implement your own electric item.
	 *
	 * @param itemStack electric item's stack
	 * @param amount amount of energy to discharge in EU
	 * @param player player holding the item
	 * @return true if the operation succeeded
	 * 
	 * @deprecated use manager.use() instead
	 */
	@Deprecated
	public static boolean use(ItemStack itemStack, int amount, EntityPlayer player) {
		return manager.use(itemStack, amount, player);
	}

	/**
	 * Charge an item from the BatPack a player is wearing.
	 * This is supposed to be used in the item code during operation, for example if you want to implement your own electric item.
	 * use() already contains this functionality.
	 *
	 * @param itemStack electric item's stack
	 * @param player player holding the item
	 * 
	 * @deprecated use manager.chargeFromArmor() instead
	 */
	@Deprecated
	public static void chargeFromArmor(ItemStack itemStack, EntityPlayer player) {
		manager.chargeFromArmor(itemStack, player);
	}
}

