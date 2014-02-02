package stargatetech2.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * Implemented by blocks that run special actions when clicked with a TabletPC in hand.
 * The default action in StargateTech2 is opening a special GUI.
 * 
 * @author LordFokas
 */
public interface ITabletAccess {
	
	/**
	 * Make the block run a special action when activated with a TabletPC.
	 * This method is only called on the client side.
	 * Implementations requiring server side should use packets.
	 * 
	 * @param player The player activating the block.
	 * @param world The world the block is in.
	 * @param x The block's X coordinate.
	 * @param y The block's Y coordinate.
	 * @param z The block's Z coordinate.
	 * @return True if a special action was executed, false otherwise.
	 */
	public boolean onTabletAccess(EntityPlayer player, World world, int x, int y, int z);
}
