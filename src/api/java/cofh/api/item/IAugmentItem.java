package cofh.api.item;

import java.util.Set;

import net.minecraft.item.ItemStack;

public interface IAugmentItem {

	/**
	 * Get the augmentation level for a given Augment and Augment Type.
	 * 
	 * @param stack
	 *            ItemStack representing the Augment.
	 * @param type
	 *            String containing the Augment type name.
	 * @return The Augment level of the stack for the requested type - 0 if it does not affect that attribute.
	 */
	int getAugmentLevel(ItemStack stack, String type);

	/**
	 * Get the Augment Types for a given Augment. Set ensure that there are no duplicates.
	 * 
	 * @param stack
	 *            ItemStack representing the Augment.
	 * @return Set of the Augmentation Types. Should return an empty set if there are none (but this would be really stupid to make). DO NOT RETURN NULL.
	 */
	Set<String> getAugmentTypes(ItemStack stack);

}
