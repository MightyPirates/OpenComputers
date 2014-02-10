package li.cil.oc.api.network;

/**
 * This interface can be used with an {@link li.cil.oc.api.network.Environment}
 * and is intended to be used for environments wrapping a ComputerCraft
 * peripheral. Tt could be used for other purposes as well, though. It allows
 * providing method names in addition to those defined via the
 * {@link li.cil.oc.api.network.Callback} annotation, and invoking said methods.
 */
public interface ManagedPeripheral {
    /**
     * Get the list of methods provided by this environment, in
     * <em>addition</em> to methods marked as callbacks.
     * <p/>
     * Returning <tt>null</tt> has the same meaning as returning an empty array,
     * that being that there are no methods - in which case you don't need this
     * interface!
     *
     * @return the list of methods provided by the environment.
     */
    String[] methods();

    /**
     * Calls a method from the list provided by {@link #methods()}.
     * <p/>
     *
     * @param method  the name of the method to call.
     * @param context the context from which the method is called.
     * @param args    the arguments to pass to the method.
     * @return the result of calling the method. Same as for callbacks.
     * @throws java.lang.NoSuchMethodException if there is no method with the
     *                                         specified name.
     */
    Object[] invoke(String method, Context context, Arguments args) throws Exception;
}
