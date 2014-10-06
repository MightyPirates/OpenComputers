package li.cil.oc.api.event;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Events for handling file system access and representing it on the client.
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
    protected String sound;

    protected World world;

    protected double x;

    protected double y;

    protected double z;

    protected TileEntity tileEntity;

    protected NBTTagCompound data;

    /**
     * Constructor for tile entity hosted file systems.
     *
     * @param sound      the name of the sound effect to play.
     * @param tileEntity the tile entity hosting the file system.
     * @param data       the additional data.
     */
    protected FileSystemAccessEvent(String sound, TileEntity tileEntity, NBTTagCompound data) {
        this.sound = sound;
        this.world = tileEntity.getWorldObj();
        this.x = tileEntity.xCoord + 0.5;
        this.y = tileEntity.yCoord + 0.5;
        this.z = tileEntity.zCoord + 0.5;
        this.tileEntity = tileEntity;
        this.data = data;
    }

    /**
     * Constructor for arbitrarily hosted file systems.
     *
     * @param sound the name of the sound effect to play.
     * @param world the world the file system lives in.
     * @param x     the x coordinate of the file system's container.
     * @param y     the y coordinate of the file system's container.
     * @param z     the z coordinate of the file system's container.
     * @param data  the additional data.
     */
    protected FileSystemAccessEvent(String sound, World world, double x, double y, double z, NBTTagCompound data) {
        this.sound = sound;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tileEntity = null;
        this.data = data;
    }

    /**
     * The name of the sound effect to play for the file system.
     */
    public String getSound() {
        return sound;
    }

    /**
     * The world the file system lives in.
     */
    public World getWorld() {
        return world;
    }

    /**
     * The x coordinate of the file system's container.
     */
    public double getX() {
        return x;
    }

    /**
     * The y coordinate of the file system's container.
     */
    public double getY() {
        return y;
    }

    /**
     * The z coordinate of the file system's container.
     */
    public double getZ() {
        return z;
    }

    /**
     * The tile entity hosting the file system.
     * <p/>
     * <em>Important</em>: this can be <tt>null</tt>, which is usually the
     * case when the container is an entity or item.
     */
    public TileEntity getTileEntity() {
        return tileEntity;
    }

    /**
     * Addition custom data, this is used to transmit the number of the server
     * in a server rack the file system lives in, for example.
     */
    public NBTTagCompound getData() {
        return data;
    }

    public static final class Server extends FileSystemAccessEvent {
        private Node node;

        public Server(String sound, TileEntity tileEntity, Node node) {
            super(sound, tileEntity, new NBTTagCompound());
            this.node = node;
        }

        public Server(String sound, World world, double x, double y, double z, Node node) {
            super(sound, world, x, y, z, new NBTTagCompound());
            this.node = node;
        }

        /**
         * The node of the file system that signalled activity.
         */
        public Node getNode() {
            return node;
        }
    }

    public static final class Client extends FileSystemAccessEvent {
        /**
         * Constructor for tile entity hosted file systems.
         *
         * @param sound      the name of the sound effect to play.
         * @param tileEntity the tile entity hosting the file system.
         * @param data       the additional data.
         */
        public Client(String sound, TileEntity tileEntity, NBTTagCompound data) {
            super(sound, tileEntity, data);
        }

        /**
         * Constructor for arbitrarily hosted file systems.
         *
         * @param sound the name of the sound effect to play.
         * @param world the world the file system lives in.
         * @param x     the x coordinate of the file system's container.
         * @param y     the y coordinate of the file system's container.
         * @param z     the z coordinate of the file system's container.
         * @param data  the additional data.
         */
        public Client(String sound, World world, double x, double y, double z, NBTTagCompound data) {
            super(sound, world, x, y, z, data);
        }
    }
}
