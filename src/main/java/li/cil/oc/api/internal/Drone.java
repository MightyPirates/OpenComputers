package li.cil.oc.api.internal;

import li.cil.oc.api.driver.EnvironmentHost;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Environment;

/**
 * This interface is implemented as a marker by drones.
 * <p/>
 * This is implemented by drones entities. That means you can use this to check
 * for drones by using:
 * <pre>
 *     if (entity instanceof Drone) {
 * </pre>
 * <p/>
 * The only purpose is to allow identifying entities as drones via the API,
 * i.e. without having to link against internal classes. This also means
 * that <em>you should not implement this</em>.
 */
public interface Drone extends EnvironmentHost, Rotatable {
    /**
     * The machine currently hosted by this drone.
     */
    Machine machine();
}
