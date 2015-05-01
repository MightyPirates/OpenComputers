package li.cil.oc.api.machine;

import li.cil.oc.api.network.ManagedEnvironment;

import java.util.Map;

/**
 * This interface allows interacting with a Machine obtained via the factory
 * method {@link li.cil.oc.api.Machine#create(MachineHost)}.
 */
@SuppressWarnings("unused")
public interface Machine extends ManagedEnvironment, Context {
    /**
     * The owner of the machine, usually a tile entity hosting the machine.
     *
     * @return the owner of the machine.
     */
    MachineHost host();

    /**
     * This must be called from the host when something relevant to the
     * machine changes, such as a change in the amount of available memory.
     */
    void onHostChanged();

    /**
     * The underlying architecture of the machine.
     * <p/>
     * This is what actually evaluates code running on the machine, where the
     * machine class itself serves as a scheduler.
     * <p/>
     * This may be <tt>null</tt>, for example when the hosting computer has
     * no CPU installed.
     *
     * @return the architecture of this machine.
     */
    Architecture architecture();

    /**
     * The list of components attached to this machine.
     * <p/>
     * This maps address to component type/name. Note that the list may not
     * immediately reflect changes after components were added to the network,
     * since such changes are cached in an internal list of 'added components'
     * that are processed in the machine's update logic (i.e. server tick).
     * <p/>
     * This list is kept up-to-date automatically, do <em>not</em> mess with it.
     *
     * @return the list of attached components.
     */
    Map<String, String> components();

    /**
     * The number of connected components.
     * <p/>
     * This number can differ from <tt>components().size()</tt>, since this is
     * the number of actually <em>connected</em> components, which is used to
     * determine whether the component limit has been exceeded, for example. It
     * takes into account components added but not processed, yet (see also
     * {@link #components()}).
     *
     * @return the number of connected components.
     */
    int componentCount();

    /**
     * The maximum number of components this machine can currently support.
     * <p/>
     * This is automatically recomputed based on the hosts internal components
     * whenever the host calls {@link li.cil.oc.api.machine.Machine#onHostChanged()}.
     *
     * @return the maximum number of components supported.
     */
    int maxComponents();

    /**
     * Gets the amount of energy this machine consumes per tick when it is
     * running.
     *
     * @return the energy consumed per tick by the machine.
     */
    double getCostPerTick();

    /**
     * Sets the amount of energy this machine consumes per tick when it is
     * running.
     *
     * @param value the energy consumed per tick by the machine.
     */
    void setCostPerTick(double value);

    /**
     * The address of the file system that holds the machine's temporary files
     * (tmpfs). This may return <tt>null</tt> if either the creation of the file
     * system failed, or if the size of the tmpfs has been set to zero in the
     * config.
     * <p/>
     * Use this in a custom architecture to allow code do differentiate the
     * tmpfs from other file systems, for example.
     *
     * @return the address of the tmpfs component, or <tt>null</tt>.
     */
    String tmpAddress();

    /**
     * A string with the last error message.
     * <p/>
     * The error string is set either when the machine crashes (see the
     * {@link #crash(String)} method), or when it fails to start (which,
     * technically, is also a crash).
     * <p/>
     * When the machine started, this is reset to <tt>null</tt>.
     *
     * @return the last error message, or <tt>null</tt>.
     */
    String lastError();

    /**
     * The current world time. This is updated each tick and provides a thread
     * safe way to access the world time for architectures.
     * <p/>
     * This is equivalent to <tt>owner().world().getWorldTime()</tt>.
     *
     * @return the current world time.
     */
    long worldTime();

    /**
     * The time that has passed since the machine was started, in seconds.
     * <p/>
     * Note that this is actually measured in world time, so the resolution is
     * pretty limited. This is done to avoid 'time skips' when leaving the game
     * and coming back later, resuming a persisted machine.
     */
    double upTime();

    /**
     * The time spent running the underlying architecture in execution threads,
     * i.e. the time spent in {@link Architecture#runThreaded(boolean)} since
     * the machine was last started, in seconds.
     */
    double cpuTime();

    // ----------------------------------------------------------------------- //

    /**
     * Play a sound using the machine's built-in speaker.
     * <p/>
     * This is what's used to emit beep codes when an error occurs while trying
     * to start the computer, for example, and what's used for playing sounds
     * when <tt>computer.beep</tt> is called.
     * <p/>
     * Be responsible in how you limit calls to this, as each call will cause
     * a packet to be sent to all nearby clients, and will cause the receiving
     * clients to generate the required sound sample on-the-fly. It is
     * therefore recommended to not call this too frequently, and to limit the
     * length of the sound to something relatively short (not longer than a few
     * seconds at most).
     * <p/>
     * The audio will be played at the machine's host's location.
     *
     * @param frequency the frequency of the tone to generate.
     * @param duration  the duration of the tone to generate, in milliseconds.
     */
    void beep(short frequency, short duration);

