package li.cil.oc.api.network;

/**
 * This interface can be added to {@link Environment}s with a number of
 * {@link li.cil.oc.api.machine.Callback}s on them, to select which of these
 * callbacks should be exposed to machines dynamically.
 * <p/>
 * <em>Important:</em> while this allows dynamic selection of callbacks, if
 * what the filter method returns changes during the lifetime of the
 * environment, these changes will not be reflected in already connected
 * computers.
 */
public interface FilteredEnvironment {
    /**
     * Whether the callback with the specified name on this environment is enabled.
     * <p/>
     * Returning <tt>true</tt> will be the same as if this interface were not
     * implemented. Returning <tt>false</tt> will hide the callback with the
     * specified name from machines connected to it.
     *
     * @param name the name of the callback to check for.
     * @return whether the callback should be visible or not.
     */
    boolean isCallbackEnabled(String name);
}
