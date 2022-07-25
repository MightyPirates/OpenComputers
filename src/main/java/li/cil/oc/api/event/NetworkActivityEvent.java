package li.cil.oc.api.event;

import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.eventbus.api.Event;

/**
 * Events for handling network activity and representing it on the client.
 * <p/>
 * This is used to render network activity
 * indicators on some containers (e.g. computer, server).
 * <p/>
 * Use this to implement rendering of disk access indicators on you own
 * containers / computers / drive bays.
 * <p/>
 * Canceling this event is provided to allow registering higher priority
 * event handlers that override default behavior.
 */
public class NetworkActivityEvent extends Event {
    protected World world;

    protected double x;

    protected double y;

    protected double z;

    protected TileEntity tileEntity;

    protected CompoundNBT data;

    /**
     * Constructor for tile entity hosted network cards.
     *
     * @param tileEntity the tile entity hosting the network card.
     * @param data       the additional data.
     */
    protected NetworkActivityEvent(TileEntity tileEntity, CompoundNBT data) {
        this.world = tileEntity.getLevel();
        this.x = tileEntity.getBlockPos().getX() + 0.5;
        this.y = tileEntity.getBlockPos().getY() + 0.5;
        this.z = tileEntity.getBlockPos().getZ() + 0.5;
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
    protected NetworkActivityEvent(World world, double x, double y, double z, CompoundNBT data) {
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
     * <p/>
     * <em>Important</em>: this can be <tt>null</tt>, which is usually the
     * case when the container is an entity or item.
     */
    public TileEntity getBlockEntity() {
        return tileEntity;
    }

    /**
     * Addition custom data, this is used to transmit the number of the server
     * in a server rack the network card lives in, for example.
     */
    public CompoundNBT getData() {
        return data;
    }

    public static final class Server extends NetworkActivityEvent {
        private Node node;

        public Server(TileEntity tileEntity, Node node) {
            super(tileEntity, new CompoundNBT());
            this.node = node;
        }

        public Server(World world, double x, double y, double z, Node node) {
            super(world, x, y, z, new CompoundNBT());
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
        public Client(TileEntity tileEntity, CompoundNBT data) {
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
        public Client(World world, double x, double y, double z, CompoundNBT data) {
            super(world, x, y, z, data);
        }
    }
}
