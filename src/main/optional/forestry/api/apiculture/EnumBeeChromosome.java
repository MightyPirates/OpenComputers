package forestry.api.apiculture;

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

/**
 * Enum representing the order of chromosomes in a bee's genome and what they control.
 * 
 * @author SirSengir
 */
public enum EnumBeeChromosome implements IChromosomeType {
	/**
	 * Species of the bee. Alleles here must implement {@link IAlleleBeeSpecies}.
	 */
	SPECIES(IAlleleBeeSpecies.class),
	/**
	 * (Production) Speed of the bee.
	 */
	SPEED(IAlleleFloat.class),
	/**
	 * Lifespan of the bee.
	 */
	LIFESPAN(IAlleleInteger.class),
	/**
	 * Fertility of the bee. Determines number of offspring.
	 */
	FERTILITY(IAlleleInteger.class),
	/**
	 * Temperature difference to its native supported one the bee can tolerate.
	 */
	TEMPERATURE_TOLERANCE(IAlleleTolerance.class),
	/**
	 * Slightly incorrectly named. If true, a naturally dirunal bee can work during the night. If true, a naturally nocturnal bee can work during the day.
	 */
	NOCTURNAL(IAlleleBoolean.class),
	/**
	 * Not used / superseded by fixed values for the species. Probably going to be replaced with a boolean for FIRE_RESIST.
	 */
	@Deprecated
	HUMIDITY(IAllele.class),
	/**
	 * Humidity difference to its native supported one the bee can tolerate.
	 */
	HUMIDITY_TOLERANCE(IAlleleTolerance.class),
	/**
	 * If true the bee can work during rain.
	 */
	TOLERANT_FLYER(IAlleleBoolean.class),
	/**
	 * If true, the bee can work without a clear view of the sky.
	 */
	CAVE_DWELLING(IAlleleBoolean.class),
	/**
	 * Contains the supported flower provider.
	 */
	FLOWER_PROVIDER(IAlleleFlowers.class),
	/**
	 * Determines pollination speed.
	 */
	FLOWERING(IAlleleInteger.class),
	/**
	 * Determines the size of the bee's territory.
	 */
	TERRITORY(IAlleleArea.class),
	/**
	 * Determines the bee's effect.
	 */
	EFFECT(IAlleleBeeEffect.class);
	
	Class<? extends IAllele> clss;
	
	EnumBeeChromosome(Class<? extends IAllele> clss) {
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
		return AlleleManager.alleleRegistry.getSpeciesRoot("rootBees");
	}
}
