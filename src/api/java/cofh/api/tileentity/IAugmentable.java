package cofh.api.tileentity;

import net.minecraft.item.ItemStack;

/**
 * Implemented on objects which support Augments.
 * 
 * @author King Lemming
 * 
 */
public interface IAugmentable {

	/**
	 * Attempt to reconfigure the tile based on the Augmentations present.
	 */
	void installAugments();

	/**
	 * Returns an array of the Augment slots for this Tile Entity.
	 */
	ItemStack[] getAugmentSlots();

	/**
	 * Returns a status array for the Augmentations installed in the Tile Entity.
	 */
	boolean[] getAugmentStatus();

}
