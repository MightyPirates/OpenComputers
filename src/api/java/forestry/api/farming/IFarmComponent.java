/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.farming;

import forestry.api.core.ITileStructure;

public interface IFarmComponent extends ITileStructure {

	boolean hasFunction();

	void registerListener(IFarmListener listener);

	void removeListener(IFarmListener listener);
}
