package appeng.api.storage;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * A Registration Record for {@link IExternalStorageRegistry}
 */
public interface IExternalStorageHandler
{

	/**
	 * if this can handle the provided inventory, return true. ( Generally
	 * skipped by AE, and it just calls getInventory )
	 * 
	 * @param te
	 * @return true, if it can get a handler via getInventory
	 */
	boolean canHandle(TileEntity te, ForgeDirection d, StorageChannel channel);

	/**
	 * if this can handle the given inventory, return the a IMEInventory
	 * implementing class for it, if not return null
	 * 
	 * @param te
	 * @return The Handler for the inventory
	 */
	IMEInventory getInventory(TileEntity te, ForgeDirection d, StorageChannel channel);

}