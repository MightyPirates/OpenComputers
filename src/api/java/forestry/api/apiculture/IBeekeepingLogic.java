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
