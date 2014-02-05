/** 
 * Copyright (c) SpaceToad, 2011
 * http://www.mod-buildcraft.com
 * 
 * BuildCraft is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package buildcraft.api.gates;

import java.util.LinkedList;

/**
 * This interface has to be implemented by a TileEntity or a Pipe that wants to provide triggers different from the ones installed by default with BuildCraft.
 */
public interface IOverrideDefaultTriggers {

	LinkedList<ITrigger> getTriggers();

}
