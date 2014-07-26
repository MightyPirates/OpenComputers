/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.lepidopterology;

import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleArea;
import forestry.api.genetics.IAlleleBoolean;
import forestry.api.genetics.IAlleleFloat;
import forestry.api.genetics.IAlleleFlowers;
import forestry.api.genetics.IAlleleInteger;
import forestry.api.genetics.IAlleleTolerance;
import forestry.api.genetics.IChromosomeType;
import forestry.api.genetics.ISpeciesRoot;

public enum EnumButterflyChromosome implements IChromosomeType {
	/**
	 * Species of the bee. Alleles here must implement {@link IAlleleButterflySpecies}.
	 */
	SPECIES(IAlleleButterflySpecies.class),
	/**
	 * Physical size.
	 */
	SIZE(IAlleleFloat.class),
	/**
	 * Flight speed.
	 */
	SPEED(IAlleleFloat.class),
	/**
	 * How long the butterfly can last without access to matching pollinatables.
	 */
	LIFESPAN(IAlleleInteger.class),
	/**
	 * Species with a higher metabolism have a higher appetite and may cause more damage to their environment.
	 */
	METABOLISM(IAlleleInteger.class),
	/**
	 * Determines likelyhood of caterpillars and length of caterpillar/pupation phase. Also: Number of max caterpillars after mating?
	 */
	FERTILITY(IAlleleInteger.class),
	/**
	 * Not sure yet.
	 */
	TEMPERATURE_TOLERANCE(IAlleleTolerance.class),
	/**
	 * Not sure yet.
	 */
	HUMIDITY_TOLERANCE(IAlleleTolerance.class),
	/**
	 * Only nocturnal butterflys/moths will fly at night. Allows daylight activity for naturally nocturnal species.
	 */
	NOCTURNAL(IAlleleBoolean.class),
	/**
	 * Only tolerant flyers will fly in the rain.
	 */
	TOLERANT_FLYER(IAlleleBoolean.class),
	/**
	 * Fire resistance.
	 */
	FIRE_RESIST(IAlleleBoolean.class),
	/**
	 * Required flowers/leaves.
	 */
	FLOWER_PROVIDER(IAlleleFlowers.class),
	/**
	 * Extra effect to surroundings. (?)
	 */
	EFFECT(IAlleleButterflyEffect.class),
	/**
	 * Not used yet
	 */
	TERRITORY(IAlleleArea.class),
	;
	
	Class<? extends IAllele> clss;
	
	EnumButterflyChromosome(Class<? extends IAllele> clss) {
		this.clss = clss;
	}

	@Override
	public Class<? extends IAllele> getAlleleClass() {
		return clss;
	}

	@Override
	public String getName() {
		return this.toString().toLowerCase();
	}

	@Override
	public ISpeciesRoot getSpeciesRoot() {
		return AlleleManager.alleleRegistry.getSpeciesRoot("rootButterflies");
	}
}
