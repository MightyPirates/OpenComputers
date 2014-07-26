package cofh.api.transport;

import net.minecraft.item.ItemStack;

/**
 * This interface is implemented on Ender Attuned objects which can receive Items.
 * 
 * @author King Lemming
 * 
 */
public interface IEnderItemHandler extends IEnderAttuned {

	/**
	 * Return whether or not the Ender Attuned object can currently send ItemStacks.
	 */
	boolean canSendItems();

	/**
	 * This should be checked to see if the Ender Attuned object can currently receive an ItemStack.
	 * 
	 * Note: In practice, this can (and should) be used to ensure that something does not send to itself.
	 */
	boolean canReceiveItems();

	/**
	 * This tells the Ender Attuned object to receive an ItemStack. This returns what remains of the original stack, *not* the amount received - a null return
	 * means that the entire stack was received!
	 * 
	 * This function does not support simulation because Inventory manipulation in Minecraft is an absolute mess and it would be a computational liability to do
	 * so.
	 * 
	 * @param item
	 *            ItemStack to be received.
	 * @return An ItemStack representing how much is remaining.
	 */
	ItemStack receiveItem(ItemStack item);

}
