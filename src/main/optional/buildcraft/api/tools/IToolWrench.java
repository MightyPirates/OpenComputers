package buildcraft.api.tools;

import net.minecraft.entity.player.EntityPlayer;

/***
 * Implement this interface on subclasses of Item to have that item work as a wrench for buildcraft
 */
public interface IToolWrench {

	/***
	 * Called to ensure that the wrench can be used. To get the ItemStack that is used, check player.inventory.getCurrentItem()
	 * 
	 * @param player
	 *            - The player doing the wrenching
	 * @param x
	 *            ,y,z - The coordinates for the block being wrenched
	 * 
	 * @return true if wrenching is allowed, false if not
	 */
	public boolean canWrench(EntityPlayer player, int x, int y, int z);

	/***
	 * Callback after the wrench has been used. This can be used to decrease durability or for other purposes. To get the ItemStack that was used, check
	 * player.inventory.getCurrentItem()
	 * 
	 * @param player
	 *            - The player doing the wrenching
	 * @param x
	 *            ,y,z - The coordinates of the block being wrenched
	 */
	public void wrenchUsed(EntityPlayer player, int x, int y, int z);
}
