package li.cil.oc.common.tileentity.traits;

import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;

public abstract class AbstractNodeBridgeAdapter implements NetworkBridge.NetworkBridgeAdapter {
    // ----------------------------------------------------------------------- //
    // Computed data.

    private NetworkBridge bridge;
    private int port;
    private boolean isEnabled;

    // ----------------------------------------------------------------------- //

    protected abstract void onEnabled();

    protected abstract void onDisabled();

    protected abstract void sendPacket(final int receivePort, final Packet packet);

    // ----------------------------------------------------------------------- //

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(final boolean enabled) {
        if (isEnabled == enabled) {
            return;
        }
        isEnabled = enabled;


        if (isEnabled) {
            onEnabled();
        } else {
            onDisabled();
        }
    }

    // ----------------------------------------------------------------------- //
    // NetworkBridgeAdapter

    @Override
    public void register(final NetworkBridge bridge) {
        this.bridge = bridge;
        port = bridge.registerPort();
    }

    @Override
    public void processPacket(final int receivePort, final Packet packet) {
        if (!isEnabled) {
            return;
        }

        sendPacket(receivePort, packet);
    }

    // ----------------------------------------------------------------------- //

    protected int getPort() {
        return port;
    }

    protected void tryEnqueuePacket(final Packet packet) {
        if (bridge == null) {
            return;
        }

        final Node node = bridge.getHost().getPacketHopNode();
        final Packet newPacket = packet.getHop(node);
        if (newPacket == null) {
            return;
        }

        bridge.tryEnqueuePacket(port, newPacket);
    }
}
