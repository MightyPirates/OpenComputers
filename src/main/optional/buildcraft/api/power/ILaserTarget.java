/*
 * Copyright (c) SpaceToad, 2011-2012
 * http://www.mod-buildcraft.com
 * 
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.power;

/**
 * Specifies a Tile Entity that can receive power via laser beam.
 *
 * @author cpw
 */
public interface ILaserTarget {

	/**
	 * Returns true if the target currently needs power. For example, if the Advanced
	 * Crafting Table has work to do.
	 *
	 * @return true if needs power
	 */
	boolean requiresLaserEnergy();

	/**
	 * Transfers energy from the laser to the target.
	 *
	 * @param energy
	 */
	void receiveLaserEnergy(float energy);

	/**
	 * Return true if the Tile Entity object is no longer a valid target. For
	 * example, if its been invalidated.
	 *
	 * @return true if no longer a valid target object
	 */
	boolean isInvalidTarget();

	int getXCoord();

	int getYCoord();

	int getZCoord();
}
