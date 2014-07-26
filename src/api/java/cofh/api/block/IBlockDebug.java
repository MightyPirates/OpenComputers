package cofh.api.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Implement this interface on blocks which have some debug method which can be activated via a tool or other means.
 * 
 * @author King Lemming
 * 
 */
public interface IBlockDebug {

	/**
	 * This function debugs a block.
	 * 
	 * @param world
	 *            Reference to the world.
	 * @param x
	 *            X coordinate of the block.
	 * @param y
	 *            Y coordinate of the block.
	 * @param z
	 *            Z coordinate of the block.
	 * @param side
	 *            The side of the block.
	 * @param player
	 *            Player doing the debugging.
	 */
	void debugBlock(IBlockAccess world, int x, int y, int z, ForgeDirection side, EntityPlayer player);

}
