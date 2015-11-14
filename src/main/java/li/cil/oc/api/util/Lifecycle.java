package li.cil.oc.api.util;

import li.cil.oc.api.network.Node;

/**
 * Implement this on {@link li.cil.oc.api.network.ManagedEnvironment}s to be
 * notified with proper lifecycle changes, instead of relying on something
 * like {@link li.cil.oc.api.network.Environment#onDisconnect(Node)}.
 * <p/>
 * This is primarily intended to be used on the client side, where there
 * are no nodes, to allow components to know when they are being unloaded.
 */
public interface Lifecycle {
    /**
     * States an object can enter.
     */
    enum LifecycleState {
        /**
         * State immediately active after construction of the object.
         * <p/>
         * This generally means initial construction of the object and
         * restoring its state (e.g. loading data if it's persistable).
         */
        Constructing,

        /**
         * State active when object is being lazily set up.
         * <p/>
         * This generally means setting up references, and connecting
         * nodes if the object is networked.
         */
        Initializing,

        /**
         * State active when object finished setting up.
         * <p/>
         * This means everything is set up and the object now enters
         * its general use lifetime (where components are updated each
         * tick for example).
         */
        Initialized,

        /**
         * State active when object begins cleaning up.
         * <p/>
         * This means tearing down references and disconnecting nodes,
         * for example.
         */
        Disposing,

        /**
         * State active after object has been cleaned up, right before
         * references by the managing container to it are dropped.
         * <p/>
         * This means the object is now considered "dead".
         */
        Disposed
    }

    /**
     * Called when the state of the object changes.
     *
     * @param state the lifecycle state that is being <em>entered</em>.
     */
    void onLifecycleStateChange(LifecycleState state);
}
