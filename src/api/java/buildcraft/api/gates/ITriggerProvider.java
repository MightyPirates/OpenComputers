/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.gates;

import buildcraft.api.transport.IPipeTile;
import java.util.LinkedList;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

public interface ITriggerProvider {

	/**
	 * Returns the list of triggers that are available from the pipe holding the gate.
	 */
	public abstract LinkedList<ITrigger> getPipeTriggers(IPipeTile pipe);

	/**
	 * Returns the list of triggers available to a gate next to the given block.
	 */
	public abstract LinkedList<ITrigger> getNeighborTriggers(Block block, TileEntity tile);

}
