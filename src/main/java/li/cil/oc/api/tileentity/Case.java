package li.cil.oc.api.tileentity;

import li.cil.oc.api.driver.EnvironmentHost;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Environment;
import net.minecraft.inventory.IInventory;

/**
 * This interface is implemented as a marker by computer cases.
 * <p/>
 * The only purpose is to allow identifying tile entities as computer cases
 * via the API, i.e. without having to link against internal classes. This
 * also means that <em>you should not implement this</em>.
 */
public interface Case extends Environment, EnvironmentHost, Rotatable, Colored, IInventory {
    /**
     * The machine currently hosted by this computer case.
     * <p/>
     * This can be <tt>null</tt>, for example when there is no CPU installed
     * in the computer case.
     *
     * @return the machine currently hosted by the computer case.
     */
    Machine machine();
}