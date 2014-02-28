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
public class BptBlockRotateMeta extends BptBlock {

	int[] rot;
	boolean rotateForward;

	int infoMask = 0;

	public BptBlockRotateMeta(int blockId, int[] rotations, boolean rotateForward) {
		super(blockId);

		rot = rotations;

		for (int i = 0; i < rot.length; ++i) {
			if (rot[i] < 4) {
				infoMask = (infoMask < 3 ? 3 : infoMask);
			} else if (rot[i] < 8) {
				infoMask = (infoMask < 7 ? 7 : infoMask);
			} else if (rot[i] < 16) {
				infoMask = (infoMask < 15 ? 15 : infoMask);
			}
		}

		this.rotateForward = rotateForward;
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
		int pos = slot.meta & infoMask;
		int others = slot.meta - pos;

		if (rotateForward) {
			if (pos == rot[0]) {
				pos = rot[1];
			} else if (pos == rot[1]) {
				pos = rot[2];
			} else if (pos == rot[2]) {
				pos = rot[3];
			} else if (pos == rot[3]) {
				pos = rot[0];
			}
		} else {
			if (pos == rot[0]) {
				pos = rot[3];
			} else if (pos == rot[1]) {
				pos = rot[2];
			} else if (pos == rot[2]) {
				pos = rot[0];
			} else if (pos == rot[3]) {
				pos = rot[1];
			}
		}

		slot.meta = pos + others;
	}

}
