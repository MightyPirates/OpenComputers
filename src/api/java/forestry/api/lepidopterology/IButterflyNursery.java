package forestry.api.lepidopterology;

import forestry.api.genetics.IHousing;
import forestry.api.genetics.IIndividual;

public interface IButterflyNursery extends IHousing {
	
	IButterfly getCaterpillar();
	
	IIndividual getNanny();
	
	void setCaterpillar(IButterfly butterfly);
	
	boolean canNurse(IButterfly butterfly);
	
}
