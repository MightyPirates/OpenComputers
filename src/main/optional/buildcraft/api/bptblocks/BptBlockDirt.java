/**
 * Copyright (c) SpaceToad, 2011
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package buildcraft.api.bptblocks;

import buildcraft.api.blueprints.BptBlock;
import buildcraft.api.blueprints.BptSlotInfo;
import buildcraft.api.blueprints.IBptContext;
import java.util.LinkedList;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

@Deprecated
public class BptBlockDirt extends BptBlock {

	public BptBlockDirt(int blockId) {
		super(blockId);
	}

	@Override
	public void addRequirements(BptSlotInfo slot, IBptContext context, LinkedList<ItemStack> requirements) {
		requirements.add(new ItemStack(Block.dirt));
	}

	@Override
	public void buildBlock(BptSlotInfo slot, IBptContext context) {
		context.world().setBlock(slot.x, slot.y, slot.z, Block.dirt.blockID, slot.meta,1);
	}

	@Override
	public boolean isValid(BptSlotInfo slot, IBptContext context) {
		int id = context.world().getBlockId(slot.x, slot.y, slot.z);

		return id == Block.dirt.blockID || id == Block.grass.blockID || id == Block.tilledField.blockID;
	}
}
