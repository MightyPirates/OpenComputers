package li.cil.oc.api.machine;

import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;

public interface Machine extends ManagedEnvironment, Context {
    /**
     * The underlying architecture of the machine.
     * <p/>
     * This is what actually evaluates code running on the machine, where the
     * machine class itself serves as a scheduler.
     *
     * @return the architecture of this machine.
     */
    Architecture architecture();

    /**
     * The owner of the machine, usually a tile entity hosting the machine.
     *
     * @return the owner of the machine.
     */
    Owner owner();
}
