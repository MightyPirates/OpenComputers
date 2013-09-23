package ic2.api.network;

/**
 * Allows a tile entity to receive field sync updates received from the server. 
 */
public interface INetworkUpdateListener {
	/**
	 * Called when a field is synchronized.
	 * 
	 * @param field field synchronized
	 */
	void onNetworkUpdate(String field);
}

