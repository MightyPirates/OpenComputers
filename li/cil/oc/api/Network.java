package li.cil.oc.api;

import li.cil.oc.api.detail.Builder;
import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.tileentity.TileEntity;

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
     * @param tileEntity the tile entity to initialize.
     */
    public static void joinOrCreateNetwork(TileEntity tileEntity) {
        if (instance != null) instance.joinOrCreateNetwork(tileEntity);
    }

    /**
     * Creates a new network with the specified node as its initial node.
     * <p/>
     * This can be used to create networks that are not bound to any tile
     * entity. For example, this is used to create the internal networks of
     * robots.
     *
     * @param node the node to create the network for.
     * @throws IllegalArgumentException if the node already is in a network.
     */
    public static void joinNewNetwork(Node node) {
        if (instance != null) instance.joinNewNetwork(node);
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
