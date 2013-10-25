package li.cil.oc.api;

import dan200.computer.api.IMount;
import dan200.computer.api.IWritableMount;
import li.cil.oc.api.detail.FileSystemAPI;
import li.cil.oc.api.network.Node;

final public class FileSystem {
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
     * @param clazz  the class whose containing JAR to wrap.
     * @param domain the mod domain, usually its name.
     * @param root   an optional subdirectory.
     * @return a file system wrapping the specified folder.
     */
    public static li.cil.oc.api.fs.FileSystem fromClass(Class<?> clazz, String domain, String root) {
        if (instance != null) return instance.fromClass(clazz, domain, root);
        return null;
    }

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
    public static li.cil.oc.api.fs.FileSystem fromSaveDirectory(String root, long capacity, boolean buffered) {
        if (instance != null) return instance.fromSaveDirectory(root, capacity, buffered);
        return null;
    }

    public static li.cil.oc.api.fs.FileSystem fromSaveDirectory(String root, long capacity) {
        return fromSaveDirectory(root, capacity, true);
    }

    /**
     * Creates a new *writable* file system that resides in memory.
     * <p/>
     * Any contents created and written on this file system will be lost when
     * the node is removed from the network.
     * <p/>
     * This is used for computers' `/tmp` mount, for example.
     *
     * @param capacity the capacity of the file system.
     * @return a file system residing in memory.
     */
    public static li.cil.oc.api.fs.FileSystem fromMemory(long capacity) {
        if (instance != null) return instance.fromMemory(capacity);
        return null;
    }

    /**
     * Creates a new file system based on a read-only ComputerCraft mount.
     *
     * @param mount the mount to wrap with a file system.
     * @return a file system wrapping the specified mount.
     */
    public static li.cil.oc.api.fs.FileSystem fromComputerCraft(IMount mount) {
        if (instance != null) return instance.fromComputerCraft(mount);
        return null;
    }

    /**
     * Creates a new file system based on a read-write ComputerCraft mount.
     *
     * @param mount the mount to wrap with a file system.
     * @return a file system wrapping the specified mount.
     */
    public static li.cil.oc.api.fs.FileSystem fromComputerCraft(IWritableMount mount) {
        if (instance != null) return instance.fromComputerCraft(mount);
        return null;
    }

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
    public static Node asNode(li.cil.oc.api.fs.FileSystem fileSystem) {
        if (instance != null) return instance.asNode(fileSystem);
        return null;
    }

    // ----------------------------------------------------------------------- //

    private FileSystem() {
    }

    /**
     * Initialized in pre-init.
     */
    public static FileSystemAPI instance = null;
}