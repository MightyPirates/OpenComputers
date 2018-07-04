package li.cil.oc.api;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.ManagedEnvironment;

/**
 * This class provides factory methods for creating file systems that are
 * compatible with the built-in file system driver.
 * <p/>
 * File systems created this way and wrapped in a managed environment via
 * {@link #asManagedEnvironment} or its overloads will appear as
 * <tt>filesystem</tt> components in the component network. Note that the
 * component's visibility is set to <tt>Neighbors</tt> per default. If you wish
 * to change the file system's visibility (e.g. like the disk drive does) you
 * must cast the environment's node to {@link li.cil.oc.api.network.Component}
 * and set the visibility to the desired value.
 * <p/>
 * Note that these methods should <em>not</em> be called in the pre-init phase,
 * since the {@link li.cil.oc.api.API#fileSystem} may not have been initialized
 * at that time. Only start calling these methods in the init phase or later.
 */
public final class FileSystem {
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
    public static li.cil.oc.api.fs.FileSystem fromClass(final Class<?> clazz, final String domain, final String root) {
        if (API.fileSystem != null)
            return API.fileSystem.fromClass(clazz, domain, root);
        return null;
    }

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
    public static li.cil.oc.api.fs.FileSystem fromSaveDirectory(final String root, final long capacity, final boolean buffered) {
        if (API.fileSystem != null)
            return API.fileSystem.fromSaveDirectory(root, capacity, buffered);
        return null;
    }

    /**
     * Same as {@link #fromSaveDirectory(String, long, boolean)} with the
     * <tt>buffered</tt> parameter being true, i.e. will always create a
     * buffered file system.
     *
     * @param root     the name of the file system.
     * @param capacity the amount of space in bytes to allow being used.
     * @return a file system wrapping the specified folder.
     */
    public static li.cil.oc.api.fs.FileSystem fromSaveDirectory(final String root, final long capacity) {
        return fromSaveDirectory(root, capacity, true);
    }

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
    public static li.cil.oc.api.fs.FileSystem fromMemory(final long capacity) {
        if (API.fileSystem != null)
            return API.fileSystem.fromMemory(capacity);
        return null;
    }

    /**
     * Wrap a file system retrieved via one of the <tt>from???</tt> methods to
     * make it read-only.
     *
     * @param fileSystem the file system to wrap.
     * @return the specified file system wrapped to be read-only.
     */
    public static li.cil.oc.api.fs.FileSystem asReadOnly(final li.cil.oc.api.fs.FileSystem fileSystem) {
        if (API.fileSystem != null)
            return API.fileSystem.asReadOnly(fileSystem);
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
     * node, though.
     * <p/>
     * The container parameter is used to give the file system some physical
     * relation to the world, for example this is used by hard drives to send
     * the disk event notifications to the client that are used to play disk
     * access sounds.
     * <p/>
     * The container may be <tt>null</tt>, if no such context can be provided.
     * <p/>
     * The access sound is the name of the sound effect to play when the file
     * system is accessed, for example by listing a directory or reading from
     * a file. It may be <tt>null</tt> to create a silent file system.
     * <p/>
     * The speed multiplier controls how fast read and write operations on the
     * file system are. It must be a value in [1,6], and controls the access
     * speed, with the default being one.
     * For reference, floppies are using the default, hard drives scale with
     * their tiers, i.e. a tier one hard drive uses speed two, tier three uses
     * speed four.
     *
     * @param fileSystem  the file system to wrap.
     * @param label       the label of the file system.
     * @param host        the tile entity containing the file system.
     * @param accessSound the name of the sound effect to play when the file
     *                    system is accessed. This has to be the fully
     *                    qualified resource name, e.g.
     *                    <tt>opencomputers:floppy_access</tt>.
     * @param speed       the speed multiplier for this file system.
     * @return the network node wrapping the file system.
     */
    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final Label label, final EnvironmentHost host, final String accessSound, int speed) {
        if (API.fileSystem != null)
            return API.fileSystem.asManagedEnvironment(fileSystem, label, host, accessSound, speed);
        return null;
    }

