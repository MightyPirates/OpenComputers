package ic2.api.network;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Allows an item to receive network events received from the server.
 */
public interface INetworkItemEventListener {
	/**
	 * Called when a network event is received.
	 * 
	 * @param metaData item metadata
	 * @param player player containing the item
	 * @param event event ID
	 */
	void onNetworkEvent(int metaData, EntityPlayer player, int event);
}

