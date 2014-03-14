package appeng.api.me.tiles;

import appeng.api.me.util.IMEInventoryHandler;

/**
 * Both useless and incredibly useful, maybe...
 */
public interface IExtendedCellProvider extends ICellProvider
{
	
    public IMEInventoryHandler provideCell( String Filter );
    
}
