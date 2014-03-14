/** 
 * Copyright (c) SpaceToad, 2011
 * http://www.mod-buildcraft.com
 * 
 * BuildCraft is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package buildcraft.api.blueprints;

import buildcraft.api.core.IBox;
import buildcraft.api.core.Position;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * This interface provide contextual information when building or initializing blueprint slots.
 */

@Deprecated
public interface IBptContext {

	/**
	 * If bptItemStack is an ItemStack extracted from the blueprint containing this mapping, this will return an item stack with the id of the current world
	 */
	public ItemStack mapItemStack(ItemStack bptItemStack);

	/**
	 * Blueprints may be created in a world with a given id setting, and then ported to a world with different ids. Heuristics are used to retreive these new
	 * ids automatically. This interface provide services to map ids from a blueprints to current ids in the world, and should be used whenever storing block
	 * numbers or item stacks in blueprints..
	 */
	public int mapWorldId(int bptWorldId);

	/**
	 * This asks the id mapping to store a mapping from this Id, which may be either an itemId or a blockId. In effect, the blueprint will record it and make it
	 * available upon blueprint load. Note that block present in the blueprint are automatically stored upon blueprint save, so this is really only needed when
	 * writing ids that are e.g. in inventory stacks.
	 */
	public void storeId(int worldId);

	public Position rotatePositionLeft(Position pos);

	public IBox surroundingBox();

	public World world();
}
