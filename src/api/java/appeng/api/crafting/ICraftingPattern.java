package appeng.api.crafting;

import appeng.api.storage.data.IAEItemStack;

/**
 * this class can be implemented if you wish to implement a TileCraftingProvider.
 */
public interface ICraftingPattern
{

	/**
	 * @return the outcome, MUST have a value
	 */
	IAEItemStack getOutput();

}