    /**
     * Utility method for playing beep codes.
     * <p/>
     * The underlying functionality is similar to that of {@link #beep(short, short)},
     * except that this will play tones at a fixed frequency, and two different
     * durations - in a pattern as defined in the passed string.
     * <p/>
     * This is useful for generating beep codes, such as for boot errors. It
     * has the advantage of only generating a single network packet, and
     * generating a single, longer sound sample for the full pattern. As such
     * the same considerations should be made as for {@link #beep(short, short)},
     * i.e. prefer not to use overly long patterns.
     * <p/>
     * The passed pattern must consist of dots (<tt>.</tt>) and dashes (<tt>-</tt>),
     * where a dot is short tone, and a dash is a long tone.
     * <p/>
     * The audio will be played at the machine's host's location.
     *
     * @param pattern the beep pattern to play.
     */
    void beep(String pattern);

    /**
     * Crashes the computer.
     * <p/>
     * This is exactly the same as {@link Context#stop()}, except that it also
     * sets the error message in the machine. This message can be seen when the
     * Analyzer is used on computer cases, for example.
     *
     * @param message the message to set.
     * @return <tt>true</tt> if the computer switched to the stopping state.
     */
    boolean crash(String message);

    /**
     * Tries to pop a signal from the queue and returns it.
     * <p/>
     * Signals are stored in a FIFO queue of limited size. This method is / must
     * be called by architectures regularly to process the queue.
     *
     * @return a signal or <tt>null</tt> if the queue was empty.
     */
    Signal popSignal();

    /**
     * Get a list of all methods and their annotations of the specified object.
     * <p/>
     * The specified object can be either a {@link li.cil.oc.api.machine.Value}
     * or a {@link li.cil.oc.api.network.Environment}. This is useful for
     * custom architectures, to allow providing a list of callback methods to
     * evaluated programs.
     *
     * @param value the value to get the method listing for.
     * @return the methods that can be called on the object.
     */
    Map<String, Callback> methods(Object value);

    /**
     * Makes the machine call a component callback.
     * <p/>
     * This is intended to be used from architectures, but may be useful in
     * other scenarios, too. It will make the machine call the method with the
     * specified name on the attached component with the specified address.
     * <p/>
     * This will perform a visibility check, ensuring the component can be seen
     * from the machine. It will also ensure that the direct call limit for
     * individual callbacks is respected.
     *
     * @param address the address of the component to call the method on.
     * @param method  the name of the method to call.
     * @param args    the list of arguments to pass to the callback.
     * @return a list of results returned by the callback, or <tt>null</tt>.
     * @throws LimitReachedException    when the called method supports direct
     *                                  calling, but the number of calls in this
     *                                  tick has exceeded the allowed limit.
     * @throws IllegalArgumentException if there is no such component.
     * @throws Exception                if the callback throws an exception.
     */
    Object[] invoke(String address, String method, Object[] args) throws Exception;

    /**
     * Makes the machine call a value callback.
     * <p/>
     * This is intended to be used from architectures, but may be useful in
     * other scenarios, too. It will make the machine call the method with the
     * specified name on the specified value.
     * <p/>
     * This will will ensure that the direct call limit for individual
     * callbacks is respected.
     *
     * @param value  the value to call the method on.
     * @param method the name of the method to call.
     * @param args   the list of arguments to pass to the callback.
     * @return a list of results returned by the callback, or <tt>null</tt>.
     * @throws LimitReachedException    when the called method supports direct
     *                                  calling, but the number of calls in this
     *                                  tick has exceeded the allowed limit.
     * @throws IllegalArgumentException if there is no such component.
     * @throws Exception                if the callback throws an exception.
     */
    Object[] invoke(Value value, String method, Object[] args) throws Exception;

    // ----------------------------------------------------------------------- //

    /**
     * The list of users registered on this machine.
     * <p/>
     * This list is used for {@link Context#canInteract(String)}. Exposed for
     * informative purposes only, for example to expose it to user code. Note
     * that the returned array is a copy of the internal representation of the
     * user list. Changing it has no influence on the actual list.
     *
     * @return the list of registered users.
     */
    String[] users();

    /**
     * Add a player to the machine's list of users, by username.
     * <p/>
     * This requires for the player to be online.
     *
     * @param name the name of the player to add as a user.
     * @throws Exception if
     *                   <ul>
     *                   <li>There are already too many users.</li>
     *                   <li>The player is already registered.</li>
     *                   <li>The provided name is too long.</li>
     *                   <li>The player is not online.</li>
     *                   </ul>
     */
    void addUser(String name) throws Exception;

    /**
     * Removes a player as a user from this machine, by username.
     * <p/>
     * Unlike when adding players, the player does <em>not</em> have to be
     * online to be removed from the list.
     *
     * @param name the name of the player to remove.
     * @return whether the player was removed from the user list.
     */
    boolean removeUser(String name);
}
