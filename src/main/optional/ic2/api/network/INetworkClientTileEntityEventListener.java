package ic2.api.network;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Allows a tile entity to receive network events received from clients.
 */
public interface INetworkClientTileEntityEventListener {
	/**
	 * Called when a network event is received.
	 * 
	 * @param player client which sent the event
	 * @param event event ID
	 */
	void onNetworkEvent(EntityPlayer player, int event);
}

