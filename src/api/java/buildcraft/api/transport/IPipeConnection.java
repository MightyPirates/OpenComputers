/**
 * Copyright (c) SpaceToad, 2011 http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License
 * 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.transport;

import buildcraft.api.transport.IPipeTile.PipeType;
import net.minecraftforge.common.ForgeDirection;

public interface IPipeConnection {

	enum ConnectOverride {

		CONNECT, DISCONNECT, DEFAULT
	};

	/**
	 * Allows you to override pipe connection logic.
	 *
	 * @param type
	 * @param with
	 * @return CONNECT to force a connection, DISCONNECT to force no connection,
	 * and DEFAULT to let the pipe decide.
	 */
	public ConnectOverride overridePipeConnection(PipeType type, ForgeDirection with);
}
