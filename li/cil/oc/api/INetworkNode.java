package li.cil.oc.api;

/**
 * A single node in a {@link INetwork}.
 * <p/>
 * All nodes in a network <em>should</em> have a unique address; the network
 * will try to generate a unique address and assign it to new nodes. A node must
 * never ever change its address while in a network (because the lookup-table in
 * the network manager would not be notified of this change). If you must change
 * the address, remove the node first, change the address and then add it again.
 * <p/>
 * Per default there are two kinds of nodes: tile entities and item components.
 * If a <tt>TileEntity</tt> implements this interface adding/removal from its
 * network on load/unload will automatically be handled and you will only have
 * to ensure it is added/removed to/from a network when the corresponding block
 * is added/removed. Item components in containers have to be handled manually.
 * All other kinds of nodes you may come up with will also have to be handled
 * manually.
 * <p/>
 * Items have to be handled by a corresponding {@link IItemDriver}. Existing
 * blocks may be interfaced with a proxy block if a {@link IBlockDriver} exists
 * that supports the block.
 */
public interface INetworkNode {
    /**
     * The address of the node, so that it can be found in the network.
     *
     * @return the id of this node.
     */
    int getAddress();

    /**
     * Set the address of the node.
     * <p/>
     * This is used by the network manager when a node is added to a network to
     * assign it a unique address in that network. Nodes should avoid using custom
     * addresses since that could lead to multiple nodes with the same address in
     * a network. Note that that in and by itself is supported however, it is just
     * harder to work with.
     * <p/>
     * Some nodes may however wish to simply ignore this and always provide the
     * same address (e.g. zero), when they are never expected to be used by other
     * nodes (cables, for example).
     *
     * @param value the new address for the node.
     */
    void setAddress(int value);

    /**
     * The network this node is currently in.
     * <p/>
     * Note that valid nodes should never return <tt>null</tt> here. When created
     * a node should immediately be added to a network, after being removed from
     * its network it should be considered invalid.
     *
     * @return the network the node is in.
     */
    INetwork getNetwork();

    /**
     * Sets the network the node is currently in.
     * <p/>
     * This is mainly used by the {@link NetworkAPI} and the network itself when
     * merging with another network. You should never have to call this yourself.
     *
     * @param network the network the node now belongs to.
     */
    void setNetwork(INetwork network);

    /**
     * Makes the node handle a message.
     *
     * @param message the message to handle.
     */
    void receive(INetworkMessage message);
}
