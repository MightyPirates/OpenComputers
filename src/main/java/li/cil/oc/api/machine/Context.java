package li.cil.oc.api.machine;

import li.cil.oc.api.network.Node;

/**
 * This is used to provide some context to {@link li.cil.oc.api.machine.Callback}s, i.e. the
 * computer from which the callback was called.
 */
public interface Context {
    /**
     * The node through which the computer is attached to the component network.
     */
    Node node();

    /**
     * Tests whether a player is allowed to use the computer.
     * <p/>
     * If enabled in the server's configuration, computers can be owned by
     * players. This means that only players that are in a computer's user list
     * may interact with it, i.e. only players in the user list may:
     * <ul>
     * <li>Trigger input via a keyboard.</li>
     * <li>Change the computer's inventory.</li>
     * <li>Break the computer block.</li>
     * </ul>
     * <p/>
     * There are three exceptions to this rule:
     * <ul>
     * <li>Operators are <em>always</em> allowed the above actions.</li>
     * <li>If the user list is <em>empty</em> then <em>all</em> players are
     * allowed the above actions.</li>
     * <li>In single player mode the player is always allowed the above
     * actions.</li>
     * </ul>
     * <p/>
     * Use this to check whether you should signal something to the computer,
     * for example. Note that for signals triggered via network messages there
     * is a <tt>computer.checked_signal</tt> message, that expects an
     * <tt>EntityPlayer</tt> as the first argument and performs this check
     * before pushing the signal.
     *
     * @param player the name of the player to check for.
     * @return whether the player with the specified name may use the computer.
     */
    boolean canInteract(String player);

    /**
     * Whether the computer is currently in a running state, i.e. it is neither
     * paused, stopping or stopped.
     * <p/>
     * The computer thread may or may not be running while the computer is in
     * this state. The computer will accept signals while in this state.
     */
    boolean isRunning();

    /**
     * Whether the computer is currently in a paused state.
     * <p/>
     * The computer thread is not running while the computer is in this state.
     * The computer will accept signals while in this state.
     */
    boolean isPaused();

    /**
     * Starts the computer.
     * <p/>
     * The computer will enter a <em>starting</em> state, in which it will start
     * accepting signals. The computer will start executing in the next server
     * tick.
     * <p/>
     * If this is called while the computer is in a paused state it will set the
     * remaining pause time to zero, but it will <em>not</em> immediately resume
     * the computer. The computer will continue with what it did before it was
     * paused in the next server tick.
     * <p/>
     * If this is called while the computer is in a non-paused and non-stopped
     * state it will do nothing and return <tt>false</tt>.
     *
     * @return <tt>true</tt> if the computer switched to a running state.
     */
    boolean start();

    /**
     * Pauses the computer for the specified duration.
     * <p/>
     * If this is called from a <em>direct</em> callback the computer will only
     * pause after the current task has completed, possibly leading to no pause
     * at all. If this is called from a <em>non-direct</em> callback the
     * computer will be paused for the specified duration before the call
     * returns. Use this to add artificial delays, e.g. for expensive or
     * powerful operations (say, scanning blocks surrounding a computer).
     * <p/>
     * <b>Important</b>: if this is called from the <em>server thread</em> while
     * the executor thread is running this will <em>block</em> until the
     * computer finishes its current task. The pause will be applied after that.
     * This is usually a bad thing to do, since it may lag the game, but can be
     * handy to synchronize the computer thread to the server thread. For
     * example, this is used when saving screens, which are controlled mostly
     * via direct callbacks.<br/>
     * <b>However</b>, if the computer is already in a paused state
     * and the call would not lead to a longer pause this will immediately
     * return <tt>false</tt>, <em>without</em> blocking.
     * <p/>
     * Note that the computer still accepts signals while in paused state, so
     * it is generally better to avoid long pauses, to avoid a signal queue
     * overflow, which would lead to some signals being dropped.
     * <p/>
     * Also note that the time left to spend paused is stored in game ticks, so
     * the time resolution is actually quite limited.
     * <p/>
     * If this is called while the computer is in a paused, stopping or stopped
     * state this will do nothing and return <tt>false</tt>.
     *
     * @param seconds the number of seconds to pause the computer for.
     * @return <tt>true</tt> if the computer switched to the paused state.
     */
    boolean pause(double seconds);

    /**
     * Stops the computer.
     * <p/>
     * The computer will enter a <em>stopping</em> state, in which it will not
     * accept new signals. It will be fully stopped in the next server tick. It
     * is not possible to return to a running state from a stopping state. If
     * start is called while in a stopping state the computer will be rebooted.
     * <p/>
     * If this is called from a callback, the callback will still finish, but
     * its result will be discarded. If this is called from the server thread
     * while the executor thread is running the computer in the background, it
     * will finish its current work and the computer will be stopped in some
     * future server tick after it has completed.
     * <p/>
     * If this is called while the computer is in a stopping or stopped state
     * this will do nothing and return <tt>false</tt>.
     *
     * @return <tt>true</tt> if the computer switched to the stopping state.
     */
    boolean stop();

    /**
     * This method allows dynamic costs for direct calls.
     * <p/>
     * It will update the budget for direct calls in the current context, and
     * throw a {@link LimitReachedException} that should <em>not</em> be caught
     * by the callback function. It will be handled in the calling code and
     * take care of switching states as necessary.
     * <p/>
     * Call this from a method with <code>@Callback(direct = true)</code> and
     * no <tt>limit</tt> set to use dynamic costs. If a limit is set, it will
     * always be deduced from the budget in addition to this.
     * <p/>
     * When called from a non-direct / synchronous callback this does nothing.
     *
     * @param callCost the cost of the direct call being performed.
     */
    void consumeCallBudget(double callCost);

    /**
     * Push a signal into the computer.
     * <p/>
     * Signals are processed sequentially by the computer, and are queued in a
     * queue with limited length. If the queue is full and the signal could not
     * be pushed this will return <tt>false</tt>.
     * <p/>
     * Note that only a limited amount of types is supported for arguments:
     * <ul>
     * <li><tt>null</tt> and Scala's <tt>Unit</tt> and <tt>None</tt> (all appear
     * as <tt>nil</tt> on the Lua side, for example)</li>
     * <li>Boolean values.</li>
     * <li>Numeric types (byte, short, int, long, float, double).</li>
     * <li>Strings.</li>
     * <li>Byte arrays (which appear as strings on the Lua side, e.g.).</li>
     * <li>Maps if and only if both keys and values are strings.</li>
     * <li>NBTTagCompounds.</li>
     * </ul>
     * If an unsupported type is specified the method will enqueue nothing
     * instead, resulting in a <tt>nil</tt> on the Lua side, e.g., and log a
     * warning.
     *
     * @param name the name of the signal to push.
     * @param args additional arguments to pass along with the signal.
     * @return <tt>true</tt> if the signal was queued; <tt>false</tt> otherwise.
     */
    boolean signal(String name, Object... args);
}
