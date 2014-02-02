package li.cil.oc.api.network;

import li.cil.oc.api.Persistable;

/**
 * A single node in a {@link Network}.
 * <p/>
 * All nodes in a network have a unique address; the network will generate a
 * unique address and assign it to new nodes.
 * <p/>
 * Per default there are two kinds of nodes: tile entities and items.
 * <p/>
 * Items will usually only have nodes when in containers, such as a computer or
 * disk drive. Otherwise you'll have to connect/disconnect them manually as
 * desired.
 * <p/>
 * All other kinds of nodes you may come up with will also have to be
 * handled manually.
 * <p/>
 * Items have to be handled by a corresponding {@link li.cil.oc.api.driver.Item}.
 * Existing blocks may be interfaced with the adapter block if a
 * {@link li.cil.oc.api.driver.Block} exists that supports the block.
 * <p/>
 * <em>Important</em>: like the <tt>Network</tt> interface you must not create
 * your own implementations of this interface. Use the factory methods in the
 * network API to create new node instances and store them in your environment.
 *
 * @see Component
 */
public interface Node extends Persistable {
    /**
     * The environment hosting this node.
     * <p/>
     * For blocks whose tile entities implement {@link Environment} this will
     * be the tile entity. For all other implementations this will be a managed
     * environment.
     */
    Environment host();

    /**
     * The reachability of this node.
     * <p/>
     * This is used by the network to control which system messages to deliver
     * to which nodes. This value should not change over the lifetime of a node.
     * <p/>
     * It furthermore determines what is returned by the <tt>Network</tt>'s
     * <tt>neighbors</tt> and <tt>nodes</tt> functions.
     * <p/>
     * Note that this has no effect on the <em>real</em> reachability of a node;
     * it is only used to filter to which nodes to send connect, disconnect and
     * reconnect messages. If addressed directly, the node will still receive
     * that message even if it comes from a node that should not be able to see
     * it. Therefore nodes should still verify themselves that they want to
     * accept a message from the message's source.
     * <p/>
     * A different matter is a {@link Component}'s <tt>visibility</tt>, which is
     * checked before delivering messages a computer tries to send.
     */
    Visibility reachability();

    /**
     * The address of the node, so that it can be found in the network.
     * <p/>
     * This is used by the network manager when a node is added to a network to
     * assign it a unique address, if it doesn't already have one. Nodes must not
     * use custom addresses, only those assigned by the network. The only option
     * they have is to *not* have an address, which can be useful for "dummy"
     * nodes, such as cables. In that case they may ignore the address being set.
     */
    String address();

    /**
     * The network this node is currently in.
     * <p/>
     * Note that valid nodes should never return `None` here. When created a node
     * should immediately be added to a network, after being removed from its
     * network a node should be considered invalid.
     * <p/>
     * This will always be set automatically by the network manager. Do not
     * change this value and do not return anything that it wasn't set to.
     */
    Network network();

    // ----------------------------------------------------------------------- //

    /**
     * Checks whether this node is a neighbor of the specified node.
     *
     * @param other the node to check for.
     * @return whether this node is directly connected to the other node.
     */
    boolean isNeighborOf(Node other);

    /**
     * Checks whether this node can be reached from the specified node.
     *
     * @param other the node to check for.
     * @return whether this node can be reached from the specified node.
     */
    boolean canBeReachedFrom(Node other);

    /**
     * Get the list of neighbor nodes, i.e. nodes directly connected to this
     * node.
     * <p/>
     * This is a shortcut for <tt>node.network.neighbors(node)</tt>.
     * <p/>
     * If this node is not in a network, i.e. <tt>network</tt> is <tt>null</tt>,
     * this returns an empty list.
     *
     * @return the list of nodes directly connected to this node.
     */
    Iterable<Node> neighbors();

    /**
     * Get the list of nodes reachable from this node, based on their
     * {@link #reachability()}.
     * <p/>
     * This is a shortcut for <tt>node.network.nodes(node)</tt>.
     * <p/>
     * If this node is not in a network, i.e. <tt>network</tt> is <tt>null</tt>,
     * this returns an empty list.
     *
     * @return the list of nodes reachable from this node.
     */
    Iterable<Node> reachableNodes();

    // ----------------------------------------------------------------------- //

    /**
     * Connects the specified node to this node.
     * <p/>
     * This is a shortcut for <tt>node.network.connect(node, other)</tt>.
     * <p/>
     * If this node is not in a network, i.e. <tt>network</tt> is <tt>null</tt>,
     * this will throw an exception.
     *
     * @param node the node to connect to this node.
     * @throws NullPointerException if <tt>network</tt> is <tt>null</tt>.
     */
    void connect(Node node);

    /**
     * Disconnects the specified node from this node.
     * <p/>
     * This is a shortcut for <tt>node.network.disconnect(node, other)</tt>.
     * <p/>
     * If this node is not in a network, i.e. <tt>network</tt> is <tt>null</tt>,
     * this will do nothing.
     *
     * @param node the node to connect to this node.
     * @throws NullPointerException if <tt>network</tt> is <tt>null</tt>.
     */
    void disconnect(Node node);

    /**
     * Removes this node from its network.
     * <p/>
     * This is a shortcut for <tt>node.network.remove(node)</tt>.
     * <p/>
     * If this node is not in a network, i.e. <tt>network</tt> is <tt>null</tt>,
     * this will do nothing.
     */
    void remove();

    // ----------------------------------------------------------------------- //

    /**
     * Send a message to a node with the specified address.
     * <p/>
     * This is a shortcut for <tt>node.network.sendToAddress(node, ...)</tt>.
     * <p/>
     * If this node is not in a network, i.e. <tt>network</tt> is <tt>null</tt>,
     * this will do nothing.
     *
     * @param target the address of the node to send the message to.
     * @param name   the name of the message.
     * @param data   the data to pass along with the message.
     */
    void sendToAddress(String target, String name, Object... data);

    /**
     * Send a message to all neighbors of this node.
     * <p/>
     * This is a shortcut for <tt>node.network.sendToNeighbors(node, ...)</tt>.
     * <p/>
     * If this node is not in a network, i.e. <tt>network</tt> is <tt>null</tt>,
     * this will do nothing.
     *
     * @param name the name of the message.
     * @param data the data to pass along with the message.
     */
    void sendToNeighbors(String name, Object... data);

    /**
     * Send a message to all nodes reachable from this node.
     * <p/>
     * This is a shortcut for <tt>node.network.sendToReachable(node, ...)</tt>.
     * <p/>
     * If this node is not in a network, i.e. <tt>network</tt> is <tt>null</tt>,
     * this will do nothing.
     *
     * @param name the name of the message.
     * @param data the data to pass along with the message.
     */
    void sendToReachable(String name, Object... data);

    /**
     * Send a message to all nodes visible from this node.
     * <p/>
     * This is a shortcut for <tt>node.network.sendToVisible(node, ...)</tt>.
     * <p/>
     * If this node is not in a network, i.e. <tt>network</tt> is <tt>null</tt>,
     * this will do nothing.
     *
     * @param name the name of the message.
     * @param data the data to pass along with the message.
     */
    void sendToVisible(String name, Object... data);
}