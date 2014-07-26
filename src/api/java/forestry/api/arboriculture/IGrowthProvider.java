/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.arboriculture;

import net.minecraft.world.World;

public interface IGrowthProvider {

	/**
	 * Check to see whether a sapling at the given location with the given genome can grow into a tree.
	 * 
	 * @param genome Genome of the tree this is called for.
	 * @param world Minecraft world the tree will inhabit.
	 * @param xPos x-Coordinate to attempt growth at.
	 * @param yPos y-Coordinate to attempt growth at.
	 * @param zPos z-Coordinate to attempt growth at.
	 * @param expectedGirth Trunk size of the tree to generate.
	 * @param expectedHeight Height of the tree to generate.
	 * @return true if the tree can grow at the given coordinates, false otherwise.
	 */
	boolean canGrow(ITreeGenome genome, World world, int xPos, int yPos, int zPos, int expectedGirth, int expectedHeight);

	EnumGrowthConditions getGrowthConditions(ITreeGenome genome, World world, int xPos, int yPos, int zPos);

	/**
	 * @return Short, human-readable identifier used in the treealyzer.
	 */
	String getDescription();

	/**
	 * @return Detailed description of growth behaviour used in the treealyzer.
	 */
	String[] getInfo();

}
