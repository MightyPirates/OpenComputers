package li.cil.oc.api.machine;

import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;

import java.util.Map;

/**
 * This interface allows interacting with a Machine obtained via the factory
 * method {@link li.cil.oc.api.Machine#create(Owner, Class)}. It is primarily
 * intended to be used by custom {@link Architecture}
 * implementations.
 */
@SuppressWarnings("unused")
public interface Machine extends ManagedEnvironment, Context {
    /**
     * The owner of the machine, usually a tile entity hosting the machine.
     *
     * @return the owner of the machine.
     */
    Owner owner();

    /**
     * The underlying architecture of the machine.
     * <p/>
     * This is what actually evaluates code running on the machine, where the
     * machine class itself serves as a scheduler.
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
     * Retrieves the docstring for the specified method of the specified
     * component. This is the string set in a method's {@link Callback}
     * annotation.
     *
     * @param address the address of the component.
     * @param method  the name of the method.
     * @return the docstring for that method.
     */
    String documentation(String address, String method);

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

    /**
     * Retrieves the docstring for the specified method of the specified
     * value. This is the string set in a method's {@link Callback}
     * annotation.
     *
     * @param value  the value.
     * @param method the name of the method.
     * @return the docstring for that method.
     */
    String documentation(Value value, String method);

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
