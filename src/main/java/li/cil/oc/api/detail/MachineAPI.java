package li.cil.oc.api.detail;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Owner;
import li.cil.oc.server.component.machine.Machine;

public interface MachineAPI {
    void add(Class<? extends Architecture> architecture);

    Iterable<Class<? extends Architecture>> architectures();

    Machine create(Owner owner, Class<? extends Architecture> architecture);
}
