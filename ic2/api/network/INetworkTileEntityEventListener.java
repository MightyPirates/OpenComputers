package ic2.api.network;

/**
 * Allows a tile entity to receive network events received from the server.
 */
public interface INetworkTileEntityEventListener {
	/**
	 * Called when a network event is received.
	 * 
	 * @param event Event ID
	 */
	void onNetworkEvent(int event);
}

