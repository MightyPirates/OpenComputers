package li.cil.oc.common.tileentity.traits;

import li.cil.oc.api.network.Packet;
import li.cil.oc.server.network.QuantumNetwork;

public final class NetworkBridgeAdapterQuantum extends AbstractNodeBridgeAdapter implements QuantumNetwork.QuantumNode {
    // ----------------------------------------------------------------------- //
    // Computed data.

    private static final String CREATIVE_TUNNEL_NAME = "creative";

    private String tunnel = CREATIVE_TUNNEL_NAME;

    // ----------------------------------------------------------------------- //

    public String getTunnel() {
        return tunnel;
    }

    public void setTunnel(final String tunnel) {
        this.tunnel = tunnel;
    }

    // ----------------------------------------------------------------------- //
    // AbstractNodeBridgeAdapter

    @Override
    protected void onEnabled() {
        QuantumNetwork.add(this);
    }

    @Override
    protected void onDisabled() {
        QuantumNetwork.remove(this);
    }

    @Override
    protected void sendPacket(final int receivePort, final Packet packet) {
        if (receivePort == getPort()) {
            return;
        }

        QuantumNetwork.send(this, packet);
    }

    // ----------------------------------------------------------------------- //
    // QuantumNode

    @Override
    public String tunnel() {
        return tunnel;
    }

    @Override
    public void receivePacket(final Packet packet) {
        tryEnqueuePacket(packet);
    }
}
