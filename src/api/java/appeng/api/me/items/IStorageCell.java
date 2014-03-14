package appeng.api.me.items;

import appeng.api.IAEItemStack;
import net.minecraft.item.ItemStack;

/**
 * Any item which implements this can be treated as an IMEInventory via Util.getCell / Util.isCell
 * It automatically handles the internals and NBT data, which is both nice, and bad for you!
 */
public interface IStorageCell
{
    /**
     * rv0 - If this returns something where N % 8 != 0 Then you will be shot on sight, or your car will explode, something like that least...
     * @param cellItem
     * @return numberofBytes
     */
    int getBytes( ItemStack cellItem );
    
    /**
     * rv0 - Determines the number of bytes used for any type included on the cell.
     * @param cellItem
     * @return numberOfBytes
     */
	int BytePerType( ItemStack iscellItem );
	
	/**
	 * rv11 - Must be between 1 and 63, indicates how many types you want to store on the item.
	 * @param cellItem
	 * @return numberOfTypes
	 */
	int getTotalTypes( ItemStack cellItem );
	
	/**
	 * rv11 - allows you to fine tune which items are allowed on a given cell,
	 * if you don't care, just return false;
	 * As the handler for this type of cell is still the default cells, the normal AE black list is also applied.
	 * @param cellItem
	 * @param requsetedAddition
	 * @return preventAdditionOfItem
	 */
	boolean isBlackListed( ItemStack cellItem, IAEItemStack requsetedAddition );
	
	/**
	 * rv11 - allows you to specify if this storage cell can be stored inside other storage cells, only set this for special items like the matter cannon that are not general purpose storage.
	 * @return
	 */
	boolean storableInStorageCell();

	/**
	 * rv14 - allows an item to selectivly enable or disable its status as a storage cell.
	 * @param i
	 * @return
	 */
	boolean isStorageCell( ItemStack i );
}
