package li.cil.oc.api;

import li.cil.oc.api.detail.MachineAPI;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Owner;

/**
 * This API is intended for people who would like to implement custom computer
 * blocks or anything else hosting a computer.
 * <p/>
 * It also allows registering new {@link li.cil.oc.api.machine.Architecture}s,
 * which are implementations of specific languages (e.g. assembler). The built-
 * in ones are available as static fields in this class.
 * <p/>
 * Note that these methods should <em>not</em> be called in the pre-init phase,
 * since the {@link #instance} may not have been initialized at that time. Only
 * start calling these methods in the init phase or later.
 */
public final class Machine {
    /**
     * The built-in architecture that uses the native Lua implementation.
     */
    public static Class<? extends Architecture> NativeLuaArchitecture = null;

    /**
     * The built-in architecture that uses the LuaJ Lua implementation.
     */
    public static Class<? extends Architecture> JavaLuaArchitecture = null;

    void add(Class<? extends Architecture> architecture) {
        if (instance != null) instance.add(architecture);
    }

    li.cil.oc.server.component.machine.Machine create(Owner owner, Class<? extends Architecture> architecture) {
        if (instance != null) return instance.create(owner, architecture);
        return null;
    }

    // ----------------------------------------------------------------------- //

    private Machine() {
    }

    public static MachineAPI instance = null;
}
