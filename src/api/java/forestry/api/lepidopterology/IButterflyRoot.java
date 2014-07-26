/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.lepidopterology;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.mojang.authlib.GameProfile;

import forestry.api.genetics.IAllele;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.ISpeciesRoot;

public interface IButterflyRoot extends ISpeciesRoot {

	@Override
	boolean isMember(ItemStack stack);

	@Override
	IButterfly getMember(ItemStack stack);

	@Override
	IButterfly getMember(NBTTagCompound compound);

	@Override
	ItemStack getMemberStack(IIndividual butterfly, int type);

	/* GENOME CONVERSION */
	@Override
	IButterfly templateAsIndividual(IAllele[] template);

	@Override
	IButterfly templateAsIndividual(IAllele[] templateActive, IAllele[] templateInactive);

	@Override
	IButterflyGenome templateAsGenome(IAllele[] template);

	@Override
	IButterflyGenome templateAsGenome(IAllele[] templateActive, IAllele[] templateInactive);

	/* BUTTERFLY SPECIFIC */
	ILepidopteristTracker getBreedingTracker(World world, GameProfile player);

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
	@Override
	ArrayList<IButterfly> getIndividualTemplates();

	/* MUTATIONS */
	@Override
	Collection<IButterflyMutation> getMutations(boolean shuffle);

}
