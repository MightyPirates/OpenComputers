package cofh.api.block;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.ForgeDirection;

/**
 * Implement this interface on blocks which can provide information about themselves.
 * 
 * @author King Lemming
 * 
 */
public interface IBlockInfo {

	/**
	 * This function appends information to a list provided to it.
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
	 *            The side of the block that is being queried.
	 * @param player
	 *            Player doing the querying - this can be NULL.
	 * @param info
	 *            The list that the information should be appended to.
	 * @param debug
	 *            If true, the Block should return "debug" information.
	 */
	public void getBlockInfo(IBlockAccess world, int x, int y, int z, ForgeDirection side, EntityPlayer player, List<String> info, boolean debug);

}
