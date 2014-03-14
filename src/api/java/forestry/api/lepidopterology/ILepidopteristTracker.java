package forestry.api.lepidopterology;

import forestry.api.genetics.IBreedingTracker;

public interface ILepidopteristTracker extends IBreedingTracker {

	void registerCatch(IButterfly butterfly);
	
}
