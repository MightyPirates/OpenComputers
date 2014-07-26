/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.genetics;

import java.util.Collection;

/**
 * Individuals can be seeded either as hive drops or as mutation results.
 * 
 * {@link IAlleleRegistry} manages these.
 * 
 * @author SirSengir
 */
public interface IMutation {

	/**
	 * @return {@link ISpeciesRoot} this mutation is associated with.
	 */
	ISpeciesRoot getRoot();
	
	/**
	 * @return first of the alleles implementing IAlleleSpecies required for this mutation.
	 */
	IAllele getAllele0();

	/**
	 * @return second of the alleles implementing IAlleleSpecies required for this mutation.
	 */
	IAllele getAllele1();

	/**
	 * @return Array of {@link IAllele} representing the full default genome of the mutated side.
	 * 
	 *         Make sure to return a proper array for the species class. Returning an allele of the wrong type will cause cast errors on runtime.
	 */
	IAllele[] getTemplate();

	/**
	 * @return Unmodified base chance for mutation to fire.
	 */
	float getBaseChance();

	/**
	 * @return Collection of localized, human-readable strings describing special mutation conditions, if any. 
	 */
	Collection<String> getSpecialConditions();
	
	/**
	 * @param allele
	 * @return true if the passed allele is one of the alleles participating in this mutation.
	 */
	boolean isPartner(IAllele allele);

	/**
	 * @param allele
	 * @return the other allele which was not passed as argument.
	 */
	IAllele getPartner(IAllele allele);

	/**
	 * @return true if the mutation should not be displayed in the beealyzer.
	 */
	boolean isSecret();

}
