package forestry.api.storage;

import java.util.Collection;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface IBackpackDefinition {

	/**
	 * @return A unique string identifier
	 */
	String getKey();

	/**
	 * @return Human-readable name of the backpack.
	 */
	String getName();

	/**
	 * @return Primary colour for the backpack icon.
	 */
	int getPrimaryColour();

	/**
	 * @return Secondary colour for backpack icon.
	 */
	int getSecondaryColour();

	/**
	 * Adds an item as valid for this backpack.
	 * 
	 * @param validItem
	 */
	void addValidItem(ItemStack validItem);

	/**
	 * Returns an arraylist of all items valid for this backpack type.
	 * 
	 * @param player
	 * @return Collection of itemstack which are valid items for this backpack type. May be empty or null and does not necessarily include all valid items.
	 */
	Collection<ItemStack> getValidItems(EntityPlayer player);

	/**
	 * Returns true if the itemstack is a valid item for this backpack type.
	 * 
	 * @param player
	 * @param itemstack
	 * @return true if the given itemstack is valid for this backpack, false otherwise.
	 */
	boolean isValidItem(EntityPlayer player, ItemStack itemstack);

}