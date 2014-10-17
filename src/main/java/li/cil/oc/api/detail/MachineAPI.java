package li.cil.oc.api.detail;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;

import java.util.Collection;

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
     * A list of all <em>registered</em> architectures.
     * <p/>
     * Note that registration is optional, although automatic when calling
     * {@link #create(li.cil.oc.api.machine.MachineHost)} with a not yet
     * registered architecture. What this means is that unless a mod providing
     * a custom architecture also registers it, you may not see it in this list
     * until it also created a new machine using that architecture.
     */
    Collection<Class<? extends Architecture>> architectures();

    /**
     * Creates a new machine for the specified host.
     * <p/>
     * You are responsible for calling update and save / load functions on the
     * machine for it to work correctly.
     *
     * @param host the owner object of the machine, providing context.
     * @return the newly created machine.
     * @throws IllegalArgumentException if the specified architecture is invalid.
     */
    Machine create(MachineHost host);
}
