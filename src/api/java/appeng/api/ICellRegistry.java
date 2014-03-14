package appeng.api;

import net.minecraft.item.ItemStack;
import appeng.api.me.util.IMEInventoryHandler;

/**
 * Storage Cell Registry, used for specially implemented cells, if you just want to make a item act like a cell, or new cell with diffrent bytes, then you should probobly consider IStorageCell instead its consideriablly simpler.
 */
public interface ICellRegistry {
	
	/**
	 * Register a new handler.
	 * @param h
	 */
	void addCellHandler( ICellHandler h );
	
	/**
	 * returns true if the provided item, can be handled by a handler in AE, ( AE May choose to skip this and just get the handler instead. )
	 * @param is
	 * @return returns true, if you can get a InventoryHandler for the item passed.
	 */
	boolean isCellHandled( ItemStack is );
	
	/**
	 * returns an IMEInventoryHandler for the provided item.
	 * @param is
	 * @return new IMEInventoryHandler, or null if there isn't one.
	 */
	IMEInventoryHandler getHandlerForCell( ItemStack is );
	
}
