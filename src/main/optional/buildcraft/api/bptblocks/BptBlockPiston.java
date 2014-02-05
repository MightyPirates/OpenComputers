/**
 * Copyright (c) SpaceToad, 2011
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package buildcraft.api.bptblocks;

import buildcraft.api.blueprints.BptSlotInfo;
import buildcraft.api.blueprints.IBptContext;

@Deprecated
public class BptBlockPiston extends BptBlockRotateMeta {

	public BptBlockPiston(int blockId) {
		super(blockId, new int[] { 2, 5, 3, 4 }, true);
	}

	@Override
	public void buildBlock(BptSlotInfo slot, IBptContext context) {
		int meta = slot.meta & 7;

		context.world().setBlock(slot.x, slot.y, slot.z, slot.blockId, meta,1);
	}

}
