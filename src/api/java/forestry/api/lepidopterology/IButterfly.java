/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.lepidopterology;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import forestry.api.genetics.IIndividualLiving;

public interface IButterfly extends IIndividualLiving {

	IButterflyGenome getGenome();

	/**
	 * @return Genetic information of the mate, null if unmated.
	 */
	IButterflyGenome getMate();

	/**
	 * @return Physical size of the butterfly.
	 */
	float getSize();
	
	/**
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @return true if the butterfly can naturally spawn at the given location at this time. (Used to auto-spawn butterflies from tree leaves.)
	 */
	boolean canSpawn(World world, double x, double y, double z);

	/**
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @return true if the butterfly can take flight at the given location at this time. (Used to auto-spawn butterflies from dropped items.)
	 */
	boolean canTakeFlight(World world, double x, double y, double z);

	/**
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @return true if the environment (temperature, humidity) is valid for the butterfly at the given location.
	 */
	boolean isAcceptedEnvironment(World world, double x, double y, double z);

	IButterfly spawnCaterpillar(IButterflyNursery nursery);
	
	/**
	 * @param entity
	 * @param playerKill Whether or not the butterfly was killed by a player.
	 * @param lootLevel Loot level according to the weapon used to kill the butterfly.
	 * @return Array of itemstacks to drop on death of the given entity.
	 */
	ItemStack[] getLootDrop(IEntityButterfly entity, boolean playerKill, int lootLevel);

	/**
	 * @param nursery
	 * @param playerKill Whether or not the nursery was broken by a player.
	 * @param lootLevel Fortune level.
	 * @return Array of itemstacks to drop on breaking of the nursery.
	 */
	ItemStack[] getCaterpillarDrop(IButterflyNursery nursery, boolean playerKill, int lootLevel);
	
	/**
	 * Create an exact copy of this butterfly.
	 */
	IButterfly copy();

}
