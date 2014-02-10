/*
 * Copyright (c) SpaceToad, 2011-2012
 * http://www.mod-buildcraft.com
 * 
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.filler;

import net.minecraft.item.ItemStack;

/**
 *
 * @author CovertJaguar <http://www.railcraft.info/>
 */
public interface IPatternIterator {

	public boolean iteratePattern(ItemStack stackToPlace);
}
