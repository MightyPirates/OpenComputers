package appeng.api.me.tiles;

import appeng.api.me.util.IMEInventoryHandler;

/**
 * Both useless and incredibly useful, maybe...
 */
public interface ICellProvider
{
	/**
	 * consume power to add an item to the storage system.
	 * @param items
	 * @param multiplier
	 * @return
	 */
	public int usePowerForAddition( int items, int multiplier );
	
	/**
	 * returns a ME Inventory for interaction.
	 */
    public IMEInventoryHandler provideCell();
    
}
