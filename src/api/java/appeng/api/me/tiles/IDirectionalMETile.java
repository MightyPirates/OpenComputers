package appeng.api.me.tiles;

import net.minecraftforge.common.ForgeDirection;

/**
 * Used to signify which directions a particular IGridTileEntity or IGridMachine can connect to/from, 
 * you must implement both, if you wish to have this functionality.
 * 
 * Example Buses, that do not conenct in the direction of the bus.
 */
public interface IDirectionalMETile
{
	
	/**
	 * return true if this tile can connect in a specific direction.
	 * @param dir
	 * @return true, if it can connect.
	 */
	boolean canConnect( ForgeDirection dir );
}
