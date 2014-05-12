package li.cil.oc.api;

import li.cil.oc.api.detail.MachineAPI;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Owner;

import java.util.Collections;

/**
 * This API is intended for people who would like to implement custom computer
 * blocks or anything else hosting a computer.
 * <p/>
 * It also allows registering new {@link li.cil.oc.api.machine.Architecture}s,
 * which are implementations of specific languages (e.g. assembler). The built-
 * in ones are available as static fields in this class.
 * <p/>
 * Note that registration of architectures is optional and only intended as a
 * convenience feature to provide mods with a way to iterate available
 * architectures using {@link #architectures()}. Also note that any architecture
 * passed to {@link #create(li.cil.oc.api.machine.Owner, Class)} that has not
 * yet been registered will automatically be registered.
 * <p/>
 * Note that these methods should <em>not</em> be called in the pre-init phase,
 * since the {@link #instance} may not have been initialized at that time. Only
 * start calling these methods in the init phase or later.
 */
public final class Machine {
    /**
     * Register an architecture that can be used to create new machines.
     * <p/>
     * Note that although registration is optional, it is strongly recommended
     * to allow {@link #architectures()} to be useful.
     *
     * @param architecture the architecture to register.
     */
    public static void add(Class<? extends Architecture> architecture) {
        if (instance != null)
            instance.add(architecture);
    }

    /**
     * A list of all <em>registered</em> architectures.
     * <p/>
     * Note that registration is optional, although automatic when calling
     * {@link #create(li.cil.oc.api.machine.Owner, Class)} with a not yet
     * registered architecture. What this means is that unless a mod providing
     * a custom architecture also registers it, you may not see it in this list
     * until it also created a new machine using that architecture.
     */
    public static Iterable<Class<? extends Architecture>> architectures() {
        if (instance != null)
            return instance.architectures();
        return Collections.emptyList();
    }

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
    public static li.cil.oc.api.machine.Machine create(Owner owner, Class<? extends Architecture> architecture) {
        if (instance != null)
            return instance.create(owner, architecture);
        return null;
    }

    /**
     * Creates a new machine using the default architecture (Lua).
     * <p/>
     * You are responsible for calling update and save / load functions on the
     * machine for it to work correctly.
     *
     * @param owner the owner object of the machine, providing context.
     * @return the newly created machine.
     */
    public static li.cil.oc.api.machine.Machine create(Owner owner) {
        if (instance != null)
            return instance.create(owner, LuaArchitecture);
        return null;
    }

    // ----------------------------------------------------------------------- //

    private Machine() {
    }

    public static MachineAPI instance = null;

    /**
     * The built-in Lua architecture. This will be set to the native Lua
     * implementation when possible, to the LuaJ fallback, otherwise.
     */
    public static Class<? extends Architecture> LuaArchitecture = null;
}
