package appeng.api.crafting;

import appeng.api.networking.events.MENetworkCraftingChange;

/**
 * Allows a IGridHost to provide crafting patterns to the network, post a
 * {@link MENetworkCraftingChange} to tell AE to update.
 */
public interface ICraftingProvider extends ICraftingMedium
{

	/**
	 * called when the network is looking for possible crafting jobs.
	 * 
	 * @param craftingTracker
	 */
	void provideCrafting(ICraftingProviderHelper craftingTracker);

}
