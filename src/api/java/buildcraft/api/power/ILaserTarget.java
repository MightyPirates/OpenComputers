/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.power;

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
	void receiveLaserEnergy(double energy);

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
