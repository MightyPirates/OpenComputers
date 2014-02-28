/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.blueprints;

import java.util.LinkedList;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * This class records a slot, either from a blueprint or from a block placed in the world.
 */

@Deprecated
public class BptSlotInfo {

	public Block block = null;
	public int meta = 0;
	public int x;
	public int y;
	public int z;

	/**
	 * This field contains requirements for a given block when stored in the blueprint. Modders can either rely on this list or compute their own int BptBlock.
	 */
	public LinkedList<ItemStack> storedRequirements = new LinkedList<ItemStack>();

	/**
	 * This tree contains additional data to be stored in the blueprint. By default, it will be initialized from BptBlock.initializeFromWorld with the standard
	 * readNBT function of the corresponding tile (if any) and will be loaded from BptBlock.buildBlock using the standard writeNBT function.
	 */
	public NBTTagCompound cpt = new NBTTagCompound();

	@Override
	public BptSlotInfo clone() {
		BptSlotInfo obj = new BptSlotInfo();

		obj.x = x;
		obj.y = y;
		obj.z = z;
		obj.block = block;
		obj.meta = meta;
		obj.cpt = (NBTTagCompound) cpt.copy();

		return obj;
	}

}
