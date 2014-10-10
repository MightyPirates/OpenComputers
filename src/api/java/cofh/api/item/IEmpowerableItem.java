package cofh.api.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Implement this interface on Item classes which may be "Empowered" - what that means is completely up to you. This just provides a uniform way of dealing with
 * them.
 * 
 * @author King Lemming
 * 
 */
public interface IEmpowerableItem {

	/**
	 * Check whether or not a given item is currently in an empowered state.
	 */
	boolean isEmpowered(ItemStack stack);

	/**
	 * Attempt to set the empowered state of the item.
	 * 
	 * @param stack
	 *            ItemStack to be empowered/disempowered.
	 * @param state
	 *            Desired state.
	 * @return TRUE if the operation was successful, FALSE if it was not.
	 */
	boolean setEmpoweredState(ItemStack stack, boolean state);

	/**
	 * Callback method for reacting to a state change. Useful in KeyBinding handlers.
	 * 
	 * @param player
	 *            Player holding the item, if applicable.
	 * @param stack
	 *            The item being held.
	 */
	void onStateChange(EntityPlayer player, ItemStack stack);

}
