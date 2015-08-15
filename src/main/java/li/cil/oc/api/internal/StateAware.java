package li.cil.oc.api.internal;

import java.util.EnumSet;

/**
 * Implemented on machines that have an "working" state.
 * <p/>
 * This is similar to BuildCraft's <tt>IHasWork</tt> interface , but is also
 * used for comparator output in the case of OpenComputers' blocks.
 */
public interface StateAware {
    /**
     * Get the current work state (usually a <tt>TileEntity</tt>.
     * <p/>
     * An empty set indicates that no work can be performed.
     *
     * @return the current state.
     */
    EnumSet<State> getCurrentState();

    /**
     * Possible work states.
     */
    enum State {
        /**
         * Indicates that some work can be performed / energy can be consumed,
         * but that the current state is being idle.
         */
        CanWork,

        /**
         * Indicates that some work is currently being performed / some energy
         * is currently being consumed.
         */
        IsWorking
    }
}
