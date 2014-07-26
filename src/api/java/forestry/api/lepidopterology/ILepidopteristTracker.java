/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.lepidopterology;

import forestry.api.genetics.IBreedingTracker;

public interface ILepidopteristTracker extends IBreedingTracker {

	void registerCatch(IButterfly butterfly);
	
}
