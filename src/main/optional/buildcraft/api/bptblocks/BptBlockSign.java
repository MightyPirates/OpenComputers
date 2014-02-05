/**
 * Copyright (c) SpaceToad, 2011
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package buildcraft.api.bptblocks;

import buildcraft.api.blueprints.BlockSignature;
import buildcraft.api.blueprints.BptBlock;
import buildcraft.api.blueprints.BptSlotInfo;
import buildcraft.api.blueprints.IBptContext;
import java.util.LinkedList;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Deprecated
public class BptBlockSign extends BptBlock {

	boolean isWall;

	public BptBlockSign(int blockId, boolean isWall) {
		super(blockId);

		this.isWall = isWall;
	}

	@Override
	public void addRequirements(BptSlotInfo slot, IBptContext context, LinkedList<ItemStack> requirements) {
		requirements.add(new ItemStack(Item.sign));
	}

	@Override
	public void rotateLeft(BptSlotInfo slot, IBptContext context) {
		if (!isWall) {
			double angle = ((slot.meta) * 360.0) / 16.0;
			angle += 90.0;
			if (angle >= 360) {
				angle -= 360;
			}
			slot.meta = (int) (angle / 360.0 * 16.0);
		} else {
			// slot.meta = ForgeDirection.values()[slot.meta].rotateLeft().ordinal();
		}
	}

	@Override
	public BlockSignature getSignature(Block block) {
		BlockSignature sig = super.getSignature(block);

		if (isWall) {
			sig.customField = "wall";
		} else {
			sig.customField = "floor";
		}

		return sig;
	}

}