    /**
     * Creates a network node that makes the specified file system available via
     * the common file system driver.
     * <p/>
     * Creates a file system with the a read-only label and the specified
     * access sound and file system speed.
     *
     * @param fileSystem  the file system to wrap.
     * @param label       the label of the file system.
     * @param host        the tile entity containing the file system.
     * @param accessSound the name of the sound effect to play when the file
     *                    system is accessed. This has to be the fully
     *                    qualified resource name, e.g.
     *                    <tt>opencomputers:floppy_access</tt>.
     * @param speed       the speed multiplier for this file system.
     * @return the network node wrapping the file system.
     */
    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final String label, final EnvironmentHost host, final String accessSound, int speed) {
        if (API.fileSystem != null)
            return API.fileSystem.asManagedEnvironment(fileSystem, label, host, accessSound, speed);
        return null;
    }

    /**
     * Creates a network node that makes the specified file system available via
     * the common file system driver.
     * <p/>
     * Creates a file system with the specified label and the specified access
     * sound, using the default file system speed.
     *
     * @param fileSystem  the file system to wrap.
     * @param label       the label of the file system.
     * @param host        the tile entity containing the file system.
     * @param accessSound the name of the sound effect to play when the file
     *                    system is accessed. This has to be the fully
     *                    qualified resource name, e.g.
     *                    <tt>opencomputers:floppy_access</tt>.
     * @return the network node wrapping the file system.
     */
    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final Label label, final EnvironmentHost host, final String accessSound) {
        return asManagedEnvironment(fileSystem, label, host, accessSound, 1);
    }

    /**
     * Creates a network node that makes the specified file system available via
     * the common file system driver.
     * <p/>
     * Creates a file system with a read-only label and the specified access
     * sound, using the default file system speed.
     *
     * @param fileSystem  the file system to wrap.
     * @param label       the read-only label of the file system.
     * @param host        the tile entity containing the file system.
     * @param accessSound the name of the sound effect to play when the file
     *                    system is accessed. This has to be the fully
     *                    qualified resource name, e.g.
     *                    <tt>opencomputers:floppy_access</tt>.
     * @return the network node wrapping the file system.
     */
    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final String label, final EnvironmentHost host, final String accessSound) {
        return asManagedEnvironment(fileSystem, label, host, accessSound, 1);
    }

    /**
     * Creates a network node that makes the specified file system available via
     * the common file system driver.
     * <p/>
     * Creates a file system with the specified label, without an environment
     * and access sound, using the default file system speed.
     *
     * @param fileSystem the file system to wrap.
     * @param label      the label of the file system.
     * @return the network node wrapping the file system.
     */
    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final Label label) {
        return asManagedEnvironment(fileSystem, label, null, null, 1);
    }

    /**
     * Creates a network node that makes the specified file system available via
     * the common file system driver.
     * <p/>
     * Creates a file system with a read-only label, without an environment and
     * access sound, using the default file system speed.
     *
     * @param fileSystem the file system to wrap.
     * @param label      the read-only label of the file system.
     * @return the network node wrapping the file system.
     */
    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final String label) {
        return asManagedEnvironment(fileSystem, label, null, null, 1);
    }

    /**
     * Creates a network node that makes the specified file system available via
     * the common file system driver.
     * <p/>
     * Creates an unlabeled file system (i.e. the label can neither be read nor
     * written), without an environment and access sound, using the default
     * file system speed.
     *
     * @param fileSystem the file system to wrap.
     * @return the network node wrapping the file system.
     */
    public static ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem) {
        return asManagedEnvironment(fileSystem, (Label) null, null, null, 1);
    }

    // ----------------------------------------------------------------------- //

    private FileSystem() {
    }
}