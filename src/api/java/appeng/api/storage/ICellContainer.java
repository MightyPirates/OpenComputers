package appeng.api.storage;

import appeng.api.networking.security.IActionHost;

import java.util.List;

/**
 * Represents a IGridhost that contributes to storage, such as a ME Chest, or ME Drive.
 */
public interface ICellContainer extends IActionHost
{

	/**
	 * Inventory of the tile for use with ME, should always return an valid list, never NULL.
	 * 
	 * You must return the correct Handler for the correct channel, if your handler returns a IAEItemStack handler, for
	 * a Fluid Channel stuffs going to explode, same with the reverse.
	 * 
	 * @return a valid list of handlers, NEVER NULL
	 */
	List<IMEInventoryHandler> getCellArray(StorageChannel channel);

	/**
	 * the storage's priority.
	 * 
	 * Positive and negative are supported
	 */
	int getPriority();

	/**
	 * tell the Cell container that this slot should blink, the slot number is relative to the
	 * 
	 * @param slot
	 */
	void blinkCell(int slot);

}
