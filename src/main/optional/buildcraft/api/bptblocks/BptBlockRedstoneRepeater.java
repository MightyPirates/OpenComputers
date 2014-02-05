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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Deprecated
public class BptBlockRedstoneRepeater extends BptBlock {

	public BptBlockRedstoneRepeater(int blockId) {
		super(blockId);
	}

	@Override
	public void addRequirements(BptSlotInfo slot, IBptContext context, LinkedList<ItemStack> requirements) {
		requirements.add(new ItemStack(Item.redstoneRepeater));
	}

	@Override
	public void rotateLeft(BptSlotInfo slot, IBptContext context) {
		int step = slot.meta - (slot.meta & 3);

		switch (slot.meta - step) {
		case 0:
			slot.meta = 1 + step;
			break;
		case 1:
			slot.meta = 2 + step;
			break;
		case 2:
			slot.meta = 3 + step;
			break;
		case 3:
			slot.meta = 0 + step;
			break;
		}
	}
}
