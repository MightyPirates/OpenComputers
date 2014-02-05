package appeng.api.me.tiles;

import java.util.List;

import net.minecraft.item.ItemStack;
import appeng.api.me.util.IMEInventory;
import appeng.api.me.util.InterfaceCraftingResponse;

/**
 * Implemented by the ME Interface
 * give direct access to some more complicated logic, in a simpler way, was used for earily LP integration.
 */
public interface ITileInterfaceApi {
	
	/**
	 * how much space is available.
	 * @param i
	 * @param MaxNeeded, a cut off where you stop caring
	 * @return totalAmountNeeded
	 */
	int apiCurrentAvailableSpace(ItemStack i, int MaxNeeded);
	
	/**
	 * Attempt to extract an item from the network.
	 * @param i, item to extract.
	 * @param doExtract, if you don't its just a simulation.
	 * @return anyExtracted Items, or null
	 */
	ItemStack apiExtractNetworkItem(ItemStack i, boolean doExtract);
	
	/**
	 * Adds an item to the network
	 * @param i
	 * @param doAdd, or simulate
	 * @return any items that couldn't be added, or nul
	 */
	ItemStack apiAddNetworkItem(ItemStack i, boolean doAdd);
	
	/**
	 * returns a list of ItemStacks that are contined in the network.
	 * @return
	 */
	List<ItemStack> apiGetNetworkContents();
	
	/**
	 * get access to the networks IMEInventory.
	 * @return
	 */
	IMEInventory getApiArray();
	
	/**
	 * get a list of items that can be crafted by the network.
	 * @return
	 */
	List<ItemStack> getCraftingOptions();
	
	/**
	 * get access to the crafting patterns.
	 * @param req
	 * @return
	 */
	List<InterfaceCraftingPattern> findCraftingPatterns( ItemStack req );
	
	/**
	 * issue a new crafting request.
	 * @param req
	 * @param enableRecursive
	 * @return
	 */
	InterfaceCraftingResponse requestCrafting( ItemStack req, boolean enableRecursive );

	/**
	 * if there is anything at all available it returns true.
	 * @return
	 */
	boolean containsItems();
	
}
