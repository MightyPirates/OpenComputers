package mods.railcraft.api.tracks;

/**
 * Implementing this interface will allow your track to be
 * powered via Redstone.
 *
 * And so long as you inherit from TrackInstanceBase, all the code for updating
 * the power state is already in place (including propagation).
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface ITrackPowered extends ITrackInstance
{

    public boolean isPowered();

    public void setPowered(boolean powered);

    /**
     * The distance that a redstone signal will be passed along from track to track.
     * @return int
     */
    public int getPowerPropagation();
}
