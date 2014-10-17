package li.cil.oc.api.machine;

import li.cil.oc.api.Persistable;

/**
 * A value object can be pushed to a machine like a primitive value.
 * <p/>
 * This is the only non-primitive type that can be pushed to machines, allowing
 * for much more advanced interaction, since the methods on this value can be
 * called from Lua directly. This is similar to component callbacks, but at the
 * same time very different, because these objects can be pushed as results
 * from callbacks, therefore outliving their component, for example.
 * <p/>
 * There are a few limitations too keep in mind:
 * <ul>
 * <li>Values <em>must</em> have a default constructor for loading.</li>
 * <li>Values must be persistable (implement save/load).</li>
 * </ul>
 * <p/>
 * Callbacks can be defined in a manner similar to environments, e.g. using the
 * {@link Callback} annotation.
 */
public interface Value extends Persistable {
    /**
     * This is called when the code running on a machine tries to index this
     * value.
     *
     * @param context   the context from which the method is called, usually the
     *                  instance of the computer running the script that made
     *                  the call.
     * @param arguments the arguments passed to the method.
     * @return the current value at the specified index, or <tt>null</tt>.
     */
    Object apply(Context context, Arguments arguments);

    /**
     * This is called when the code running on a machine tries to assign a new
     * value at the specified index of this value.
     * <p/>
     * Does nothing if the value is not indexable.
     *
     * @param context   the context from which the method is called, usually the
     *                  instance of the computer running the script that made
     *                  the call.
     * @param arguments the arguments passed to the method.
     */
    void unapply(Context context, Arguments arguments);

    /**
     * This is called when the code running on a machine tries to call this
     * value as a function.
     * <p/>
     * If this value is not callable, throws an exception.
     *
     * @param context   the context from which the method is called, usually the
     *                  instance of the computer running the script that made
     *                  the call.
     * @param arguments the arguments passed to the method.
     * @return the result of the call.
     * @throws java.lang.RuntimeException if this value is not callable.
     */
    Object[] call(Context context, Arguments arguments);

    /**
     * This is called when the object's representation in the machine it was
     * pushed to is garbage collected.
     * <p/>
     * <em>Important</em>: be aware of the consequences of pushing the same
     * object to multiple machines. You should usually <em>not</em> do that,
     * but if you do, realize this method may be called by either machine.
     *
     * @param context the context from which the method is called, usually the
     *                instance of the computer running the script that just
     *                garbage collected the object.
     */
    void dispose(Context context);
}
