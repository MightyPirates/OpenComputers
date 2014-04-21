package appeng.api.networking.storage;

import appeng.api.networking.IGridCache;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEStack;

/**
 * Common base class for item / fluid storage caches.
 */
public interface IStorageGrid extends IGridCache, IStorageMonitorable
{

	/**
	 * Used to inform the network of alterations to the storage system that fall outside of the standard Network
	 * operations, Examples, ME Chest inputs from the world, or a Storage Bus detecting modifications made to the chest
	 * by an outside force.
	 * 
	 * Expects the input to have either a negative or a positive stack size to correspond to the injection, or
	 * extraction operation.
	 * 
	 * @param input
	 */
	void postAlterationOfStoredItems(StorageChannel chan, IAEStack input, BaseActionSource src);

}
