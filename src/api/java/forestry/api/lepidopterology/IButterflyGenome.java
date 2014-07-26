/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.lepidopterology;

import forestry.api.genetics.EnumTolerance;
import forestry.api.genetics.IFlowerProvider;
import forestry.api.genetics.IGenome;

public interface IButterflyGenome extends IGenome {
	
	IAlleleButterflySpecies getPrimary();

	IAlleleButterflySpecies getSecondary();

	float getSize();

	int getLifespan();

	int getMetabolism();
	
	int getFertility();

	float getSpeed();

	EnumTolerance getToleranceTemp();

	EnumTolerance getToleranceHumid();

	boolean getNocturnal();

	boolean getTolerantFlyer();

	boolean getFireResist();

	IFlowerProvider getFlowerProvider();

	IAlleleButterflyEffect getEffect();

}
