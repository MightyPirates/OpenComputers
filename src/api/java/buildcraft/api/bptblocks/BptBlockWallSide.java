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
public class BptBlockWallSide extends BptBlock {

	public BptBlockWallSide(int blockId) {
		super(blockId);
	}

	@Override
	public void addRequirements(BptSlotInfo slot, IBptContext context, LinkedList<ItemStack> requirements) {
		//requirements.add(new ItemStack(slot.blockId, 1, 0));
	}

	@Override
	public void rotateLeft(BptSlotInfo slot, IBptContext context) {
		final int XPos = 2;
		final int XNeg = 1;
		final int ZPos = 4;
		final int ZNeg = 3;

		switch (slot.meta) {
		case XPos:
			slot.meta = ZPos;
			break;
		case ZNeg:
			slot.meta = XPos;
			break;
		case XNeg:
			slot.meta = ZNeg;
			break;
		case ZPos:
			slot.meta = XNeg;
			break;
		}
	}
}
