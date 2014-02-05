/*
 * Copyright (c) SpaceToad, 2011-2012
 * http://www.mod-buildcraft.com
 * 
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.core;

import net.minecraft.item.ItemStack;

/**
 *
 * @author CovertJaguar <http://www.railcraft.info/>
 */
public class StackWrapper {

	public final ItemStack stack;

	public StackWrapper(ItemStack stack) {
		this.stack = stack;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 67 * hash + stack.itemID;
		hash = 67 * hash + stack.getItemDamage();
		if (stack.stackTagCompound != null)
			hash = 67 * hash + stack.stackTagCompound.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final StackWrapper other = (StackWrapper) obj;
		if (stack.itemID != other.stack.itemID)
			return false;
		if (stack.getHasSubtypes() && stack.getItemDamage() != other.stack.getItemDamage())
			return false;
		if (stack.stackTagCompound != null && !stack.stackTagCompound.equals(other.stack.stackTagCompound))
			return false;
		return true;
	}
}
