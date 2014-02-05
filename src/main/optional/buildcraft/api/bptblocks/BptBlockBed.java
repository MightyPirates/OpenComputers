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
public class BptBlockBed extends BptBlock {

	public BptBlockBed(int blockId) {
		super(blockId);
	}

	@Override
	public void addRequirements(BptSlotInfo slot, IBptContext context, LinkedList<ItemStack> requirements) {
		if ((slot.meta & 8) == 0) {
			requirements.add(new ItemStack(Item.bed));
		}
	}

	@Override
	public void rotateLeft(BptSlotInfo slot, IBptContext context) {
		int orientation = (slot.meta & 7);
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
	public void buildBlock(BptSlotInfo slot, IBptContext context) {
		if ((slot.meta & 8) != 0)
			return;

		context.world().setBlock(slot.x, slot.y, slot.z, slot.blockId, slot.meta,1);

		int x2 = slot.x;
		int z2 = slot.z;

		switch (slot.meta) {
		case 0:
			z2++;
			break;
		case 1:
			x2--;
			break;
		case 2:
			z2--;
			break;
		case 3:
			x2++;
			break;
		}

		context.world().setBlock(x2, slot.y, z2, slot.blockId, slot.meta + 8,1);
	}

	@Override
	public boolean ignoreBuilding(BptSlotInfo slot) {
		return (slot.meta & 8) != 0;
	}
}
