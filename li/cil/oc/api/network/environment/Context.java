package li.cil.oc.api.network.environment;

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
