package forestry.api.apiculture;

import forestry.api.genetics.EnumTolerance;
import forestry.api.genetics.IFlowerProvider;
import forestry.api.genetics.IGenome;

/**
 * Only the default implementation is supported.
 * 
 * @author SirSengir
 * 
 */
public interface IBeeGenome extends IGenome {

	IAlleleBeeSpecies getPrimary();
	
	IAlleleBeeSpecies getSecondary();

	float getSpeed();

	int getLifespan();

	int getFertility();

	EnumTolerance getToleranceTemp();

	EnumTolerance getToleranceHumid();

	boolean getNocturnal();

	boolean getTolerantFlyer();

	boolean getCaveDwelling();

	IFlowerProvider getFlowerProvider();

	int getFlowering();

	int[] getTerritory();

	IAlleleBeeEffect getEffect();

}
