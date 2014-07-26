/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.genetics;

import net.minecraft.world.World;

public interface IIndividualLiving extends IIndividual {

	/**
	 * @return Genetic information of the mate, null if unmated.
	 */
	IGenome getMate();

	/**
	 * @return Current health of the individual.
	 */
	int getHealth();

	/**
	 * @return Maximum health of the individual.
	 */
	int getMaxHealth();

	/**
	 * Age the individual.
	 * @param world
	 * @param ageModifier
	 */
	void age(World world, float ageModifier);

	/**
	 * Mate with the given individual.
	 * @param individual the {@link IIndividual} to mate this one with.
	 */
	void mate(IIndividual individual);

	/**
	 * @return true if the individual is among the living.
	 */
	boolean isAlive();

}
