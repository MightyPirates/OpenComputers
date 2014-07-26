/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.arboriculture;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.mojang.authlib.GameProfile;

import forestry.api.genetics.IAllele;
import forestry.api.genetics.IChromosome;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.ISpeciesRoot;

public interface ITreeRoot extends ISpeciesRoot {

	@Override
	boolean isMember(ItemStack itemstack);

	@Override
	ITree getMember(ItemStack itemstack);

	@Override
	ITree getMember(NBTTagCompound compound);

	@Override
	ITreeGenome templateAsGenome(IAllele[] template);

	@Override
	ITreeGenome templateAsGenome(IAllele[] templateActive, IAllele[] templateInactive);

	/**
	 * @param world
	 * @return {@link IArboristTracker} associated with the passed world.
	 */
	@Override
	IArboristTracker getBreedingTracker(World world, GameProfile player);

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

	boolean plantSapling(World world, ITree tree, GameProfile owner, int x, int y, int z);

	boolean setLeaves(World world, IIndividual tree, GameProfile owner, int x, int y, int z);

	@Override
	IChromosome[] templateAsChromosomes(IAllele[] template);

	@Override
	IChromosome[] templateAsChromosomes(IAllele[] templateActive, IAllele[] templateInactive);

	boolean setFruitBlock(World world, IAlleleFruit allele, float sappiness, short[] indices, int x, int y, int z);

	/* GAME MODE */
	ArrayList<ITreekeepingMode> getTreekeepingModes();

	ITreekeepingMode getTreekeepingMode(World world);

	ITreekeepingMode getTreekeepingMode(String name);

	void registerTreekeepingMode(ITreekeepingMode mode);

	void setTreekeepingMode(World world, String name);

	/* TEMPLATES */
	@Override
	ArrayList<ITree> getIndividualTemplates();

	/* MUTATIONS */
	@Override
	Collection<ITreeMutation> getMutations(boolean shuffle);

}
