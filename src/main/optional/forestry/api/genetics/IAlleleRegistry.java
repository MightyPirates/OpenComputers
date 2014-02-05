package forestry.api.genetics;

import java.util.Collection;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import forestry.api.genetics.IClassification.EnumClassLevel;

/**
 * Manages {@link ISpeciesRoot}, {@link IAllele}s, {@link IFruitFamily}s, {@link IClassification}, the blacklist and allows creation of research notes.
 *
 * @author SirSengir
 */
public interface IAlleleRegistry {

	/* SPECIES ROOT CLASSES */
	/**
	 * Register a {@link ISpeciesRoot}.
	 * @param root {@link ISpeciesRoot} to register.
	 */
	void registerSpeciesRoot(ISpeciesRoot root);

	/**
	 * @return Map of all registered {@link ISpeciesRoot}.
	 */
	Map<String, ISpeciesRoot> getSpeciesRoot();

	/**
	 * Retrieve the {@link ISpeciesRoot} with the given uid.
	 * @param uid Unique id for the species class, i.e. "rootBees", "rootTrees", "rootButterflies".
	 * @return {@link ISpeciesRoot} if it exists, null otherwise.
	 */
	ISpeciesRoot getSpeciesRoot(String uid);
	
	/**
	 * Retrieve a matching {@link ISpeciesRoot} for the given itemstack.
	 * @param stack An itemstack possibly containing NBT data which can be converted by a species root.
	 * @return {@link ISpeciesRoot} if found, null otherwise.
	 */
	ISpeciesRoot getSpeciesRoot(ItemStack stack);
	
	/**
	 * Retrieve a matching {@link ISpeciesRoot} for the given {@link IIndividual}-class.
	 * @param clz Class extending {@link IIndividual}.
	 * @return {@link ISpeciesRoot} if found, null otherwise.
	 */
	ISpeciesRoot getSpeciesRoot(Class<? extends IIndividual> clz);
	
	/* INDIVIDUAL */
	/**
	 * Tests the itemstack for genetic information.
	 * 
	 * @param stack
	 * @return true if the itemstack is an individual.
	 */
	boolean isIndividual(ItemStack stack);

	/**
	 * Retrieve genetic information from an itemstack.
	 * 
	 * @param stack
	 *            Stack to retrieve genetic information for.
	 * @return IIndividual containing genetic information, null if none could be extracted.
	 */
	IIndividual getIndividual(ItemStack stack);

	/* ALLELES */

	/**
	 * @return HashMap of all currently registered alleles.
	 */
	Map<String, IAllele> getRegisteredAlleles();

	/**
	 * Registers an allele.
	 * 
	 * @param allele
	 *            IAllele to register.
	 */
	void registerAllele(IAllele allele);
	
	/**
	 * @return HashMap of all registered deprecated alleles and their corresponding replacements
	 */
	Map<String, IAllele> getDeprecatedAlleleReplacements();

	/**
	 * Registers an old allele UID and the new IAllele to replace instances of it with.
	 * 
	 * @param deprecatedAlleleUID
	 * 			the old allele's UID
	 * @param replacement
	 * 			the IAllele that the deprecated Allele will be replaced with.
	 */
	void registerDeprecatedAlleleReplacement(String deprecatedAlleleUID, IAllele replacement);

	/**
	 * Gets an allele
	 * 
	 * @param uid
	 *            String based unique identifier of the allele to retrieve.
	 * @return IAllele if found or a replacement is found in the Deprecated Allele map, null otherwise.
	 */
	IAllele getAllele(String uid);

	/* THIS SHOULD BE PHASED OUT */
	@Deprecated
	void reloadMetaMap(World world);

	@Deprecated
	IAllele getFromMetaMap(int meta);

	@Deprecated
	int getFromUIDMap(String uid);

	/* CLASSIFICATIONS */
	/**
	 * @return HashMap of all currently registered classifications.
	 */
	Map<String, IClassification> getRegisteredClassifications();

	/**
	 * Registers a classification.
	 * 
	 * @param classification
	 *            IClassification to register.
	 */
	void registerClassification(IClassification classification);

	/**
	 * Creates and returns a classification.
	 * 
	 * @param level
	 *            EnumClassLevel of the classification to create.
	 * @param uid
	 *            String based unique identifier. Implementation will throw an exception if the key is already taken.
	 * @param scientific
	 *            Binomial for the given classification.
	 * @return Created {@link IClassification} for easier chaining.
	 */
	IClassification createAndRegisterClassification(EnumClassLevel level, String uid, String scientific);

	/**
	 * Gets a classification.
	 * 
	 * @param uid
	 *            String based unique identifier of the classification to retrieve.
	 * @return {@link IClassification} if found, null otherwise.
	 */
	IClassification getClassification(String uid);

	/* FRUIT FAMILIES */
	/**
	 * Get all registered fruit families.
	 * 
	 * @return A map of registered fruit families and their UIDs.
	 */
	Map<String, IFruitFamily> getRegisteredFruitFamilies();

	/**
	 * Registers a new fruit family.
	 * 
	 * @param family
	 */
	void registerFruitFamily(IFruitFamily family);

	/**
	 * Retrieves a fruit family identified by uid.
	 * 
	 * @param uid
	 * @return {IFruitFamily} if found, false otherwise.
	 */
	IFruitFamily getFruitFamily(String uid);

	/* ALLELE HANDLERS */
	/**
	 * Registers a new IAlleleHandler
	 * 
	 * @param handler
	 *            IAlleleHandler to register.
	 */
	void registerAlleleHandler(IAlleleHandler handler);

	/* BLACKLIST */
	/**
	 * Blacklist an allele identified by its UID from mutation.
	 * 
	 * @param uid
	 *            UID of the allele to blacklist.
	 */
	void blacklistAllele(String uid);

	/**
	 * @return Current blacklisted alleles.
	 */
	Collection<String> getAlleleBlacklist();

	/**
	 * @param uid
	 *            UID of the species to vet.
	 * @return true if the allele is blacklisted.
	 */
	boolean isBlacklisted(String uid);

	/* RESEARCH */
	/**
	 * @param researcher Username of the player who researched this note.
	 * @param species {@link IAlleleSpecies} to encode on the research note.
	 * @return An itemstack containing a research note with the given species encoded onto it.
	 */
	ItemStack getSpeciesNoteStack(String researcher, IAlleleSpecies species);
	
	/**
	 * @param researcher Username of the player who researched this note.
	 * @param mutation {@link IMutation} to encode on the research note.
	 * @return An itemstack containing a research note with the given mutation encoded onto it.
	 */
	ItemStack getMutationNoteStack(String researcher, IMutation mutation);

}
