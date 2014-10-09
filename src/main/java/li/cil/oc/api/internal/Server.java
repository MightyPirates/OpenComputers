package li.cil.oc.api.internal;

import li.cil.oc.api.driver.EnvironmentHost;
import li.cil.oc.api.machine.Machine;

/**
 * This interface is implemented as a marker by servers in server racks.
 * <p/>
 * This is implemented by servers in server racks, which serve as their
 * computer components' environment. That means you can use this to check for
 * servers by using either:
 * <pre>
 *     if (node.host() instanceof Server) {
 * </pre>
 * <p/>
 * You can get a reference to a server either via the above cast, or via a
 * {@link li.cil.oc.api.internal.ServerRack}.
 * <p/>
 * The only purpose is to allow identifying node environments as servers
 * via the API, i.e. without having to link against internal classes. This
 * also means that <em>you should not implement this</em>.
 */
public interface Server extends EnvironmentHost {
    /**
     * The machine currently hosted by this server.
     */
    Machine machine();

    /**
     * The server rack this server is in.
     */
    ServerRack rack();

    /**
     * The slot of the server rack this server is in.
     */
    int slot();

    /**
     * The tier of the server.
     */
    int tier();
}
