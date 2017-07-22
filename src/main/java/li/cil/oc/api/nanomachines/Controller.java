package li.cil.oc.api.nanomachines;

/**
 * The nanomachine controller is responsible for keeping track of the current
 * layout of neural connections (i.e. how nanomachine "inputs" connect to
 * behaviors, directly or indirectly).
 * <p/>
 * Each input can connect to one or more nodes. A node can either be a
 * behavior, or an indirect connection, which in turn is connected to one
 * or more behaviors (there is at maximum one layer of indirection). Each
 * indirection may trigger one or more behaviors, but may also require one
 * or more inputs to activate its outputs.
 * <p/>
 * Each node, input or indirection, will only connect to one or two other
 * nodes, to keep randomization at a somewhat manageable level, but to still
 * allow for some optimization by re-rolling the connections.
 * <p/>
 * This interface is not meant to be implemented externally. To get a reference
 * to a controller, use {@link li.cil.oc.api.Nanomachines#getController}.
 */
public interface Controller {
    /**
     * Reconfigure the neural connections managed by this controller. This
     * will lead to the system being unavailable for a short while, in which
     * the neural connections are rebuilt in a new configuration. In addition,
     * some debuffs will be applied to the player.
     * <p/>
     * This will reset all inputs to disabled and deactivate all previously
     * active behaviors.
     *
     * @return the controller itself, for chaining / convenience.
     */
    Controller reconfigure();

    /**
     * Get the number of inputs available.
     * <p/>
     * This number depends on the total number of behaviors available, to keep
     * randomization at a manageable level. It is computed internally and
     * based on a configuration value.
     *
     * @return the total number of available inputs.
     */
    int getTotalInputCount();

    /**
     * Get the number of inputs that may be active at the same time
     * before negative effects are applied to the player.
     * <p/>
     * The number of active inputs may exceed this value, but this will
     * have negative effects on the player.
     *
     * @return the number of inputs that may safely be active at a time.
     */
    int getSafeActiveInputs();

    /**
     * Get the total number of inputs that may be active at the same time.
     * <p/>
     * The number of active inputs cannot exceed this value.
     *
     * @return the number of inputs that may be active at a time.
     */
    int getMaxActiveInputs();

    /**
     * Get whether the input with the specified index is active.
     *
     * @param index the input index.
     * @return whether the input is active.
     * @throws IndexOutOfBoundsException if <code>index &lt; 0</code> or <code>index &gt;= getInputCount</code>.
     */
    boolean getInput(int index);

    /**
     * Set the state of the input with the specified index.
     * <p/>
     * This will fail if too many inputs are active already. It will also
     * always fail when called on the client.
     *
     * @param index the input index.
     * @param value whether the input should be active.
     * @return whether the input was changed successfully.
     * @throws IndexOutOfBoundsException if <code>index &lt; 0</code> or <code>index &gt;= getInputCount</code>.
     */
    boolean setInput(int index, boolean value);

    /**
     * Get the list of currently active behaviors, based on the current input states.
     * <p/>
     * Note that behaviors may behave differently depending on how many active
     * inputs they have. Behaviors in the returned list will have at least one
     * active input.
     *
     * @return the list of currently active behaviors. Never <tt>null</tt>.
     */
    Iterable<Behavior> getActiveBehaviors();

    /**
     * Get the number of active inputs for the specified behavior.
     *
     * @param behavior the behavior to get the number of inputs for.
     * @return the number of inputs active for the specified behavior.
     */
    int getInputCount(Behavior behavior);

    // ----------------------------------------------------------------------- //

    /**
     * The amount of energy stored by this nanomachine controller.
     */
    double getLocalBuffer();

    /**
     * The maximum amount of energy stored by this nanomachine controller.
     */
    double getLocalBufferSize();

    /**
     * Try to apply the specified delta to the controller's buffer.
     * <p/>
     * A negative value will drain energy from the buffer, a positive value
     * will inject energy into the buffer.
     *
     * @param delta the amount of energy to consume or store.
     * @return the remainder of the delta that could not be applied.
     */
    double changeBuffer(double delta);
}
