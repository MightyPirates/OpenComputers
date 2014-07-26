/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.core;

import net.minecraft.item.ItemStack;

public interface IGameMode {

	/**
	 * @return Human-readable identifier for the game mode. (i.e. 'EASY', 'NORMAL', 'HARD')
	 */
	String getIdentifier();

	/**
	 * @param ident Identifier for the setting. (See the gamemode config.)
	 * @return Value of the requested setting, false if unknown setting.
	 */
	boolean getBooleanSetting(String ident);
	
	/**
	 * @param ident Identifier for the setting. (See the gamemode config.)
	 * @return Value of the requested setting, 0 if unknown setting.
	 */
	int getIntegerSetting(String ident);

	/**
	 * @param ident Identifier for the setting. (See the gamemode config.)
	 * @return Value of the requested setting, 0 if unknown setting.
	 */
	float getFloatSetting(String ident);

	/**
	 * @param ident Identifier for the setting. (See the gamemode config.)
	 * @return Value of the requested setting, an itemstack containing an apple if unknown setting.
	 */
	ItemStack getStackSetting(String ident);

}
