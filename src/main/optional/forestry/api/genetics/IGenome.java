package forestry.api.genetics;

import forestry.api.core.INBTTagable;

/**
 * Holds the {@link IChromosome}s which comprise the traits of a given individual.
 * 
 * Only the default implementation is supported.
 */
public interface IGenome extends INBTTagable {

	IAlleleSpecies getPrimary();

	IAlleleSpecies getSecondary();

	IChromosome[] getChromosomes();

	IAllele getActiveAllele(int chromosome);

	IAllele getInactiveAllele(int chromosome);

	boolean isGeneticEqual(IGenome other);
	
	ISpeciesRoot getSpeciesRoot();
}
