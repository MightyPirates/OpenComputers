package appeng.api.util;

import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Used to adjust settings on an object,
 * 
 * Obtained via {@link IConfigureableObject}
 */
public interface IConfigManager
{

	/**
	 * get a list of different settings
	 * 
	 * @return
	 */
	Set<Enum> getSettings();

	/**
	 * used to initialize the configuration manager, should be called for all settings.
	 * 
	 * @param settingName
	 * @param defaultValue
	 * @return
	 */
	void registerSetting(Enum settingName, Enum defaultValue);

	/**
	 * Get Value of a particlar setting
	 * 
	 * @param settingName
	 * @return
	 */
	Enum getSetting(Enum settingName);

	/**
	 * Change setting
	 * 
	 * @param settingName
	 * @param newValue
	 * @return
	 */
	Enum putSetting(Enum settingName, Enum newValue);

	/**
	 * write all settings to the NBT Tag so they can be read later.
	 * 
	 * @param dest
	 */
	void writeToNBT(NBTTagCompound dest);

	/**
	 * Only works after settings have been registered
	 * 
	 * @param src
	 */
	void readFromNBT(NBTTagCompound src);

}
