package appeng.api.implementations.items;

import java.util.EnumSet;

import net.minecraft.item.ItemStack;
import appeng.api.config.SecurityPermissions;

public interface IBiometricCard
{

	/**
	 * Set the username to blank, to clear it.
	 */
	void setUsername(ItemStack itemStack, String username);

	/**
	 * @return username of the player encoded on this card, or a blank string.
	 */
	String getUsername(ItemStack is);

	/**
	 * @param itemStack
	 * @return the full list of permissions encoded on the card.
	 */
	EnumSet<SecurityPermissions> getPermissions(ItemStack itemStack);

	/**
	 * Check if a permission is encoded on the card.
	 * 
	 * @param permission
	 * @return true if this permission is set on the card.
	 */
	boolean hasPermission(ItemStack is, SecurityPermissions permission);

	/**
	 * remove a permission from the item stack.
	 * 
	 * @param itemStack
	 * @param permission
	 */
	void removePermission(ItemStack itemStack, SecurityPermissions permission);

	/**
	 * add a permission to the item stack.
	 * 
	 * @param itemStack
	 * @param permission
	 */
	void addPermission(ItemStack itemStack, SecurityPermissions permission);

}
