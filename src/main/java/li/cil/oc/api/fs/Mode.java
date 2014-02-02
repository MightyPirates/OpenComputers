package li.cil.oc.api.fs;

/**
 * Possible file modes.
 * <p/>
 * This is used when opening files from a {@link FileSystem}.
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
    Append
}
