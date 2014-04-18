package appeng.api.storage;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;

public interface IMEMonitor<T extends IAEStack> extends IMEInventoryHandler<T>
{

	/**
	 * add a new Listener to the monitor, be sure to properly remove yourself when your done.
	 */
	void addListener(IMEMonitorHandlerReceiver<T> l, Object verificationToken);

	/**
	 * remove a Listener to the monitor.
	 */
	void removeListener(IMEMonitorHandlerReceiver<T> l);

	@Override
	@Deprecated
	/**
	 * This method is discouraged when accessing data via a IMEMonitor
	 */
	public IItemList<T> getAvailableItems(IItemList out);

	/**
	 * Get access to the full item list of the network, preferred over {@link IMEInventory} .getAvaialbleItems(...)
	 * 
	 * @return full storage list.
	 */
	IItemList<T> getStorageList();

}
