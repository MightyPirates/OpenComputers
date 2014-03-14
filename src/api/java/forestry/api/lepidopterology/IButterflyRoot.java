package forestry.api.lepidopterology;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.ISpeciesRoot;

public interface IButterflyRoot extends ISpeciesRoot {

	boolean isMember(ItemStack stack);

	IButterfly getMember(ItemStack stack);

	IButterfly getMember(NBTTagCompound compound);
	
	ItemStack getMemberStack(IIndividual butterfly, int type);

	/* GENOME CONVERSION */
	IButterfly templateAsIndividual(IAllele[] template);
	
	IButterfly templateAsIndividual(IAllele[] templateActive, IAllele[] templateInactive);
	
	IButterflyGenome templateAsGenome(IAllele[] template);

	IButterflyGenome templateAsGenome(IAllele[] templateActive, IAllele[] templateInactive);

	/* BUTTERFLY SPECIFIC */
	ILepidopteristTracker getBreedingTracker(World world, String player);

	/**
	 * Spawns the given butterfly in the world.
	 * @param butterfly
	 * @return butterfly entity on success, null otherwise.
	 */
	EntityLiving spawnButterflyInWorld(World world, IButterfly butterfly, double x, double y, double z);

	/**
	 * @return true if passed item is mated.
	 */
	boolean isMated(ItemStack stack);

	/* TEMPLATES */
	ArrayList<IButterfly> getIndividualTemplates();

	/* MUTATIONS */
	Collection<IButterflyMutation> getMutations(boolean shuffle);

}
