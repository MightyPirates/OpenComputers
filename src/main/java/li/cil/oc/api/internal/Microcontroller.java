package li.cil.oc.api.internal;

import li.cil.oc.api.util.Location;
import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.tileentity.Rotatable;
import li.cil.oc.api.machine.MachineHost;

/**
 * This interface is implemented as a marker by microcontrollers.
 * <p/>
 * This is implemented by microcontroller tile entities. That means you can
 * use this to check for microcontrollers by using:
 * <pre>
 *     if (tileEntity instanceof Microcontroller) {
 * </pre>
 * <p/>
 * The only purpose is to allow identifying tile entities as microcontrollers
 * via the API, i.e. without having to link against internal classes. This
 * also means that <em>you should not implement this</em>.
 */
public interface Microcontroller extends NodeContainer, Location, MachineHost, Rotatable, Tiered {
}
