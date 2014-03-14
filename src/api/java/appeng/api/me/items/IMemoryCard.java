package appeng.api.me.items;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * used to interact with memory card items.
 */
public interface IMemoryCard {
	
	/**
	 * returns true if the item is in fact a memory card.
	 * @param is
	 * @return
	 */
	boolean isMemoryCard( ItemStack is );
	
	/**
	 * Configures the data stored on the memory card, the SettingsName, will be localized when displayed.
	 * @param is
	 * @param SettingsName
	 * @param data
	 *  - tooltip - is a unlocalized string displayed after the settings name.
	 */
	void setMemoryCardContents( ItemStack is, String SettingsName, NBTTagCompound data );
	
	/**
	 * returns the settings name provided previously by setMemoryCardContents, or "AppEng.GuiITooltip.Blank" if there is no settings.
	 * @param is
	 * @return
	 */
	String getSettingsName( ItemStack is );
	
	/**
	 * returns the NBT Data previously saved by setMemoryCardContents, or an empty NBTCompound
	 * @param is
	 * @return
	 */
	NBTTagCompound getData( ItemStack is );
	
}
