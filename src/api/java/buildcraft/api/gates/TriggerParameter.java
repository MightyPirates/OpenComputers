/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.gates;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class TriggerParameter implements ITriggerParameter {

	protected ItemStack stack;

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.src.buildcraft.api.gates.ITriggerParameter#getItemStack()
	 */
	@Override
	public ItemStack getItemStack() {
		return stack;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.src.buildcraft.api.gates.ITriggerParameter#set(net.minecraft.src.ItemStack)
	 */
	@Override
	public void set(ItemStack stack) {
		if (stack != null) {
			this.stack = stack.copy();
			this.stack.stackSize = 1;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.src.buildcraft.api.gates.ITriggerParameter#writeToNBT(net.minecraft.src.NBTTagCompound)
	 */
	@Override
	public void writeToNBT(NBTTagCompound compound) {
		if (stack != null) {
			NBTTagCompound tagCompound = new NBTTagCompound();
			stack.writeToNBT(tagCompound);
			compound.setTag("stack", tagCompound);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.minecraft.src.buildcraft.api.gates.ITriggerParameter#readFromNBT(net.minecraft.src.NBTTagCompound)
	 */
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		// Legacy code to prevent existing gates from losing their contents
		int itemID = compound.getInteger("itemID");
		if (itemID != 0) {
			stack = new ItemStack((Item) Item.itemRegistry.getObject(itemID), 1, compound.getInteger("itemDMG"));
			return;
		}
		
		stack = ItemStack.loadItemStackFromNBT(compound.getCompoundTag("stack"));
	}

	@Override
	@Deprecated
	public ItemStack getItem() {
		return stack;
	}

}
