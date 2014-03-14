package forestry.api.apiculture;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import forestry.api.core.IStructureLogic;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.ISpeciesRoot;

public interface IBeeRoot extends ISpeciesRoot {
	
	/**
	 * @return true if passed item is a Forestry bee. Equal to getType(ItemStack stack) != EnumBeeType.NONE
	 */
	boolean isMember(ItemStack stack);
	
	/**
	 * @return {@link IBee} pattern parsed from the passed stack's nbt data.
	 */
	IBee getMember(ItemStack stack);

	IBee getMember(NBTTagCompound compound);

	/* GENOME CONVERSION */
	IBee templateAsIndividual(IAllele[] template);
	
	IBee templateAsIndividual(IAllele[] templateActive, IAllele[] templateInactive);
	
	IBeeGenome templateAsGenome(IAllele[] template);

	IBeeGenome templateAsGenome(IAllele[] templateActive, IAllele[] templateInactive);

	/* BREEDING TRACKER */
	/**
	 * @param world
	 * @return {@link IApiaristTracker} associated with the passed world.
	 */
	IApiaristTracker getBreedingTracker(World world, String player);

	/* BEE SPECIFIC */
	/**
	 * @return type of bee encoded on the itemstack. EnumBeeType.NONE if it isn't a bee.
	 */
	EnumBeeType getType(ItemStack stack);
	
	/**
	 * @return true if passed item is a drone. Equal to getType(ItemStack stack) == EnumBeeType.DRONE
	 */
	boolean isDrone(ItemStack stack);

	/**
	 * @return true if passed item is mated (i.e. a queen)
	 */
	boolean isMated(ItemStack stack);

	/**
	 * @param genome
	 *            Valid {@link IBeeGenome}
	 * @return {@link IBee} from the passed genome
	 */
	IBee getBee(World world, IBeeGenome genome);

	/**
	 * Creates an IBee suitable for a queen containing the necessary second genome for the mate.
	 * 
	 * @param genome
	 *            Valid {@link IBeeGenome}
	 * @param mate
	 *            Valid {@link IBee} representing the mate.
	 * @return Mated {@link IBee} from the passed genomes.
	 */
	IBee getBee(World world, IBeeGenome genome, IBee mate);
	
	/* TEMPLATES */
	ArrayList<IBee> getIndividualTemplates();

	/* MUTATIONS */
	Collection<IBeeMutation> getMutations(boolean shuffle);

	/* GAME MODE */
	void resetBeekeepingMode();
	
	ArrayList<IBeekeepingMode> getBeekeepingModes();

	IBeekeepingMode getBeekeepingMode(World world);

	IBeekeepingMode getBeekeepingMode(String name);

	void registerBeekeepingMode(IBeekeepingMode mode);

	void setBeekeepingMode(World world, String name);

	/* MISC */
	/**
	 * @param housing
	 *            Object implementing IBeeHousing.
	 * @return IBeekeepingLogic
	 */
	IBeekeepingLogic createBeekeepingLogic(IBeeHousing housing);

	/**
	 * TileEntities wanting to function as alveary components need to implement structure logic for validation.
	 * 
	 * @return IStructureLogic for alvearies.
	 */
	IStructureLogic createAlvearyStructureLogic(IAlvearyComponent structure);

}
