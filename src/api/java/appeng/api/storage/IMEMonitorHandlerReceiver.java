package appeng.api.storage;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.data.IAEStack;

public interface IMEMonitorHandlerReceiver<StackType extends IAEStack>
{

	/**
	 * return true if this object should remain as a listener.
	 * 
	 * @param verificationToken
	 * @return
	 */
	boolean isValid(Object verificationToken);

	/**
	 * called when changes are made to the Monitor, but only if listener is still valid.
	 * 
	 * @param change
	 */
	void postChange(IMEMonitor<StackType> monitor, StackType change, BaseActionSource actionSource);

	/**
	 * called when the list updates its contents, this is mostly for handling power events.
	 */
	void onListUpdate();

}
