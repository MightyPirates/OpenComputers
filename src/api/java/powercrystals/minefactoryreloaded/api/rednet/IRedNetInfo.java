package powercrystals.minefactoryreloaded.api.rednet;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Defines a Block that can print information about itself using the RedNet Meter. This must be implemented on your Block class.
 */
public interface IRedNetInfo
{
	/**
	 * This function appends information to a list provided to it.
	 * 
	 * @param world Reference to the world.
	 * @param x X coordinate of the block.
	 * @param y Y coordinate of the block.
	 * @param z Z coordinate of the block.
	 * @param side The side of the block that is being queried.
	 * @param player Player doing the querying - this can be NULL.
	 * @param info The list that the information should be appended to.
	 */
	public void getRedNetInfo(IBlockAccess world, int x, int y, int z,
			ForgeDirection side, EntityPlayer player, List<IChatComponent> info);
}
