package li.cil.oc.api

import li.cil.oc.api.detail.FileSystemAPI
import li.cil.oc.api.fs.{Mode, Handle}
import li.cil.oc.api.network.Node

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
 */
trait FileSystem extends Persistable {
  /**
   * Tests if a file or directory exists at the specified path.
   * <p/>
   * This function should never throw.
   *
   * @param path the path to check at.
   * @return true if the path points to a file or directory; false otherwise.
   */
  def exists(path: String): Boolean

  /**
   * Gets the size of a file or folder.
   * <p/>
   * For files this should return the actual length of the file, in bytes. For
   * folders it is recommended to return some value larger than zero to
   * simulate the node cost (in particular for writable file systems to avoid
   * users spawning an infinite amount of folders).
   * <p/>
   * If the path is invalid this should return zero. It should never throw.
   *
   * @param path the path to get the size for.
   * @return the size of the object at the specified path.
   */
  def size(path: String): Long

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
  def isDirectory(path: String): Boolean

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
   * @return an array with the names of all objects in that folder; None if
   *         the specified object does not exist or is not a folder.
   */
  def list(path: String): Option[Array[String]]

  // ----------------------------------------------------------------------- //

  /**
   * Deletes a file or folder.
   * <p/>
   * This is only available for writable file systems. For read-only systems
   * it should just always return false.
   * <p/>
   * If the specified object is a folder, it and all its contents should be
   * deleted recursively.
   *
   * @param path the path to the file or folder to delete.
   * @return true if something was deleted; false otherwise.
   */
  def remove(path: String): Boolean = false

  /**
   * Moves / renames a file or folder.
   * <p/>
   * This is only available for writable file systems. For read-only systems
   * it should just always return false.
   *
   * @param from the name of the file or folder to move.
   * @param to the location to move the file or folder to.
   * @return true if the object was renamed; false otherwise.
   * @throws FileNotFoundException if the source is not a file or folder.
   */
  def rename(from: String, to: String): Boolean = false

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
   * @throws FileNotFoundException if the object is not a file, or the file
   *                               cannot be opened in the specified mode.
   */
  def open(path: String, mode: Mode.Value): Int

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
  def file(handle: Int): Option[Handle]

  /**
   * Called when the file system is deconstructed.
   * <p/>
   * This should close any open real file handles (e.g. all open I/O streams)
   * and clear any other internal state.
   * <p/>
   * When the filesystem is made available as a network node created via
   * `FileSystem.asNode` this will be called whenever the node is disconnected
   * from its network. If the node was used to represent an item (which will
   * be the usual use-case, I imagine) this means the item was removed from
   * its container (e.g. hard drive from a computer) or the container was
   * unloaded.
   */
  def close()
}

object FileSystem extends FileSystemAPI {
  /**
   * Creates a new file system based on the location of a class.
   * <p/>
   * This can be used to wrap a folder in the assets folder of your mod's JAR.
   * The actual path is built like this: `"/assets/" + domain + "/" + root`.
   * <p/>
   * If the class is located in a JAR file, this will create a read-only file
   * system based on that JAR file. If the class file is located in the native
   * file system, this will create a read-only file system first trying from
   * the actual location of the class file, and failing that by searching the
   * class path (i.e. it'll look for a path constructed as described above).
   * <p/>
   * If the specified path cannot be located, the creation fails and this
   * returns `None`.
   *
   * @param clazz the class whose containing JAR to wrap.
   * @param domain the mod domain, usually its name.
   * @param root an optional subdirectory.
   * @return a file system wrapping the specified folder.
   */
  def fromClass(clazz: Class[_], domain: String, root: String = "") =
    instance.fold(None: Option[FileSystem])(_.fromClass(clazz, domain, root))

  /**
   * Creates a new *writable* file system in the save folder.
   * <p/>
   * This will create a folder, if necessary, and create a writable virtual
   * file system based in that folder. The actual path is based in a sub-
   * folder of the save folder. The actual path is e.g. built like this:
   * `"saves/" + WORLD_NAME + "/opencomputers/" + root`. Where the first
   * part may differ, in particular for servers. But you get the idea.
   * <p/>
   * Usually the name will be the name of the node used to represent the
   * file system.
   *
   * @param root the name of the file system.
   * @return
   */
  def fromSaveDir(root: String) =
    instance.fold(None: Option[FileSystem])(_.fromSaveDir(root))

  /**
   * Creates a network node that makes the specified file system available via
   * the common file system driver.
   * <p/>
   * This can be useful for providing some data if you don't wish to implement
   * your own driver. Which will probably be most of the time. If you need
   * more control over the node, implement your own, and connect this one to
   * it. In that case you will have to forward any disk driver messages to the
   * node, though, since it's visibility is neighbors only.
   *
   * @param fileSystem the file system to wrap.
   * @return the network node wrapping the file system.
   */
  def asNode(fileSystem: FileSystem) =
    instance.fold(None: Option[Node])(_.asNode(fileSystem))

  // ----------------------------------------------------------------------- //

  /** Initialized in pre-init. */
  private[oc] var instance: Option[FileSystemAPI] = None
}