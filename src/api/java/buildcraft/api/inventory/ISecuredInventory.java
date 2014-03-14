package buildcraft.api.inventory;

import net.minecraftforge.common.ForgeDirection;

public interface ISecuredInventory {

	/**
	 * @param name
	 * @return true if the user/player with the given name has access permissions on this machine.
	 */
	boolean canAccess(String name);

	/**
	 * Informs the inventory with whose permissions the next item or liquid transaction will be performed. It is up to the inventory to determine the effect.
	 * 
	 * @param orientation
	 *            Orientation the transaction will be performed from.
	 * @param name
	 *            Name of the user/player who owns the transaction.
	 */
	void prepareTransaction(ForgeDirection orientation, String name);

}
