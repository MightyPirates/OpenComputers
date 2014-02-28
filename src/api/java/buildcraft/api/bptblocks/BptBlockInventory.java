
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
import net.minecraft.inventory.IInventory;

@Deprecated
public class BptBlockInventory extends BptBlock {

	public BptBlockInventory(int blockId) {
		super(blockId);

	}

	@Override
	public void buildBlock(BptSlotInfo slot, IBptContext context) {
		super.buildBlock(slot, context);

		IInventory inv = (IInventory) context.world().getTileEntity(slot.x, slot.y, slot.z);

		for (int i = 0; i < inv.getSizeInventory(); ++i) {
			inv.setInventorySlotContents(i, null);
		}

	}

}
