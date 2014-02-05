package appeng.api.me.tiles;

import appeng.api.IItemList;

@Deprecated
public interface IStorageAware {
	
	/**
	 * if your GridTile Implements this interface, the Network will notify it of changes, and send you a list of the contents so you can updates statues, WAY BETTER then polling.
	 * @param iss
	 */
	void onNetworkInventoryChange( IItemList iss );
	
}
