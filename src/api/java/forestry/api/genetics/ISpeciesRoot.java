package forestry.api.genetics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Describes a class of species (i.e. bees, trees, butterflies), provides helper functions and access to common functionality. 
 */
public interface ISpeciesRoot {
	
	/**
	 * @return A unique identifier for the species class. Should consist of "root" + a common name for the species class in camel-case, i.e. "rootBees", "rootTrees", "rootButterflies".
	 */
	String getUID();

	/**
	 * @return Class of the sub-interface inheriting from {@link IIndividual}. 
	 */
	Class getMemberClass();
	
	/**
	 * @return Integer denoting the number of (counted) species of this type in the world.
	 */
	int getSpeciesCount();

	/**
	 * Used to check whether a given itemstack contains genetic data corresponding to an {@link IIndividual} of this class.
	 * @param stack itemstack to check.
	 * @return true if the itemstack contains an {@link IIndividual} of this class, false otherwise.
	 */
	boolean isMember(ItemStack stack);

	/**
	 * Used to check whether a given itemstack contains genetic data corresponding to an {@link IIndividual} of this class and matches the given type.
	 * @param stack itemstack to check.
	 * @param type Integer denoting the type needed to match. (i.e. butterfly vs. butterfly serum; bee queens, princesses, drones; etc.)
	 * @return true if the itemstack contains an {@link IIndividual} of this class, false otherwise.
	 */
	boolean isMember(ItemStack stack, int type);
	
	/**
	 * Used to check whether the given {@link IIndividual} is member of this class.
	 * @param individual {@link IIndividual} to check.
	 * @return true if the individual is member of this class, false otherwise.
	 */
	boolean isMember(IIndividual individual);
	
	IIndividual getMember(ItemStack stack);
	
	IIndividual getMember(NBTTagCompound compound);

	ItemStack getMemberStack(IIndividual individual, int type);

	/* BREEDING TRACKER */
	IBreedingTracker getBreedingTracker(World world, String player);
	
	/* GENOME MANIPULATION */
	IIndividual templateAsIndividual(IAllele[] template);
	
	IIndividual templateAsIndividual(IAllele[] templateActive, IAllele[] templateInactive);
	
	IChromosome[] templateAsChromosomes(IAllele[] template);

	IChromosome[] templateAsChromosomes(IAllele[] templateActive, IAllele[] templateInactive);

	IGenome templateAsGenome(IAllele[] template);

	IGenome templateAsGenome(IAllele[] templateActive, IAllele[] templateInactive);

	/* TEMPLATES */
	/**
	 * Registers a bee template using the UID of the first allele as identifier.
	 * 
	 * @param template
	 */
	void registerTemplate(IAllele[] template);

	/**
	 * Registers a bee template using the passed identifier.
	 * 
	 * @param template
	 */
	void registerTemplate(String identifier, IAllele[] template);

	/**
	 * Retrieves a registered template using the passed identifier.
	 * 
	 * @param identifier
	 * @return Array of {@link IAllele} representing a genome.
	 */
	IAllele[] getTemplate(String identifier);

	/**
	 * @return Default individual template for use when stuff breaks.
	 */
	IAllele[] getDefaultTemplate();

	/**
	 * @param rand Random to use.
	 * @return A random template from the pool of registered species templates.
	 */
	IAllele[] getRandomTemplate(Random rand);

	Map<String, IAllele[]> getGenomeTemplates();
	ArrayList<? extends IIndividual> getIndividualTemplates();

	/* MUTATIONS */
	/**
	 * Use to register mutations.
	 * 
	 * @param mutation
	 */
	void registerMutation(IMutation mutation);

	/**
	 * @return All registered mutations.
	 */
	Collection<? extends IMutation> getMutations(boolean shuffle);

	/**
	 * @param other Allele to match mutations against.
	 * @return All registered mutations the given allele is part of.
	 */
	Collection<? extends IMutation> getCombinations(IAllele other);
	
	/**
	 * @param result {@link IAllele} to search for.
	 * @return All registered mutations the given {@link IAllele} is the result of.
	 */
	Collection<? extends IMutation> getPaths(IAllele result, int chromosomeOrdinal);
	
	/* RESEARCH */
	/**
	 * @return List of generic catalysts which should be accepted for research by species of this class.
	 */
	Map<ItemStack, Float> getResearchCatalysts();
	
	/**
	 * Sets an item stack as a valid (generic) research catalyst for this class.
	 * @param itemstack ItemStack to set as suitable.
	 * @param suitability Float between 0 and 1 to indicate suitability.
	 */
	void setResearchSuitability(ItemStack itemstack, float suitability);
	
	/**
	 * @return Array of {@link IChromosomeType} which are in this species genome
	 */
	IChromosomeType[] getKaryotype();

	/**
	 * @return {@link IChromosomeType} which is the "key" for this species class, usually the species chromosome.  
	 */
	IChromosomeType getKaryotypeKey();
}
