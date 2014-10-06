package li.cil.oc.api.event;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Fired on the <em>client side only</em>, signals file system access.
 * <p/>
 * This is used to play file system access sounds and render disk activity
 * indicators on some containers (e.g. disk drive, computer, server).
 * <p/>
 * Use this to implement rendering of disk access indicators on you own
 * containers / computers / drive bays.
 * <p/>
 * Canceling this event is provided to allow registering higher priority
 * event handlers that override default behavior.
 */
@Cancelable
public class FileSystemAccessEvent extends Event {
    /**
     * The name of the sound effect to play for the file system.
     */
    public final String sound;

    /**
     * The world the file system lives in.
     */
    public final World world;

    /**
     * The x coordinate of the file system's container.
     */
    public final double x;

    /**
     * The y coordinate of the file system's container.
     */
    public final double y;

    /**
     * The z coordinate of the file system's container.
     */
    public final double z;

    /**
     * The tile entity hosting the file system.
     * <p/>
     * <em>Important</em>: this can be <tt>null</tt>, which is usually the
     * case when the container is an entity or item.
     */
    public final TileEntity tileEntity;

    /**
     * Constructor for tile entity hosted file systems.
     *
     * @param sound      the name of the sound effect to play.
     * @param tileEntity the tile entity hosting the file system.
     */
    public FileSystemAccessEvent(String sound, TileEntity tileEntity) {
        this.sound = sound;
        this.world = tileEntity.getWorldObj();
        this.x = tileEntity.xCoord + 0.5;
        this.y = tileEntity.yCoord + 0.5;
        this.z = tileEntity.zCoord + 0.5;
        this.tileEntity = tileEntity;
    }

    /**
     * Constructor for arbitrarily hosted file systems.
     *
     * @param sound the name of the sound effect to play.
     * @param world the world the file system lives in.
     * @param x     the x coordinate of the file system's container.
     * @param y     the y coordinate of the file system's container.
     * @param z     the z coordinate of the file system's container.
     */
    public FileSystemAccessEvent(String sound, World world, double x, double y, double z) {
        this.sound = sound;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tileEntity = null;
    }
}
