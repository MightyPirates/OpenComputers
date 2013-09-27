package ic2.api.tile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Allows a tile entity to make use of the wrench's removal and rotation functions.
 */
public interface IWrenchable {
	/**
	 * Determine if the wrench can be used to set the block's facing.
	 * Called before wrenchCanRemove().
	 * 
	 * @param entityPlayer player using the wrench, may be null
	 * @param side block's side the wrench was clicked on
	 * @return Whether the wrenching was done and the wrench should be damaged
	 */
	boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int side);

	/**
	 * Get the block's facing.
	 * 
	 * @return Block facing
	 */
	short getFacing();

	/**
	 * Set the block's facing
	 * 
	 * @param facing facing to set the block to
	 */
	void setFacing(short facing);

	/**
	 * Determine if the wrench can be used to remove the block.
	 * Called if wrenchSetFacing fails.
	 *
	 * @param entityPlayer player using the wrench, may be null
	 * @return Whether the wrenching was done and the wrench should be damaged
	 */
	boolean wrenchCanRemove(EntityPlayer entityPlayer);

	/**
	 * Determine the probability to drop the block as it is.
	 * The first entry in getBlockDropped will be replaced by blockid:meta if the drop is successful.
	 * 
	 * @return Probability from 0 to 1
	 */
	float getWrenchDropRate();

	/**
	 * Determine the item the block will drop when the wrenching is successful.
	 * 
	 * @param entityPlayer player using the wrench, may be null
	 * @return Item to drop, may be null
	 */
	ItemStack getWrenchDrop(EntityPlayer entityPlayer);
}

