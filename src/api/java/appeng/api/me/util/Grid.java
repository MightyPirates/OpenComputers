package appeng.api.me.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import appeng.api.me.tiles.IGridTileEntity;

/**
 * ME Network, aka Grid related features and functionality.
 */
public class Grid
{
	/**
	 * Attempts to find a grid interface for the specified location.
	 * @param w
	 * @param x
	 * @param y
	 * @param z
	 * @return interfaceForGrid
	 */
    public static IGridInterface getGridInterface(World w, int x, int y, int z)
    {
        IGridTileEntity te = getGridEntity(w, x, y, z);

        if (te != null)
        {
            return te.getGrid();
        }

        return null;
    }
    
    /**
     * Simple test to see if the tile at the specified location is in fact a grid entity.
     * @param w
     * @param x
     * @param y
     * @param z
     * @return isTileGridEntity
     */
    public static boolean isGridEntity(World w, int x, int y, int z)
    {
        return getGridEntity(w, x, y, z) != null;
    }
    
    /**
     * Returns the grid entity at the given location, or null if its not a grid entity.
     * @param w
     * @param x
     * @param y
     * @param z
     * @return gridEntity
     */
    public static IGridTileEntity getGridEntity(World w, int x, int y, int z)
    {
        TileEntity te = w.getBlockTileEntity(x, y, z);

        if (te instanceof IGridTileEntity)
        {
            return (IGridTileEntity)te;
        }

        return null;
    }
    
    /**
     * Tests if the given grid entity is on a Network.
     * @param w
     * @param x
     * @param y
     * @param z
     * @return isOnNetwork
     */
    public static boolean isOnGrid(World w, int x, int y, int z)
    {
        IGridTileEntity te = getGridEntity(w, x, y, z);

        if (te != null)
        {
            return te.getGrid() != null;
        }

        return false;
    }
}
