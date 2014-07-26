/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.storage;

import net.minecraft.item.Item;

public interface IBackpackInterface {

	/**
	 * Adds a backpack with the given definition and type, returning the item.
	 * 
	 * @param definition
	 *            Definition of backpack behaviour.
	 * @param type
	 *            Type of backpack. (T1 or T2 (= Woven)
	 * @return Created backpack item.
	 */
	Item addBackpack(IBackpackDefinition definition, EnumBackpackType type);
}
