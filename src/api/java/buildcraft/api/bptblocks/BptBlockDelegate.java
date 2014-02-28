/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.bptblocks;

import buildcraft.api.blueprints.BlueprintManager;
import buildcraft.api.blueprints.BptBlock;
import buildcraft.api.blueprints.BptSlotInfo;
import buildcraft.api.blueprints.IBptContext;

import java.util.LinkedList;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

@Deprecated
public class BptBlockDelegate extends BptBlock {

	final Block delegateTo;

	public BptBlockDelegate(int blockId, Block delegateTo) {
		super(blockId);

		this.delegateTo = delegateTo;
	}

	@Override
	public void addRequirements(BptSlotInfo slot, IBptContext context, LinkedList<ItemStack> requirements) {
		BptSlotInfo newSlot = slot.clone();
		slot.block = delegateTo;

		//if (BlueprintManager.blockBptProps[delegateTo] != null) {
		//	BlueprintManager.blockBptProps[delegateTo].addRequirements(newSlot, context, requirements);
		//} else {
		//	super.addRequirements(newSlot, context, requirements);
		//}
	}

	@Override
	public boolean isValid(BptSlotInfo slot, IBptContext context) {
		BptSlotInfo newSlot = slot.clone();
		slot.block = delegateTo;

		//if (BlueprintManager.blockBptProps[delegateTo] != null)
		//	return BlueprintManager.blockBptProps[delegateTo].isValid(newSlot, context);
		//else
		//	return super.isValid(newSlot, context);
		
		return false;
	}

	@Override
	public void rotateLeft(BptSlotInfo slot, IBptContext context) {
		BptSlotInfo newSlot = slot.clone();
		slot.block = delegateTo;

		//if (BlueprintManager.blockBptProps[delegateTo] != null) {
		//	BlueprintManager.blockBptProps[delegateTo].rotateLeft(newSlot, context);
		//} else {
		//	super.rotateLeft(newSlot, context);
		//}
	}

}
