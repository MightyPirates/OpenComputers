/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.apiculture;

import forestry.api.core.INBTTagable;
import forestry.api.genetics.IEffectData;

public interface IBeekeepingLogic extends INBTTagable {

	/* STATE INFORMATION */
	int getBreedingTime();

	int getTotalBreedingTime();

	IBee getQueen();

	IBeeHousing getHousing();
	
	IEffectData[] getEffectData();

	/* UPDATING */
	void update();

}
