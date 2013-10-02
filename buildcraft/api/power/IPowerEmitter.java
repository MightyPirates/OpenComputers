/*
 * Copyright (c) SpaceToad, 2011-2012
 * http://www.mod-buildcraft.com
 * 
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.power;

import net.minecraftforge.common.ForgeDirection;

/**
 * Essentially only used for Wooden Power Pipe connection rules.
 * 
 * This Tile Entity interface allows you to indicate that a block can emit power from a specific
 * side.
 * 
 * @author CovertJaguar <http://www.railcraft.info/>
 */
public interface IPowerEmitter
{

	public boolean canEmitPowerFrom(ForgeDirection side);
}
