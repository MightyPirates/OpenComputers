package li.cil.oc.api.internal;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Environment;

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
public interface Microcontroller extends Environment, EnvironmentHost, MachineHost, Rotatable, Tiered {
}
