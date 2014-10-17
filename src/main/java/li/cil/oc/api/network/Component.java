package li.cil.oc.api.network;

import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.util.Collection;

/**
 * Components are nodes that can be addressed computers via drivers.
 * <p/>
 * Components therefore form a sub-network in the overall network, and some
 * special rules apply to them. For one, components specify an additional
 * kind of visibility. Component visibility may have to differ from real
 * network reachability in some cases, such as network cards (which have to
 * be able to communicate across the whole network, but computers should only
 * "see" the cards installed directly in them).
 * <p/>
 * Unlike the {@link Node}'s network reachability, this is a dynamic value and
 * can be changed at any time. For example, this is used to hide multi-block
 * screen parts that are not the origin from computers in the network.
 * <p/>
 * The method responsible for dispatching network messages from computers also
 * only allows sending messages to components that the computer can see,
 * according to the component's visibility. Therefore nodes won't receive
 * messages from computer's that should not be able to see them.
 */
public interface Component extends Node {
    /**
     * The name of the node.
     * <p/>
     * This should be the type name of the component represented by the node,
     * since this is what is returned from <tt>component.type</tt>. As such it
     * is to be expected that there be multiple nodes with the same name, but
     * that those nodes all have the same underlying type (i.e. there can be
     * multiple "filesystem" nodes, but they should all behave the same way).
     */
    String name();

    /**
     * Get the visibility of this component.
     */
    Visibility visibility();

    /**
     * Set the visibility of this component.
     * <p/>
     * Note that this cannot be higher / more visible than the reachability of
     * the node. Trying to set it to a higher value will generate an exception.
     *
     * @throws java.lang.IllegalArgumentException if the specified value is
     *                                            more visible than the node's
     *                                            reachability.
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
    boolean canBeSeenFrom(Node other);

    // ----------------------------------------------------------------------- //

    /**
     * The list of names of methods exposed by this component.
     * <p/>
     * This does not return the callback annotations directly, because those
     * may not contain the method's name (as it defaults to the name of the
     * annotated method).
     * <p/>
     * The returned collection is read-only.
     */
    Collection<String> methods();

    /**
     * Get the annotation information of a method.
     * <p/>
     * This is needed for custom architecture implementations that need to know
     * if a callback is direct or not, for example.
     *
     * @param method the method to the the info for.
     * @return the annotation of the specified method or <tt>null</tt>.
     */
    Callback annotation(String method);

    /**
     * Tries to call a function with the specified name on this component.
     * <p/>
     * The name of the method must be one of the names in {@link #methods()}.
     * The returned array may be <tt>null</tt> if there is no return value.
     *
     * @param method    the name of the method to call.
     * @param context   the context from which the method is called, usually the
     *                  instance of the computer running the script that made
     *                  the call.
     * @param arguments the arguments passed to the method.
     * @return the list of results, or <tt>null</tt> if there is no result.
     * @throws NoSuchMethodException if there is no method with that name.
     */
    Object[] invoke(String method, Context context, Object... arguments) throws Exception;
}
