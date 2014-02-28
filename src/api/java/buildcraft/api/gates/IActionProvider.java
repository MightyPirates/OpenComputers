/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.gates;

import java.util.LinkedList;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

public interface IActionProvider {

	/**
	 * Returns the list of actions available to a gate next to the given block.
	 */
	public abstract LinkedList<IAction> getNeighborActions(Block block, TileEntity tile);

}
