package appeng.api.me.tiles;

import appeng.api.DimentionalCoord;

/**
 * Create a connection to a normally not connected tile
 */
public interface IGridTeleport
{
	DimentionalCoord[] findRemoteSide();
}
