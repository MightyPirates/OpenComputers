/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.farming;

import forestry.api.core.IStructureLogic;

public interface IFarmInterface {

	/**
	 * Creates {@link IStructureLogic} for use in farm components.
	 * 
	 * @param structure
	 *            {@link IFarmComponent} to create the logic for.
	 * @return {@link IStructureLogic} for use in farm components
	 */
	IStructureLogic createFarmStructureLogic(IFarmComponent structure);
}
