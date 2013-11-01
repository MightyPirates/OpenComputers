package li.cil.oc.api.network;

/**
 * This interface provides access to arguments passed to a {@link LuaCallback}.
 * <p/>
 * It allows checking for the presence of arguments in a uniform manner, taking
 * care of proper type checking based on what can be passed along by Lua.
 */
public interface Arguments extends Iterable<Object> {
    /**
     * The total number of arguments that were passed to the function.
     */
    int count();

    /**
     * Get whatever is at the specified index.
     * <p/>
     * Throws an error if there are too few arguments.
     * <p/>
     * The returned object will be one of the following, based on the conversion
     * performed internally:
     * <ul>
     * <li><tt>null</tt> if the Lua value was <tt>nil</tt>.</li>
     * <li><tt>java.lang.Boolean</tt> if the Lua value was a boolean.</li>
     * <li><tt>java.lang.Double</tt> if the Lua value was a number.</li>
     * <li><tt>byte[]</tt> if the Lua value was a string.</li>
     * </ul>
     *
     * @param index the index from which to get the argument.
     * @return the raw value at that index.
     */
    Object checkAny(int index);

    boolean checkBoolean(int index);

    double checkDouble(int index);

    int checkInteger(int index);

    String checkString(int index);

    byte[] checkByteArray(int index);
}
