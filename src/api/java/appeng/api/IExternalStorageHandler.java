package appeng.api;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import appeng.api.me.util.IMEInventory;

/**
 * A Registration for a special storage handler.
 *
 */
public interface IExternalStorageHandler {
	
	/**
	 * if this can handle the provided inventory, return true. ( Generally skipped by AE, and it just calls getInventory )
	 * @param te
	 * @return true, if it can get a handler via getInventory
	 */
	boolean canHandle( TileEntity te, ForgeDirection d );
	
	/**
	 * if this can handle the given inventory, return the a IMEInventory implemnting class for it, if not return null
	 * @param te
	 * @return HandlerFor Inventory.
	 */
	IMEInventory getInventory( TileEntity te, ForgeDirection d );
	
}
