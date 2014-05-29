package li.cil.oc.api.detail;

import li.cil.oc.api.driver.Container;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.ManagedEnvironment;

public interface FileSystemAPI {
    /**
     * Creates a new file system based on the location of a class.
     * <p/>
     * This can be used to wrap a folder in the assets folder of your mod's JAR.
     * The actual path is built like this:
     * <pre>"/assets/" + domain + "/" + root</pre>
     * <p/>
     * If the class is located in a JAR file, this will create a read-only file
     * system based on that JAR file. If the class file is located in the native
     * file system, this will create a read-only file system first trying from
     * the actual location of the class file, and failing that by searching the
     * class path (i.e. it'll look for a path constructed as described above).
     * <p/>
     * If the specified path cannot be located, the creation fails and this
     * returns <tt>null</tt>.
     *
     * @param clazz  the class whose containing JAR to wrap.
     * @param domain the domain, usually your mod's ID.
     * @param root   an optional subdirectory.
     * @return a file system wrapping the specified folder.
     */
    FileSystem fromClass(Class<?> clazz, String domain, String root);

    /**
     * Creates a new <em>writable</em> file system in the save folder.
     * <p/>
     * This will create a folder, if necessary, and create a writable virtual
     * file system based in that folder. The actual path is based in a sub-
     * folder of the save folder. The actual path is built like this:
     * <pre>"saves/" + WORLD_NAME + "/opencomputers/" + root</pre>
     * The first part may differ, in particular for servers.
     * <p/>
     * Usually the name will be the address of the node used to represent the
     * file system.
     * <p/>
     * Note that by default file systems are "buffered", meaning that any
     * changes made to them are only saved to disk when the world is saved. This
     * ensured that the file system contents do not go "out of sync" when the
     * game crashes, but introduces additional memory overhead, since all files
     * in the file system have to be kept in memory.
     *
     * @param root     the name of the file system.
     * @param capacity the amount of space in bytes to allow being used.
     * @param buffered whether data should only be written to disk when saving.
     * @return a file system wrapping the specified folder.
     */
    FileSystem fromSaveDirectory(String root, long capacity, boolean buffered);

    /**
     * Creates a new <em>writable</em> file system that resides in memory.
     * <p/>
     * Any contents created and written on this file system will be lost when
     * the node is removed from the network.
     * <p/>
     * This is used for computers' <tt>/tmp</tt> mount, for example.
     *
     * @param capacity the capacity of the file system.
     * @return a file system residing in memory.
     */
    FileSystem fromMemory(long capacity);

    /**
     * Creates a new file system based on a read-only ComputerCraft mount.
     * <p/>
     * This supports read-only and writable mounts from either CC 1.5x or
     * CC 1.6x. The argument is kept untyped to avoid having the OC API
     * depend on the CC API.
     * <p/>
     * If the passed type is unsupported, this will return <tt>null</tt>.
     *
     * @param mount the mount to wrap with a file system.
     * @return a file system wrapping the specified mount.
     */
    FileSystem fromComputerCraft(Object mount);

    /**
     * Creates a network node that makes the specified file system available via
     * the common file system driver.
     * <p/>
     * This can be useful for providing some data if you don't wish to implement
     * your own driver. Which will probably be most of the time. If you need
     * more control over the node, implement your own, and connect this one to
     * it. In that case you will have to forward any disk driver messages to the
     * node, though.
     * <p/>
     * The container parameter is used to give the file system some physical
     * relation to the world, for example this is used by hard drives to send
     * the disk event notifications to the client that are used to play disk
     * access sounds.
     * <p/>
     * The container may be <tt>null</tt>, if no such context can be provided.
     *
     * @param fileSystem the file system to wrap.
     * @param label      the label of the file system.
     * @param container  the tile entity containing the file system.
     * @return the network node wrapping the file system.
     */
    ManagedEnvironment asManagedEnvironment(FileSystem fileSystem, Label label, Container container);

    /**
     * Like {@link #asManagedEnvironment(li.cil.oc.api.fs.FileSystem, Label, Container)},
     * but creates a read-only label initialized to the specified value.
     *
     * @param fileSystem the file system to wrap.
     * @param label      the read-only label of the file system.
     * @return the network node wrapping the file system.
     */
    ManagedEnvironment asManagedEnvironment(FileSystem fileSystem, String label, Container container);

    /**
     * Like {@link #asManagedEnvironment(li.cil.oc.api.fs.FileSystem, Label, Container)},
     * but does not provide a container.
     *
     * @param fileSystem the file system to wrap.
     * @param label      the label of the file system.
     * @return the network node wrapping the file system.
     */
    ManagedEnvironment asManagedEnvironment(FileSystem fileSystem, Label label);

    /**
     * Like {@link #asManagedEnvironment(li.cil.oc.api.fs.FileSystem, Label)},
     * but creates a read-only label initialized to the specified value.
     *
     * @param fileSystem the file system to wrap.
     * @param label      the read-only label of the file system.
     * @return the network node wrapping the file system.
     */
    ManagedEnvironment asManagedEnvironment(FileSystem fileSystem, String label);

    /**
     * Like {@link #asManagedEnvironment(li.cil.oc.api.fs.FileSystem, Label)},
     * but creates an unlabeled file system (i.e. the label can neither be read
     * nor written).
     *
     * @param fileSystem the file system to wrap.
     * @return the network node wrapping the file system.
     */
    ManagedEnvironment asManagedEnvironment(FileSystem fileSystem);
}