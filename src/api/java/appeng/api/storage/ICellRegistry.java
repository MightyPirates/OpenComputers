package appeng.api.storage;

import net.minecraft.item.ItemStack;
import appeng.api.IAppEngApi;

/**
 * Storage Cell Registry, used for specially implemented cells, if you just want to make a item act like a cell, or new
 * cell with different bytes, then you should probably consider IStorageCell instead its considerably simpler.
 * 
 * Do not Implement, obtained via {@link IAppEngApi}.getCellRegistry()
 */
public interface ICellRegistry
{

	/**
	 * Register a new handler.
	 * 
	 * @param handler
	 */
	void addCellHandler(ICellHandler handler);

	/**
	 * return true, if you can get a InventoryHandler for the item passed.
	 * 
	 * @param is
	 * @return true if the provided item, can be handled by a handler in AE, ( AE May choose to skip this and just get
	 *         the handler instead. )
	 */
	boolean isCellHandled(ItemStack is);

	/**
	 * get the handler, for the requested type.
	 * 
	 * @param is
	 * @return the handler registered for this item type.
	 */
	ICellHandler getHandler(ItemStack is);

	/**
	 * returns an IMEInventoryHandler for the provided item.
	 * 
	 * @param is
	 * @return new IMEInventoryHandler, or null if there isn't one.
	 */
	IMEInventoryHandler getCellInventory(ItemStack is, StorageChannel chan);

}