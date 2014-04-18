package appeng.api.util;

import net.minecraft.world.IBlockAccess;

/**
 * Implemented on many of AE's non Tile Entity Blocks as a way to get a IOrientable.
 */
public interface IOrientableBlock
{
	
	/**
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @return a IOrientable if applicable
	 */
	IOrientable getOrientable(IBlockAccess world, int x, int y, int z);
	
}