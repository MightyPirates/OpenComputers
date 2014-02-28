package ic2.api.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Allows an item to receive network events received from the server.
 */
public interface INetworkItemEventListener {
	/**
	 * Called when a network event is received.
	 * 
	 * @param itemStack item stack
	 * @param player player containing the item
	 * @param event event ID
	 */
	void onNetworkEvent(ItemStack stack, EntityPlayer player, int event);
}

