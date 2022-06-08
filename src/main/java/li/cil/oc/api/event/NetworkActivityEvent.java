package li.cil.oc.api.event;

import cpw.mods.fml.common.eventhandler.Event;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Events for handling network activity and representing it on the client.
 * <br>
 * This is used to render network activity
 * indicators on some containers (e.g. computer, server).
 * <br>
 * Use this to implement rendering of disk access indicators on you own
 * containers / computers / drive bays.
 * <br>
 * Canceling this event is provided to allow registering higher priority
 * event handlers that override default behavior.
 */
public class NetworkActivityEvent extends Event {
    protected World world;

    protected double x;

    protected double y;

    protected double z;

    protected TileEntity tileEntity;

    protected NBTTagCompound data;

    /**
     * Constructor for tile entity hosted network cards.
     *
     * @param tileEntity the tile entity hosting the network card.
     * @param data       the additional data.
     */
    protected NetworkActivityEvent(TileEntity tileEntity, NBTTagCompound data) {
        this.world = tileEntity.getWorldObj();
        this.x = tileEntity.xCoord + 0.5;
        this.y = tileEntity.yCoord + 0.5;
        this.z = tileEntity.zCoord + 0.5;
        this.tileEntity = tileEntity;
        this.data = data;
    }

    /**
     * Constructor for arbitrarily hosted network cards.
     *
     * @param world the world the network card lives in.
     * @param x     the x coordinate of the network card's container.
     * @param y     the y coordinate of the network card's container.
     * @param z     the z coordinate of the network card's container.
     * @param data  the additional data.
     */
    protected NetworkActivityEvent(World world, double x, double y, double z, NBTTagCompound data) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.tileEntity = null;
        this.data = data;
    }

    /**
     * The world the network card lives in.
     */
    public World getWorld() {
        return world;
    }

    /**
     * The x coordinate of the network card's container.
     */
    public double getX() {
        return x;
    }

    /**
     * The y coordinate of the network card's container.
     */
    public double getY() {
        return y;
    }

    /**
     * The z coordinate of the network card's container.
     */
    public double getZ() {
        return z;
    }

    /**
     * The tile entity hosting the network card.
     * <br>
     * <em>Important</em>: this can be <tt>null</tt>, which is usually the
     * case when the container is an entity or item.
     */
    public TileEntity getTileEntity() {
        return tileEntity;
    }

    /**
     * Addition custom data, this is used to transmit the number of the server
     * in a server rack the network card lives in, for example.
     */
    public NBTTagCompound getData() {
        return data;
    }

    public static final class Server extends NetworkActivityEvent {
        private Node node;

        public Server(TileEntity tileEntity, Node node) {
            super(tileEntity, new NBTTagCompound());
            this.node = node;
        }

        public Server(World world, double x, double y, double z, Node node) {
            super(world, x, y, z, new NBTTagCompound());
            this.node = node;
        }

        /**
         * The node of the network card that signalled activity.
         */
        public Node getNode() {
            return node;
        }
    }

    public static final class Client extends NetworkActivityEvent {
        /**
         * Constructor for tile entity hosted network card.
         *
         * @param tileEntity the tile entity hosting the network card.
         * @param data       the additional data.
         */
        public Client(TileEntity tileEntity, NBTTagCompound data) {
            super(tileEntity, data);
        }

        /**
         * Constructor for arbitrarily hosted network card.
         *
         * @param world the world the file system lives in.
         * @param x     the x coordinate of the network card's container.
         * @param y     the y coordinate of the network card's container.
         * @param z     the z coordinate of the network card's container.
         * @param data  the additional data.
         */
        public Client(World world, double x, double y, double z, NBTTagCompound data) {
            super(world, x, y, z, data);
        }
    }
}
