package li.cil.oc.api.tileentity;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.inventory.IInventory;

/**
 * This interface is implemented as a marker by server racks.
 * <p/>
 * The only purpose is to allow identifying tile entities as server racks
 * via the API, i.e. without having to link against internal classes. This
 * also means that <em>you should not implement this</em>.
 */
public interface ServerRack extends Environment, SidedEnvironment, Rotatable, IInventory {
    /**
     * The machine currently hosted by the server in the specified slot.
     * <p/>
     * This can be <tt>null</tt>, for example when there is no CPU installed
     * in the server in that slot, or there is no server in that slot.
     *
     * @return the machine currently hosted in the specified slot.
     */
    Machine machine(int slot);
}