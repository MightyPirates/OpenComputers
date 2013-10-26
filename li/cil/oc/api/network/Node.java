package li.cil.oc.api.network;

import li.cil.oc.api.Persistable;
import li.cil.oc.api.network.environment.Environment;
import li.cil.oc.api.network.environment.ManagedEnvironment;

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
 * Items have to be handled by a corresponding `ItemDriver`. Existing
 * blocks may be interfaced with the adapter block if a `BlockDriver` exists
 * that supports the block.
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
     * The name of the node.
     * <p/>
     * This should be the type name of the component represented by the node,
     * since this is what is returned from `driver.componentType`. As such it
     * is to be expected that there be multiple nodes with the same name.
     */
    String name();

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

    // ----------------------------------------------------------------------- //

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
     * This is called once per tick.
     * <p/>
     * If the node is held by a {@link ManagedEnvironment} this function will
     * also automatically be called, i.e. a managed environment should
     * <em>not</em> call this function. Normal environments (tile entities, e.g)
     * however <em>should</em> call this function in their own update method.
     */
    void update();

    Object[] receive(Message message);
}