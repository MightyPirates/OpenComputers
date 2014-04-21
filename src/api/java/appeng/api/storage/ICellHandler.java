package appeng.api.storage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import appeng.api.implementations.tiles.IChestOrDrive;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Registration record for {@link ICellRegistry}
 */
public interface ICellHandler
{

	/**
	 * return true if the provided item is handled by your cell handler. ( AE May choose to skip this method, and just
	 * request a handler )
	 * 
	 * @param is
	 * @return return true, if getCellHandler will not return null.
	 */
	boolean isCell(ItemStack is);

	/**
	 * If you cannot handle the provided item, return null
	 * 
	 * @param is
	 *            a storage cell item.
	 * 
	 * @return a new IMEHandler for the provided item
	 */
	IMEInventoryHandler getCellInventory(ItemStack is, StorageChannel channel);

	/**
	 * @return the ME Chest texture for this storage cell type, should be 10x10 with 3px of transparent padding on a
	 *         16x16 texture, null is valid if your cell cannot be used in the ME Chest. refer to the assets for
	 *         examples and colors.
	 */
	@SideOnly(Side.CLIENT)
	IIcon getTopTexture();

	/**
	 * 
	 * Called when the storage cell is planed in an ME Chest and the user tries to open the terminal side, if your item
	 * is not available via ME Chests simply tell the user they can't use it, or something, other wise you should open
	 * your gui and display the cell to the user.
	 * 
	 * @param player
	 * @param chest
	 * @param cellHandler
	 * @param inv
	 * @param is
	 * @param chan
	 */
	void openChestGui(EntityPlayer player, IChestOrDrive chest, ICellHandler cellHandler, IMEInventoryHandler inv, ItemStack is, StorageChannel chan);

	/**
	 * 0 - cell is missing.
	 * 
	 * 1 - green, ( usually means available room for types or items. )
	 * 
	 * 2 - orange, ( usually means available room for items, but not types. )
	 * 
	 * 3 - red, ( usually means the cell is 100% full )
	 * 
	 * @param is
	 *            the cell item. ( use the handler for any details you can )
	 * @param the
	 *            handler for the cell is provides for reference, you can cast this to your handler.
	 * 
	 * @return get the status of the cell based on its contents.
	 */
	int getStatusForCell(ItemStack is, IMEInventory handler);

	/**
	 * @return the ae/t to drain for this storage cell inside a chest/drive.
	 */
	double cellIdleDrain(ItemStack is, IMEInventory handler);

}