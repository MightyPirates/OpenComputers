/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.core;

public interface IStructureLogic extends INBTTagable {

	/**
	 * @return String unique to the type of structure controlled by this structure logic.
	 */
	String getTypeUID();

	/**
	 * Called by {@link ITileStructure}'s validateStructure().
	 */
	void validateStructure();

}
