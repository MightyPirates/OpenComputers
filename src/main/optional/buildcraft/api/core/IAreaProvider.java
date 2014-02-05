/** 
 * Copyright (c) SpaceToad, 2011
 * http://www.mod-buildcraft.com
 * 
 * BuildCraft is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package buildcraft.api.core;

/**
 * To be implemented by TileEntities able to provide a square area on the world, typically BuildCraft markers.
 */
public interface IAreaProvider {

	public int xMin();

	public int yMin();

	public int zMin();

	public int xMax();

	public int yMax();

	public int zMax();

	/**
	 * Remove from the world all objects used to define the area.
	 */
	public void removeFromWorld();

}
