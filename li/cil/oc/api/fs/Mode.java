package li.cil.oc.api.fs;

/**
 * Possible file modes.
 * <p/>
 * This is used when opening files from a `FileSystem`.
 */
public enum Mode {
    /**
     * Open a file in reading mode.
     */
    Read,

    /**
     * Open a file in writing mode, overwriting existing contents.
     */
    Write,

    /**
     * Open a file in append mode, writing new data after existing contents.
     */
    Append;

    /**
     * Parses a mode from a string.
     *
     * @param value the string to parse.
     * @return the mode the string represents.
     * @throws IllegalArgumentException if the string cannot be parsed to a mode.
     */
    public static Mode parse(String value) {
        if ("r".equals(value) || "rb".equals(value))
            return Read;
        if ("w".equals(value) || "wb".equals(value))
            return Write;
        if ("a".equals(value) || "ab".equals(value))
            return Append;
        throw new IllegalArgumentException("unsupported mode");
    }
}
