package li.cil.oc.api.network;

/**
 * Components are nodes that can be addressed computers via drivers.
 * <p/>
 * Components therefore form a sub-network in the overall network, and some
 * special rules apply to them. For one, components specify an additional
 * kind of visibility. Component visibility may have to differ from real
 * network visibility in some cases, such as network cards (which have to
 * be able to communicate across the whole network, but computers should only
 * "see" the cards installed directly in them).
 * <p/>
 * Unlike the `Node`'s network visibility, this is a dynamic value and can be
 * changed at any time. For example, this is used to hide multi-block screen
 * parts that are not the origin from computers in the network.
 * <p/>
 * The method responsible for dispatching network messages from computers also
 * only allows sending messages to components that the computer can see,
 * according to the component's visibility. Therefore nodes won't receive
 * messages from computer's that should not be able to see them.
 */
public interface Component extends Node {
    /**
     * Get the visibility of this component.
     */
    Visibility visibility();

    /**
     * Set the visibility of this component.
     * <p/>
     * Note that this cannot be higher / more visible than the reachability of
     * the node. Trying to set it to a higher value will generate an exception.
     */
    void setVisibility(Visibility value);

    /**
     * Tests whether this component can be seen by the specified node,
     * usually representing a computer in the network.
     * <p/>
     * <em>Important</em>: this will always return <tt>true</tt> if the node is
     * not currently in a network.
     *
     * @param other the computer node to check for.
     * @return true if the computer can see this node; false otherwise.
     */
    boolean canBeSeenBy(Node other);
}
