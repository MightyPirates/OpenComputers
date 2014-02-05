package li.cil.oc.api;

import li.cil.oc.api.detail.Builder;
import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.tileentity.TileEntity;

/**
 * This class provides factories for networks and nodes.
 * <p/>
 * The first two functions provided provided by this API are to allow existing
 * environments - implemented in a tile entity for example - to join an existing
 * network or create new ones. The third one is used to create nodes that may
 * then be connected to an existing network. It is not possible to create
 * networks that do not belong to at least one tile entity or node.
 * <p/>
 * It is important to understand that component networks only exist on the
 * server side. It is impossible to create nodes, and therefore networks, on
 * the client side. This is to enforce a clear distinction of where the actual
 * logic lies - since user code only runs on the server.
 * <p/>
 * Note that these methods should <em>not</em> be called in the pre-init phase,
 * since the {@link #instance} may not have been initialized at that time. Only
 * start calling these methods in the init phase or later.
 */
public final class Network {
    /**
     * Tries to add a tile entity's network node(s) at the specified coordinates
     * to adjacent networks.
     * <p/>
     * If the tile entity implements {@link Environment} its one node will be
     * connected to any existing adjacent tile entity nodes. If none exist a
     * new network with the specified tile entity's node as its sole entry.
     * <p/>
     * If the tile entity is a {@link li.cil.oc.api.network.SidedEnvironment}
     * the same rules as for simple environments apply, except that the
     * respective for each side is used when connecting, and each side's node
     * is added to its own new network, if necessary.
     *
     * @param tileEntity the tile entity to initialize.
     */
    public static void joinOrCreateNetwork(final TileEntity tileEntity) {
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
    public static void joinNewNetwork(final Node node) {
        if (instance != null) instance.joinNewNetwork(node);
    }

    /**
     * Factory function for creating new nodes.
     * <p/>
     * Use this to create a node for your environment (e.g. tile entity). This
     * will return a builder that can be used to further specialize the node,
     * making it either a component node (for callbacks), a connector node
     * (for power interaction) or both.
     * <p/>
     * Example use:
     * <pre>
     * class YourThing extends TileEntity implements Environment {
     *     private ComponentConnector node_ =
     *         api.Network.newNode(this, Visibility.Network).
     *             withComponent("your_thing").
     *             withConnector(32).
     *             create();
     *
     *     public Node node() { return node_; }
     *
     *     // ...
     * }
     * </pre>
     * <p/>
     * Note that the <em>reachability</em> specified here is the general
     * availability of the created node to other nodes in the network. Special
     * rules apply to components, which have a <em>visibility</em> that is used
     * to control how they can be reached from computers. For example, network
     * cards have a <em>reachability</em> of <tt>Visibility.Network</tt>, to
     * allow them to communicate with each other, but a <em>visibility</em> of
     * <tt>Visibility.Neighbors</tt> to avoid other computers in the network
     * to see the card (i.e. only the user programs running on the computer the
     * card installed in can see interact with it).
     *
     * @param host         the environment the node is created for.
     * @param reachability the reachability of the node.
     * @return a new node builder.
     */
    public static Builder.NodeBuilder newNode(final Environment host, final Visibility reachability) {
        if (instance != null) return instance.newNode(host, reachability);
        return null;
    }

    // ----------------------------------------------------------------------- //

    private Network() {
    }

    public static NetworkAPI instance = null;
}
