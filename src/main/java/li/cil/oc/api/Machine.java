package li.cil.oc.api;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.MachineHost;

import java.util.Collection;
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
 * since the {@link li.cil.oc.api.API#machine} may not have been initialized
 * at that time. Only start calling these methods in the init phase or later.
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
        if (API.machine != null)
            API.machine.add(architecture);
    }

    /**
     * A list of all <em>registered</em> architectures.
     */
    public static Collection<Class<? extends Architecture>> architectures() {
        if (API.machine != null)
            return API.machine.architectures();
        return Collections.emptyList();
    }

    /**
     * Creates a new machine for the specified host.
     * <p/>
     * You are responsible for calling update and save / load functions on the
     * machine for it to work correctly.
     *
     * @param host the owner object of the machine, providing context.
     * @return the newly created machine.
     */
    public static li.cil.oc.api.machine.Machine create(MachineHost host) {
        if (API.machine != null)
            return API.machine.create(host);
        return null;
    }

    // ----------------------------------------------------------------------- //

    private Machine() {
    }

    /**
     * The built-in Lua architecture. This will be set to the native Lua
     * implementation when possible, to the LuaJ fallback, otherwise.
     */
    public static Class<? extends Architecture> LuaArchitecture = null;
}
