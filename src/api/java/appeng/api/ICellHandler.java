package appeng.api;

import net.minecraft.item.ItemStack;
import appeng.api.me.util.IMEInventoryHandler;

/**
 * Registratio record for a Custom Cell handler.
 */
public interface ICellHandler {
	
	/**
	 * return true if the provided item is handled by your cell handler. ( AE May choose to skip this method, and just request a handler )
	 * @param is
	 * @return return true, if getCellHandler will not return null.
	 */
	boolean isCell( ItemStack is );
	
	/**
	 * return a new IMEHandler for the provided item.
	 * @param is
	 * @return if you cannot handle the provided item, return null.
	 */
	IMEInventoryHandler getCellHandler( ItemStack is );
	
}
