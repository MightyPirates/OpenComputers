package appeng.api.implementations.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import appeng.api.implementations.TransitionResult;
import appeng.api.util.WorldCoord;

/**
 * Implemented on a {@link Item}
 */
public interface ISpatialStorageCell
{

	/**
	 * @param is
	 * @return true if this item is a spatial storage cell
	 */
	boolean isSpatialStorage(ItemStack is);

	/**
	 * @param is
	 * @return the maximum size of the spatial storage cell along any given axis
	 */
	int getMaxStoredDim(ItemStack is);

	/**
	 * @param is
	 * @return the world for this cell
	 */
	World getWorld(ItemStack is);

	/**
	 * get the currently stored size.
	 * 
	 * @param is
	 * @return
	 */
	WorldCoord getStoredSize(ItemStack is);

	/**
	 * Minimum coordinates in its world for the storage cell.
	 * 
	 * @param is
	 * @return
	 */
	WorldCoord getMin(ItemStack is);

	/**
	 * Maximum coordinates in its world for the storage cell.
	 * 
	 * @param is
	 * @return
	 */
	WorldCoord getMax(ItemStack is);

	/**
	 * Perform a spatial swap with the contents of the cell, and the world.
	 * 
	 * @param is
	 * @param w
	 * @param min
	 * @param max
	 * @param doTransition
	 * @return
	 */
	TransitionResult doSpatialTransition(ItemStack is, World w, WorldCoord min, WorldCoord max, boolean doTransition);

}