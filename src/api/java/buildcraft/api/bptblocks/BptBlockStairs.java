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
public class BptBlockStairs extends BptBlock {

	public BptBlockStairs(int blockId) {
		super(blockId);
	}

	@Override
	public void addRequirements(BptSlotInfo slot, IBptContext context, LinkedList<ItemStack> requirements) {
		//requirements.add(new ItemStack(slot.blockId, 1, 0));
	}

	@Override
	public boolean isValid(BptSlotInfo slot, IBptContext context) {
		//return slot.blockId == context.world().getBlockId(slot.x, slot.y, slot.z);
		return false;
	}

	@Override
	public void rotateLeft(BptSlotInfo slot, IBptContext context) {
		switch (slot.meta) {
		case 0:
			slot.meta = 2;
			break;
		case 1:
			slot.meta = 3;
			break;
		case 2:
			slot.meta = 1;
			break;
		case 3:
			slot.meta = 0;
			break;
		}
	}

}
