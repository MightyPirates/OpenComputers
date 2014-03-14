package appeng.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import appeng.api.me.util.IMEInventoryHandler;

/**
 * Registratio record for a Custom Cell handler.
 */
public interface IWirelessTermRegistery {
	
	/**
	 * add thsi handler to the list of other wireless handler.
	 * @param handler
	 */
	void registerWirelessHandler( IWirelessTermHandler handler );

	/**
	 * returns true if theres a handler for this item.
	 * @param is
	 * @return
	 */
	boolean isWirelessTerminal( ItemStack is );
	
	/**
	 * returns a register handler for the item in question, or null if there isn't one.
	 * @param is
	 * @return
	 */
	IWirelessTermHandler getWirelessTerminalHandler( ItemStack is );
	
	/**
	 * opens the wireless terminal gui, the wireless terminal item, must be in the active slot on the tool bar.
	 */
	void OpenWirelessTermainlGui( ItemStack item, World w, EntityPlayer player );
	
}
