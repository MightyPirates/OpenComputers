/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
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
import net.minecraft.item.ItemStack;

@Deprecated
public class BptBlockDoor extends BptBlock {

	final ItemStack stack;

	public BptBlockDoor(int blockId, ItemStack stack) {
		super(blockId);

		this.stack = stack;
	}

	@Override
	public void addRequirements(BptSlotInfo slot, IBptContext context, LinkedList<ItemStack> requirements) {
		if ((slot.meta & 8) == 0) {
			requirements.add(stack.copy());
		}
	}

	@Override
	public void rotateLeft(BptSlotInfo slot, IBptContext context) {
		int orientation = (slot.meta & 3);
		int others = slot.meta - orientation;

		switch (orientation) {
		case 0:
			slot.meta = 1 + others;
			break;
		case 1:
			slot.meta = 2 + others;
			break;
		case 2:
			slot.meta = 3 + others;
			break;
		case 3:
			slot.meta = 0 + others;
			break;
		}
	}

	@Override
	public boolean ignoreBuilding(BptSlotInfo slot) {
		return (slot.meta & 8) != 0;
	}

	@Override
	public void buildBlock(BptSlotInfo slot, IBptContext context) {
		//context.world().setBlock(slot.x, slot.y, slot.z, slot.blockId, slot.meta,1);
		//context.world().setBlock(slot.x, slot.y + 1, slot.z, slot.blockId, slot.meta + 8,1);

		context.world().setBlockMetadataWithNotify(slot.x, slot.y + 1, slot.z, slot.meta + 8,1);
		context.world().setBlockMetadataWithNotify(slot.x, slot.y, slot.z, slot.meta,1);

	}
}
