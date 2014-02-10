package mods.railcraft.api.tracks;

import net.minecraft.world.World;

/**
 * Have your ITrackInstance implement this to override normal track placement.
 *
 * Used by tracks such as the Suspended Track.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface ITrackCustomPlaced extends ITrackInstance
{

    /**
     * Used to override normal track placement.
     *
     * Used by tracks such as the Suspended Track.
     *
     * Warning: This is called before the TileEntity is set.
     *
     * @param world The World
     * @param i x-Coord
     * @param j y-Coord
     * @param k z-Coord
     * @return true if the rail can placed at the specified location, false to prevent placement
     */
    public boolean canPlaceRailAt(World world, int i, int j, int k);
}
