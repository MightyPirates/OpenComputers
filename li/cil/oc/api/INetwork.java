package li.cil.oc.api;

/**
 * Interface for interacting with component networks.
 * <p/>
 * Computers and components form ad-hoc "networks" when placed next to each
 * other. They allow computers to communicate with the components attached to
 * them, as well as components to send signals to computers they are attached to
 * (and even among each other).
 * <p/>
 * Whenever a networkable component is placed, it should first scan its
 * neighbors to see if a network already exists. If so, it should join that
 * network. If multiple different networks are adjacent it should join one and
 * then merge it with the other(s). If no networks exist, it should create a new
 * one. All this logic is provided by {@link NetworkAPI#joinOrCreateNetwork}.
 * <p/>
 * Note that for network nodes implemented in <tt>TileEntities</tt> adding and
 * removal is automatically provided on chunk load and unload. When a block is
 * placed or broken you will have to implement this logic yourself (i.e. call
 * <tt>NetworkAPI.joinOrCreateNetwork</tt> in <tt>onBlockAdded</tt> and
 * <tt>getNetwork.remove</tt> in <tt>breakBlock</tt>.
 * <p/>
 * All other kinds of nodes have to be managed manually. Also see
 * {@link INetworkNode}.
 * <p/>
 * There are a couple of system messages to be aware of. These are all sent by
 * the network manager itself:
 * <ul>
 * <li><tt>network.connect</tt> is generated when a node is added to the
 * network, with the added node as the sender.</li>
 * <li><tt>network.disconnect</tt> is generated when a node is removed from the
 * network, with the removed node as the sender.</li>
 * <li><tt>network.reconnect</tt> is generated when a node's address changes,
 * usually due to a network merge, with the node whose address changed as the
 * sender and the old address as the only parameter.</li>
 * </ul>
 * <p/>
 * IMPORTANT: do <em>not</em> implement this interface yourself and create
 * instances of your own network implementation; this will lead to
 * incompatibilities with the built-in network implementation (which can only
 * merge with other networks of its own type). Always use the methods provided
 * in <tt>NetworkAPI</tt> to create and join networks.
 */
public interface INetwork {
    /**
     * Adds a new node connection in the network.
     * <p/>
     * This is used by nodes to join an existing network. At least one of the two
     * nodes must already be in the network. If one of the nodes is not yet in the
     * network, it will be added to the network. If both nodes are already in the
     * network only the connection between the two nodes is stored. If one of the
     * nodes is not in this network but in another network, the networks will be
     * merged.
     * <p/>
     * This way of adding nodes is used to build an internal graph to allow
     * properly splitting networks when nodes are removed.
     *
     * @param nodeA the first node.
     * @param nodeB the other node.
     * @return whether a node was added or not.
     * @throws IllegalArgumentException if neither node is in this network.
     * @throws IllegalStateException    if this is called while the network is
     *                                  already updating, for example from
     *                                  {@link INetworkNode#receive} (which may
     *                                  be called during the update).
     */
    boolean connect(INetworkNode nodeA, INetworkNode nodeB);

    /**
     * Removes a node from the network.
     * <p/>
     * This should be called by nodes when they are destroyed (e.g. onBreakBlock)
     * or unloaded. If removing the node leads to two graphs (it was the a bridge
     * node) the network will be split up.
     *
     * @param node the node to remove from the network.
     * @return whether the node was removed.
     */
    boolean remove(INetworkNode node);

    /**
     * Sends a message to a specific node.
     * <p/>
     * Messages should have a unique name to allow differentiating them when
     * handling them in a network node. For example, computers will try to parse
     * messages named "signal" by converting the message data to a signal and
     * inject that signal into the Lua VM, so no message not used for this purpose
     * should be named "signal".
     *
     * @param source the node that sends the message.
     * @param target the id of the node to send the message to.
     * @param name   the name of the message.
     * @param data   the message to send.
     */
    void sendToNode(INetworkNode source, int target, String name, Object... data);

    /**
     * Sends a message to all nodes in the network.
     * <p/>
     * Messages should have a unique name to allow differentiating them when
     * handling them in a network node. For example, computers will try to parse
     * messages named "signal" by converting the message data to a signal and
     * inject that signal into the Lua VM, so no message not used for this purpose
     * should be named "signal".
     *
     * @param source the node that sends the message.
     * @param data   the message to send.
     */
    void sendToAll(INetworkNode source, String name, Object... data);
}
