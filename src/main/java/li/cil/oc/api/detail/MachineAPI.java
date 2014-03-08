package li.cil.oc.api.detail;

import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Owner;

import java.util.concurrent.Callable;

public interface MachineAPI {
    /**
     * Register an architecture that can be used to create new machines.
     * <p/>
     * Note that although registration is optional, it is strongly recommended
     * to allow {@link #architectures()} to be useful.
     *
     * @param architecture the architecture to register.
     * @throws IllegalArgumentException if the specified architecture is invalid.
     */
    void add(Class<? extends Architecture> architecture);

    /**
     * Adds a file system to use for the composite file system that is made
     * available as the ROM to each machine of the specified architecture.
     * <p/>
     * File systems are merged in a overshadowing manner, i.e. if files or
     * directories with the same path exist in multiple file systems, only the
     * one that was last registered will be used. In other words, added file
     * systems override previously existed file systems on a file-by-file
     * level (where files can override folders and vice versa).
     *
     * @param architecture the the architecture for which to add to the ROM.
     * @param resource     the file system to add to the ROM.
     * @param name         a unique name for the file system. This is required
     *                     to allow for deterministic loading/saving of the
     *                     file system (open file handles). This value must be
     *                     unique for each file system in the resource set.
     * @throws java.lang.IllegalArgumentException if the name is not unique or
     *                                            the architecture is invalid.
     */
    void addRomResource(Class<? extends Architecture> architecture, Callable<FileSystem> resource, String name);

    /**
     * A list of all <em>registered</em> architectures.
     * <p/>
     * Note that registration is optional, although automatic when calling
     * {@link #create(li.cil.oc.api.machine.Owner, Class)} with a not yet
     * registered architecture. What this means is that unless a mod providing
     * a custom architecture also registers it, you may not see it in this list
     * until it also created a new machine using that architecture.
     */
    Iterable<Class<? extends Architecture>> architectures();

    /**
     * Creates a new machine using the specified architecture.
     * <p/>
     * You are responsible for calling update and save / load functions on the
     * machine for it to work correctly.
     *
     * @param owner        the owner object of the machine, providing context.
     * @param architecture the architecture to use for running code on the machine.
     * @return the newly created machine.
     * @throws IllegalArgumentException if the specified architecture is invalid.
     */
    Machine create(Owner owner, Class<? extends Architecture> architecture);
}
