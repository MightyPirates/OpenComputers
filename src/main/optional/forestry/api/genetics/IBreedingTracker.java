package forestry.api.genetics;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import forestry.api.apiculture.IBeekeepingMode;

/**
 * Keeps track of who bred and/or discovered which species in a world.
 * 
 * @author SirSengir
 */
public interface IBreedingTracker {

	/**
	 * @return Name of the current {@link IBeekeepingMode}.
	 */
	String getModeName();

	/**
	 * Set the current {@link IBeekeepingMode}.
	 */
	void setModeName(String name);

	/**
	 * @return Amount of species discovered.
	 */
	int getSpeciesBred();

	/**
	 * Register the birth of an individual. Will mark it as discovered.
	 * 
	 * @param individual
	 */
	void registerBirth(IIndividual individual);

	/**
	 * Register the pickup of an individual.
	 * 
	 * @param individual
	 */
	void registerPickup(IIndividual individual);
	
	/**
	 * Marks a species as discovered. Should only be called from registerIndividual normally.
	 * 
	 * @param species
	 */
	void registerSpecies(IAlleleSpecies species);

	/**
	 * Register a successful mutation. Will mark it as discovered.
	 */
	@Deprecated
	void registerMutation(IAllele allele0, IAllele allele1);

	/**
	 * Register a successful mutation. Will mark it as discovered.
	 * 
	 * @param mutation
	 */
	void registerMutation(IMutation mutation);

	/**
	 * Queries the tracker for discovered species.
	 * 
	 * @param mutation
	 *            Mutation to query for.
	 * @return true if the mutation has been discovered.
	 */
	boolean isDiscovered(IMutation mutation);

	/**
	 * Queries the tracker for discovered species.
	 * 
	 * @param species
	 *            Species to check.
	 * @return true if the species has been bred.
	 */
	boolean isDiscovered(IAlleleSpecies species);

	/**
	 * Synchronizes the tracker to the client side. Should be called before opening any gui needing that information.
	 * 
	 * @param player
	 */
	void synchToPlayer(EntityPlayer player);

	/* LOADING & SAVING */
	void decodeFromNBT(NBTTagCompound nbttagcompound);

	void encodeToNBT(NBTTagCompound nbttagcompound);

}
