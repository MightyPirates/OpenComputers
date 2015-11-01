package li.cil.oc.api.component;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Packet;

/**
 * Use this interface on environments that may receive network messages from a
 * bus in a rack.
 * <p/>
 * Specifically, this is checked on environments in servers installed in racks.
 * The server will collect the first three environments of components in it
 * implement this interface, and provide their nodes to the rack via the
 * {@link RackMountable#getConnectableAt(int)} method. This in turn will allow them
 * to be 'connected' to the buses, so that they can receive network messages
 * arriving on the respective side of the rack.
 */
public interface RackBusConnectable extends Environment {
    /**
     * Called to inject a network packet that arrived on the bus this
     * environment is connected to in the hosting rack.
     *
     * @param packet the packet to handle.
     */
    void receivePacket(Packet packet);
}
