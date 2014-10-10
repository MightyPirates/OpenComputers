package cofh.api.inventory;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Implement this interface on TileEntities which should handle items.
 * 
 * A reference implementation is provided {@link TileInventoryHandler}.
 * 
 * @author King Lemming
 * 
 */
public interface IInventoryHandler extends IInventoryConnection {

	/**
	 * Insert an ItemStack into the IInventoryHandler, internal distribution is left entirely to the IInventoryHandler. This returns what is remaining of the
	 * original stack - a null return means that the entire stack was accepted!
	 * 
	 * @param from
	 *            Orientation the item is inserted from.
	 * @param item
	 *            ItemStack to be inserted. The size of this stack corresponds to the maximum amount to insert.
	 * @param simulate
	 *            If TRUE, the insertion will only be simulated.
	 * @return An ItemStack representing how much is remaining after the item was inserted (or would have been, if simulated) into the container inventory.
	 */
	ItemStack insertItem(ForgeDirection from, ItemStack item, boolean simulate);

	/**
	 * Extract an ItemStack from an IInventoryHandler, internal distribution is left entirely to the IInventoryHandler. This returns the resulting stack - a
	 * null return means that nothing was extracted!
	 * 
	 * @param from
	 *            Orientation the item is extracted from.
	 * @param item
	 *            ItemStack to be extracted. The size of this stack corresponds to the maximum amount to extract. If this is null, then a null ItemStack should
	 *            immediately be returned.
	 * @param simulate
	 *            If TRUE, the extraction will only be simulated.
	 * @return An ItemStack representing how much was extracted (or would have been, if simulated) from the container inventory.
	 */
	ItemStack extractItem(ForgeDirection from, ItemStack item, boolean simulate);

	/**
	 * Extract an ItemStack from an IInventoryHandler, internal distribution is left entirely to the IInventoryHandler. This returns the resulting stack - a
	 * null return means that nothing was extracted!
	 * 
	 * @param from
	 *            Orientation the item is extracted from.
	 * @param maxExtract
	 *            Maximum number of items to extract. (The returned ItemStack should have a stackSize no higher than this.)
	 * @param simulate
	 *            If TRUE, the extraction will only be simulated.
	 * @return An ItemStack representing how much was extracted (or would have been, if simulated) from the container inventory.
	 */
	ItemStack extractItem(ForgeDirection from, int maxExtract, boolean simulate);

	/**
	 * Get the contents of the IInventoryHandler's inventory. This returns a COPY. This should only return non-null ItemStacks, and an empty List if the
	 * inventory has nothing.
	 */
	List<ItemStack> getInventoryContents(ForgeDirection from);

	/**
	 * Get the size (number of internal slots) of the IInventoryHandler's inventory.
	 */
	int getSizeInventory(ForgeDirection from);

	/**
	 * Returns whether or not the IInventoryHandler's inventory is empty (for a given side).
	 */
	boolean isEmpty(ForgeDirection from);

	/**
	 * Returns whether or not the IInventoryHandler's inventory is full (for a given side).
	 */
	boolean isFull(ForgeDirection from);

}
