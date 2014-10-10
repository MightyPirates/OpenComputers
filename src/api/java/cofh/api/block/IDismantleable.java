package cofh.api.block;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Implemented on Blocks which have some method of being instantly dismantled.
 * 
 * @author King Lemming
 * 
 */
public interface IDismantleable {

	/**
	 * Dismantles the block. If returnDrops is true, the drop(s) should be placed into the player's inventory.
	 */
	ArrayList<ItemStack> dismantleBlock(EntityPlayer player, World world, int x, int y, int z, boolean returnDrops);

	/**
	 * Return true if the block can be dismantled. The criteria for this is entirely up to the block.
	 */
	boolean canDismantle(EntityPlayer player, World world, int x, int y, int z);

}
