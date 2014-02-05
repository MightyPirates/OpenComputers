package appeng.api.me.util;

import appeng.api.me.tiles.ICraftingTracker;
import appeng.api.me.tiles.ITilePushable;

/**
 * Allows a tile to provide crafting patterns to the network, post a GridPatternUpdateEvent to tell AE to update.
 */
public interface ITileCraftingProvider extends ITilePushable {
	
	/**
	 * called when the network is looking for possible crafting jobs.
	 * @param craftingTracker
	 */
	void provideCrafting( ICraftingTracker craftingTracker );

}
