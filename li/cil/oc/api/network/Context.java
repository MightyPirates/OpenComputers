package li.cil.oc.api.network;

/**
 * This is used to provide some context to {@link LuaCallback}s, i.e. the
 * computer from which the callback was called.
 */
public interface Context {
    /**
     * The network address of the computer that called the function.
     */
    String address();

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
    boolean isUser(String player);

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
     * as <tt>nil</tt> on the Lua side)</li>
     * <li>Boolean values.</li>
     * <li>Numeric types (byte, short, int, long, float, double).</li>
     * <li>Strings.</li>
     * <li>Byte arrays (which appear as strings on the Lua side).</li>
     * </ul>
     * If an unsupported type is specified the method will enqueue nothing
     * instead, resulting in a <tt>nil</tt> on the Lua side, and log a warning.
     *
     * @param name the name of the signal to push.
     * @param args additional arguments to pass along with the signal.
     * @return <tt>true</tt> if the signal was queued; <tt>false</tt> otherwise.
     */
    boolean signal(String name, Object... args);
}
