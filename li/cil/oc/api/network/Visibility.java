package li.cil.oc.api.network;

/**
 * Possible reachability values for nodes.
 * <p/>
 * Since all nodes that are connected are packed into the same network, we want
 * some way of controlling what's accessible from where on a low level (to
 * avoid unnecessary messages and unauthorized access).
 * <p/>
 * Note that there is a more specific kind of visibility for components. See
 * {@link Component} for more details on that.
 */
public enum Visibility {
    /**
     * Nodes with this visibility neither receive nor send messages.
     * <p/>
     * Components with this visibility cannot be seen nor reached by computers.
     */
    None,

    /**
     * Nodes with this visibility only receive messages from their immediate
     * neighbors, i.e. nodes to which a direct connection exists. It can send
     * messages to all nodes visible to it.
     * <p/>
     * Components with this visibility can likewise only be reached by the
     * computer(s) they are directly attached to. For example, if a block
     * component is placed directly next to the computer, or an item installed
     * in the computer (i.e. it is in the computer's inventory).
     */
    Neighbors,

    /**
     * Nodes with this visibility can receive messages from any node in its
     * network. It can still only send messages to all nodes visible to it.
     * <p/>
     * Components with this visibility are likewise reachable by all computers
     * in their network. For example, a screen only indirectly connected to a
     * computer will still be addressable by that computer.
     */
    Network
}
