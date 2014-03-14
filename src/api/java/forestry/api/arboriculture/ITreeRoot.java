package forestry.api.arboriculture;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IChromosome;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.ISpeciesRoot;

public interface ITreeRoot extends ISpeciesRoot {
	
	boolean isMember(ItemStack itemstack);

	ITree getMember(ItemStack itemstack);

	ITree getMember(NBTTagCompound compound);

	ITreeGenome templateAsGenome(IAllele[] template);

	ITreeGenome templateAsGenome(IAllele[] templateActive, IAllele[] templateInactive);

	/**
	 * @param world
	 * @return {@link IArboristTracker} associated with the passed world.
	 */
	IArboristTracker getBreedingTracker(World world, String player);

	/* TREE SPECIFIC */
	/**
	 * Register a leaf tick handler.
	 * @param handler the {@link ILeafTickHandler} to register.
	 */
	void registerLeafTickHandler(ILeafTickHandler handler);
	
	Collection<ILeafTickHandler> getLeafTickHandlers();
	
	/**
	 * @return type of tree encoded on the itemstack. EnumBeeType.NONE if it isn't a tree.
	 */
	EnumGermlingType getType(ItemStack stack);

	ITree getTree(World world, int x, int y, int z);

	ITree getTree(World world, ITreeGenome genome);

	boolean plantSapling(World world, ITree tree, String owner, int x, int y, int z);

	boolean setLeaves(World world, IIndividual tree, String owner, int x, int y, int z);

	IChromosome[] templateAsChromosomes(IAllele[] template);

	IChromosome[] templateAsChromosomes(IAllele[] templateActive, IAllele[] templateInactive);

	boolean setFruitBlock(World world, IAlleleFruit allele, float sappiness, short[] indices, int x, int y, int z);

	/* GAME MODE */
	ArrayList<ITreekeepingMode> getTreekeepingModes();

	ITreekeepingMode getTreekeepingMode(World world);

	ITreekeepingMode getTreekeepingMode(String name);

	void registerTreekeepingMode(ITreekeepingMode mode);

	void setTreekeepingMode(World world, String name);
	
	/* TEMPLATES */
	ArrayList<ITree> getIndividualTemplates();

	/* MUTATIONS */
	Collection<ITreeMutation> getMutations(boolean shuffle);

}
