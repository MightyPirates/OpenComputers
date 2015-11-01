package li.cil.oc.api.internal;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.inventory.IInventory;

/**
 * This interface is implemented by the rack tile entity.
 * <p/>
 * It particularly allows {@link RackMountable}s installed in the rack to flag
 * themselves as having changed, so their data gets resent to clients.
 * <p/>
 * Server racks <em>do not</em> serve as environment for the computer nodes of
 * servers. That's what the {@link li.cil.oc.api.internal.Server}s are for,
 * which are mountables that can be placed in the rack.
 * <p/>
 * Another purpose is to allow identifying tile entities as racks via the API,
 * i.e. without having to link against internal classes. This also means that
 * <em>you should not implement this</em>.
 */
public interface Rack extends SidedEnvironment, EnvironmentHost, Rotatable, IInventory {
    /**
     * The mountable in the specified slot.
     * <p/>
     * This can be <tt>null</tt>, for example when there is no mountable installed
     * in that slot.
     *
     * @return the mountable currently hosted in the specified slot.
     */
    RackMountable getMountable(int slot);

    /**
     * Mark the mountable in the specified slot as changed.
     * <p/>
     * This will cause the mountable's {@link RackMountable#getData()} method
     * to be called in the next tick and the updated data to be sent to the
     * clients, where it can be used for state based rendering of the mountable
     * for example.
     *
     * @param slot the slot of the mountable to queue for updating.
     */
    void markChanged(int slot);
}
