package li.cil.oc.api.fs;

import li.cil.oc.api.Persistable;

import java.io.FileNotFoundException;

/**
 * Interface for file system driver compatible file systems.
 * <p/>
 * To create a file system from a JAR file or folder (in read-only mode; for
 * example the one containing your mod) use `Filesystem.fromClass`, providing
 * a class from your mod as the first parameter.
 * <p/>
 * To get a network node wrapping a file system and using the default file
 * system driver, use `Filesystem.asNode`.
 * <p/>
 * Alternatively to using the factory methods for file systems in `Filesystem`
 * you are free to implement this interface yourself.
 * <p/>
 * Note that all paths passed here are assumed to be absolute in the underlying
 * file system implementation, meaning they do not contain any "." or "..", and
 * are relative to the root of the file system. When wrapping a file system in
 * a node with the provided factory function this is automatically ensured. If
 * you call any of the functions of a file system directly it is your
 * responsibility to ensure the path has been cleaned up.
 */
public interface FileSystem extends Persistable {
    /**
     * The total storage capacity of the file system, in bytes.
     * <p/>
     * For read-only systems this should return zero, for writable file systems
     * that do not enforce a storage limit this should be a negative value.
     *
     * @return the total storage space of this file system.
     */
    long spaceTotal();

    /**
     * The used storage capacity of the file system, in bytes.
     * <p/>
     * For read-only systems this should return zero.
     *
     * @return the used storage space of this file system.
     */
    long spaceUsed();

    // ----------------------------------------------------------------------- //

    /**
     * Tests if a file or directory exists at the specified path.
     * <p/>
     * This function should never throw.
     *
     * @param path the path to check at.
     * @return true if the path points to a file or directory; false otherwise.
     */
    boolean exists(String path);

    /**
     * Gets the size of a file.
     * <p/>
     * For files this should return the actual length of the file, in bytes. For
     * folders this should return zero.
     * <p/>
     * If the path is invalid this should return zero. It should never throw.
     *
     * @param path the path to get the size for.
     * @return the size of the object at the specified path.
     */
    long size(String path);

    /**
     * Tests whether the object at the specified path is a directory.
     * <p/>
     * If the path is invalid (i.e. there is neither a file nor a directory at
     * the specified location) this should also return false. It should never
     * throw.
     *
     * @param path the path to the object to check.
     * @return true if the object is a directory; false otherwise.
     */
    boolean isDirectory(String path);

    /**
     * Gets the timestamp of the last time the file at the specified path was
     * written to.
     * <p/>
     * For folders this should be the time they were created.
     * <p/>
     * If the path is invalid (i.e. there is neither a file nor a directory at
     * the specified location) this should return zero. It should never throw.
     * <p/>
     * For read-only systems this may be zero for all queries.
     *
     * @param path the path to the object to get the last modified time of.
     * @return the time the object was last modified.
     */
    long lastModified(String path);

    /**
     * Gets a list of all items in the specified folder.
     * <p/>
     * This must return the actual object names in the specified parent folder,
     * not their full path. For example, for a file at `/home/test`, when doing
     * `list("/home/")` this should return `["test"]`, *not* `["/home/test"]`.
     * <p/>
     * Sub-folders should be returned with a trailing slash, to indicate that
     * they are folders. This is primarily intended to avoid Lua programs having
     * to check which of the entries are folders via calling `isDirectory`, which
     * would be excruciatingly slow (since each call takes one game tick).
     * <p/>
     * If the folder is empty this should return an empty array.
     *
     * @param path the path to the folder to get the contents of.
     * @return an array with the names of all objects in that folder; `null` if
     *         the specified object does not exist or is not a folder.
     */
    String[] list(String path);

    // ----------------------------------------------------------------------- //

    /**
     * Deletes a file or folder.
     * <p/>
     * This only has to support deleting single files and empty folders. If a
     * directory is non-empty this may return false. If the target object does
     * not exists it should return false.
     * <p/>
     * This is only available for writable file systems. For read-only systems
     * it should just always return false.
     *
     * @param path the path to the object to delete.
     * @return true if the object was successfully deleted; false otherwise.
     */
    boolean delete(String path);

    /**
     * Create the specified directory.
     * <p/>
     * This should always only create a single directory. If the parent directory
     * does not exists it should return false. If the target object already
     * exists it should also return false.
     * <p/>
     * This is only available for writable file systems. For read-only systems
     * it should just always return false.
     *
     * @param path the path to the directory to create.
     * @return true if the directory was created; false otherwise.
     */
    boolean makeDirectory(String path);

    /**
     * Moves / renames a file or folder.
     * <p/>
     * This is only available for writable file systems. For read-only systems
     * it should just always return false.
     *
     * @param from the name of the file or folder to move.
     * @param to   the location to move the file or folder to.
     * @return true if the object was renamed; false otherwise.
     * @throws FileNotFoundException if the source is not a file or folder.
     */
    boolean rename(String from, String to) throws FileNotFoundException;

    /**
     * Sets the time a file or folder was supposedly last modified.
     * <p/>
     * This is not available to the Lua side via the file system driver. It is
     * intended to be used when initializing a file system to a set of known
     * modification times (for example, this is used when creating a virtual
     * file system from a set of real files).
     *
     * @param path the path of the object for which to set the modification time.
     * @param time the time the object was supposedly last modified.
     * @return whether the modification time was adjusted.
     */
    boolean setLastModified(String path, long time);

    // ----------------------------------------------------------------------- //

    /**
     * Opens a file for reading or writing.
     * <p/>
     * This should create some internal handle to the file, based on the mode
     * specified. A unique ID corresponding to that handle should be returned.
     * This ID can be used in `file` to get an abstract wrapper for the handle,
     * and to allow interaction with the file.
     * <p/>
     * It is the responsibility of the file system to restore all handles to
     * their previous state when it is reloaded (game loaded for example).
     *
     * @param path the path to the file to open.
     * @param mode the mode in which to open the file.
     * @return the handle to the opened file.
     * @throws FileNotFoundException if the object is not a file, or
     *                               the file cannot be opened in the
     *                               specified mode.
     */
    int open(String path, Mode mode) throws FileNotFoundException;

    /**
     * Gets a wrapper for a file previously opened using `open`.
     * <p/>
     * The wrapper allows interaction with the underlying file (stream) based
     * on the mode it was opened in. See the `File` interface for more details.
     * <p/>
     * If there is no such handle, this should return `None`, but never throw.
     *
     * @param handle the ID of the handle to get the wrapper for.
     * @return the wrapper for that handle ID; None if the ID is invalid.
     */
    Handle file(int handle);

    /**
     * Called when the file system is close.
     * <p/>
     * This should close any open real file handles (e.g. all open I/O streams),
     * but keep any internal state that may have to be persisted, for example
     * for floppy disks (which are removed before they are saved so they don't
     * save any open handles).
     * <p/>
     * When the filesystem is made available as a network node created via
     * `FileSystem.asNode` this will be called whenever the node is disconnected
     * from its network. If the node was used to represent an item (which will
     * be the usual use-case, I imagine) this means the item was removed from
     * its container (e.g. hard drive from a computer) or the container was
     * unloaded.
     */
    void close();
}