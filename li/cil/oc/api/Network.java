package li.cil.oc.api;

import li.cil.oc.api.detail.Builder;
import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Visibility;
import net.minecraft.world.World;

/**
 * This class provides factories networks and nodes.
 * <p/>
 * The two functions provided provided by this API are to allow existing
 * environments - implemented in a tile entity - to join an existing network or
 * create a new one, and to create nodes that may then be connected to an
 * existing network. It is not possible to create networks that do not belong
 * to at least one tile entity at this time.
 */
public final class Network {
    /**
     * Tries to add a tile entity network node at the specified coordinates to
     * adjacent networks.
     *
     * @param world the world the tile entity lives in.
     * @param x     the X coordinate of the tile entity.
     * @param y     the Y coordinate of the tile entity.
     * @param z     the Z coordinate of the tile entity.
     */
    public static void joinOrCreateNetwork(World world, int x, int y, int z) {
        if (instance != null) instance.joinOrCreateNetwork(world, x, y, z);
    }

    /**
     * Factory function for creating new nodes.
     * <p/>
     * Use this to create a node for your environment (e.g. tile entity). This
     * will return a builder that can be used to further specialize the node,
     * making it either a component node (for Lua callbacks), a connector node
     * (for power interaction) or both.
     * <p/>
     * Example use:
     * <pre>
     * class YourThing extends TileEntity implements Environment {
     *     private ComponentConnector node_ = api.Network.newNode(this, Visibility.Network).
     *         withComponent("your_thing").
     *         withConnector(32).
     *         create();
     *
     *     public Node node() { return node_; }
     *
     *     // ...
     * }
     * </pre>
     *
     * @param host       the environment the node is created for.
     * @param visibility the visibility of the node.
     * @return a new node builder.
     */
    public static Builder.NodeBuilder newNode(Environment host, Visibility visibility) {
        if (instance != null) return instance.newNode(host, visibility);
        return null;
    }

    // ----------------------------------------------------------------------- //

    private Network() {
    }

    public static NetworkAPI instance = null;
}
