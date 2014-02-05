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
import net.minecraft.item.ItemStack;

@Deprecated
public class BptBlockFluid extends BptBlock {

	private final ItemStack bucketStack;

	public BptBlockFluid(int blockId, ItemStack bucketStack) {
		super(blockId);

		this.bucketStack = bucketStack;
	}

	@Override
	public void addRequirements(BptSlotInfo slot, IBptContext context, LinkedList<ItemStack> requirements) {
		if (slot.meta == 0) {
			requirements.add(bucketStack.copy());
		}
	}

	@Override
	public boolean isValid(BptSlotInfo slot, IBptContext context) {
		if (slot.meta == 0)
			return slot.blockId == context.world().getBlockId(slot.x, slot.y, slot.z) && context.world().getBlockMetadata(slot.x, slot.y, slot.z) == 0;
		else
			return true;
	}

	@Override
	public void rotateLeft(BptSlotInfo slot, IBptContext context) {

	}

	@Override
	public boolean ignoreBuilding(BptSlotInfo slot) {
		return slot.meta != 0;
	}

	@Override
	public void buildBlock(BptSlotInfo slot, IBptContext context) {
		if (slot.meta == 0) {
			context.world().setBlock(slot.x, slot.y, slot.z, slot.blockId, 0,1);
		}
	}

}
