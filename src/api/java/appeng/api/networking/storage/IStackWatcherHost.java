package appeng.api.networking.storage;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;

public interface IStackWatcherHost
{

	/**
	 * provides the IStackWatcher for this host, for the current network, is called when the hot changes networks. You
	 * do not need to clear your old watcher, its already been removed by the time this gets called.
	 * 
	 * @param newWatcher
	 */
	void updateWatcher(IStackWatcher newWatcher);

	/**
	 * Called when a watched item changes amounts.
	 * 
	 * @param o
	 * @param fullStack
	 * @param diffStack
	 * @param src
	 * @param chan
	 */
	void onStackChange(IItemList o, IAEStack fullStack, IAEStack diffStack, BaseActionSource src, StorageChannel chan);

}
