/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.blueprints;

import buildcraft.api.core.BuildCraftAPI;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Deprecated
public class BlueprintManager {

	//public static BptBlock[] blockBptProps = new BptBlock[Block.blocksList.length];

	public static ItemSignature getItemSignature(Item item) {
		ItemSignature sig = new ItemSignature();

		//if (item.itemID >= Block.blocksList.length + BuildCraftAPI.LAST_ORIGINAL_ITEM) {
		//	sig.itemClassName = item.getClass().getSimpleName();
		//}

		sig.itemName = item.getUnlocalizedName(new ItemStack(item));

		return sig;
	}

	public static BlockSignature getBlockSignature(Block block) {
		//return BlueprintManager.blockBptProps[0].getSignature(block);
		return null;
	}

	static {
		// Initialize defaults for block properties.
		//for (int i = 0; i < BlueprintManager.blockBptProps.length; ++i) {
		//	new BptBlock(i);
		//}
	}
}
