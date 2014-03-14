package forestry.api.core;

import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.FMLLog;
import java.lang.reflect.Method;

/**
 * This is going away someday, use FML's GameRegistry instead.
 * @deprecated
 */
@Deprecated
public class ItemInterface {

	/**
	 * Get items here!
	 *
	 * Blocks currently not supported.
	 *
	 * @param ident
	 * @return ItemStack representing the item, null if not found.
	 */
	public static ItemStack getItem(String ident) {
		ItemStack item = null;

		try {
			String pack = ItemInterface.class.getPackage().getName();
			pack = pack.substring(0, pack.lastIndexOf('.'));
			String itemClass = pack.substring(0, pack.lastIndexOf('.')) + ".core.config.ForestryItem";
			Object[] enums = Class.forName(itemClass).getEnumConstants();
			for (Object e : enums) {
				if (e.toString().equals(ident)) {
					Method m = e.getClass().getMethod("getItemStack");
					return (ItemStack) m.invoke(e);
				}
			}
		} catch (Exception ex) {
			FMLLog.warning("Could not retrieve Forestry item identified by: " + ident);
		}

		return item;
	}
}
