package appeng.api.me.tiles;

import java.util.List;

import appeng.api.me.util.IMEInventoryHandler;

/**
 * Represents a tile that contributes to storage, such as a ME Chest, or ME Drive.
 * 
 * Remember to trigger the appropriate storage update vents, when your getCellArray Changes. * 
 */
public interface ICellContainer {
	
	/**
	 * Inventory of the tile for use with ME, should always return an valid list, never NULL.
	 */
	List<IMEInventoryHandler> getCellArray();
	
	/**
	 * the storage's priority.
	 */
	int getPriority();
	
}
