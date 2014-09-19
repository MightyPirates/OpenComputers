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
 * convenience feature to make architectures usable via the built-in CPUs.
 * <p/>
 * Note that these methods should <em>not</em> be called in the pre-init phase,
 * since the {@link #instance} may not have been initialized at that time. Only
 * start calling these methods in the init phase or later.
 */
public final class Machine {
    /**
     * Register an architecture that can be used to create new machines.
     * <p/>
     * Registering an architecture will make it possible to configure CPUs to
     * run that architecture. This allows providing architectures without
     * implementing a custom CPU item.
     *
     * @param architecture the architecture to register.
     */
    public static void add(Class<? extends Architecture> architecture) {
        if (instance != null)
            instance.add(architecture);
    }

    /**
     * A list of all <em>registered</em> architectures.
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
