package appeng.api.util;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface ICommonTile
{

	/**
	 * implemented on AE's Tile Entities, Gets a list of drops that the entity will normally drop, this doesn't include
	 * the block itself.
	 * 
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @param drops
	 */
	void getDrops(World world, int x, int y, int z, ArrayList<ItemStack> drops);

}