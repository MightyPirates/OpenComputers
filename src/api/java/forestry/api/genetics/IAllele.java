package forestry.api.genetics;

/**
 * An {@link IIndividual}'s {@link IGenome} is composed of {@link IChromosome}s consisting each of a primary and secondary {@link IAllele}.
 * 
 * {@link IAllele}s hold all information regarding an {@link IIndividual}'s traits, from species to size, temperature tolerances, etc.
 * 
 * Should be extended for different types of alleles. ISpeciesAllele, IBiomeAllele, etc.
 * 
 * @author SirSengir
 */
public interface IAllele {

	/**
	 * @return A unique string identifier for this allele.
	 */
	String getUID();

	/**
	 * @return true if the allele is dominant, false otherwise.
	 */
	boolean isDominant();
	
	/**
	 * @return Localized short, human-readable identifier used in tooltips and beealyzer.
	 */
	String getName();
	
}
