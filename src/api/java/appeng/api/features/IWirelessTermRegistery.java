package appeng.api.features;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Registration record for a Custom Cell handler.
 */
public interface IWirelessTermRegistery
{

	/**
	 * add this handler to the list of other wireless handler.
	 * 
	 * @param handler
	 */
	void registerWirelessHandler(IWirelessTermHandler handler);

	/**
	 * @param is
	 * @return true if there is a handler for this item
	 */
	boolean isWirelessTerminal(ItemStack is);

	/**
	 * @param is
	 * @return a register handler for the item in question, or null if there
	 *         isn't one
	 */
	IWirelessTermHandler getWirelessTerminalHandler(ItemStack is);

	/**
	 * opens the wireless terminal gui, the wireless terminal item, must be in
	 * the active slot on the tool bar.
	 */
	void OpenWirelessTermainlGui(ItemStack item, World w, EntityPlayer player);

}