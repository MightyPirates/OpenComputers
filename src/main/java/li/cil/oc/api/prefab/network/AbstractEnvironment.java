package li.cil.oc.api.prefab.network;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.*;
import net.minecraft.nbt.NBTTagCompound;

@SuppressWarnings("UnusedDeclaration")
public abstract class AbstractEnvironment implements Environment {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final Node node;

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String NODE_TAG = "node";

    private final EnvironmentHost host;

    // ----------------------------------------------------------------------- //

    protected AbstractEnvironment(final EnvironmentHost host) {
        node = createNode();
        this.host = host;
    }

    // ----------------------------------------------------------------------- //
    // Environment

    @Override
    public EnvironmentHost getHost() {
        return host;
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public void onConnect(final Node node) {
        // This is called when the call to Network.joinOrCreateNetwork(this) in
        // updateEntity was successful, in which case `node == this`.
        // This is also called for any other node that gets connected to the
        // network our node is in, in which case `node` is the added node.
        // If our node is added to an existing network, this is called for each
        // node already in said network.
    }

    @Override
    public void onDisconnect(final Node node) {
        // This is called when this node is removed from its network when the
        // tile entity is removed from the world (see onChunkUnload() and
        // invalidate()), in which case `node == this`.
        // This is also called for each other node that gets removed from the
        // network our node is in, in which case `node` is the removed node.
        // If a net-split occurs this is called for each node that is no longer
        // connected to our node.
    }

    @Override
    public void onMessage(final Message message) {
        // This is used to deliver messages sent via node.sendToXYZ. Handle
        // messages at your own discretion. If you do not wish to handle a
        // message you should *not* throw an exception, though.
    }

    // ----------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagCompound serializeNBT() {
        final NBTTagCompound nbt = new NBTTagCompound();

        final Node n = getNode();
        if (n != null) {
            // Force joining a network when saving and we're not in one yet, so that
            // the address is embedded in the saved data that gets sent to the client,
            // so that that address can be used to associate components on server and
            // client (for example keyboard and screen/text buffer).
            if (n.getAddress() == null) {
                Network.joinNewNetwork(n);
                n.remove();
            }

            nbt.setTag(NODE_TAG, n.serializeNBT());
        }

        return nbt;
    }

    @Override
    public void deserializeNBT(final NBTTagCompound nbt) {
        final Node n = getNode();
        if (n != null) {
            n.deserializeNBT(nbt);
        }
    }

    // ----------------------------------------------------------------------- //

    /**
     * Override this in subclasses to build a node that is used to represent
     * this tile entity. By default, a network visible node is created with
     * component features (i.e. {@link li.cil.oc.api.machine.Callback}s
     * will be made available) using the lower-cased class name.
     * <p/>
     * You must only create new nodes using the factory method in the network
     * API, {@link li.cil.oc.api.Network#newNode(Environment, Visibility)}.
     * <p/>
     * For example:
     * <pre>
     * // The first parameters to newNode is the host() of the node, which will
     * // usually be this tile entity. The second one is it's reachability,
     * // which determines how other nodes in the same network can query this
     * // node. See {@link li.cil.oc.api.network.Network#nodes(li.cil.oc.api.network.Node)}.
     * node = Network.newNode(this, Visibility.Network)
     *       // This call allows the node to consume energy from the
     *       // component network it is in and act as a consumer, or to
     *       // inject energy into that network and act as a producer.
     *       // If you do not need energy remove this call.
     *       .withConnector()
     *       // This call marks the tile entity as a component. This means you
     *       // can mark methods in it using the {@link li.cil.oc.api.machine.Callback}
     *       // annotation, making them callable from user code. The first
     *       // parameter is the name by which the component will be known in
     *       // the computer, in this case it could be accessed as
     *       // <tt>component.example</tt>. The second parameter is the
     *       // component's visibility. This is like the node's reachability,
     *       // but only applies to computers. For example, network cards can
     *       // only be <em>seen</em> by the computer they're installed in, but
     *       // can be <em>reached</em> by all other network cards in the same
     *       // network. If you do not need callbacks remove this call.
     *       .withComponent("example", Visibility.Neighbors)
     *       // Finalizes the construction of the node and returns it.
     *       .create();
     * </pre>
     */
    protected abstract Node createNode();
}
