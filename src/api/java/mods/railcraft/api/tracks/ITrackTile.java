package mods.railcraft.api.tracks;

/**
 * Don't use this, its an interface that allows other API code
 * access to internal functions of the code.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface ITrackTile
{

    public ITrackInstance getTrackInstance();
    
    public void sendUpdateToClient();
}
