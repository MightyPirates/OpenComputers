/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.gates;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public interface ITileTrigger extends ITrigger {

	/**
	 * Return true if the tile given in parameter activates the trigger, given
	 * the parameters.
	 */
	boolean isTriggerActive(ForgeDirection side, TileEntity tile, ITriggerParameter parameter);
}
