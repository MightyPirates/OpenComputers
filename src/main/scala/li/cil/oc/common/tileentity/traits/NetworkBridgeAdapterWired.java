package li.cil.oc.common.tileentity.traits;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.network.*;
import li.cil.oc.api.prefab.network.AbstractNodeContainer;
import li.cil.oc.api.util.Location;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.INBTSerializable;
import org.apache.commons.lang3.ObjectUtils;

import java.util.HashSet;
import java.util.Set;

public final class NetworkBridgeAdapterWired implements NetworkBridge.NetworkBridgeAdapter, INBTSerializable<NBTTagList> {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NodeContainerNetworkBridgeAdapterBasic[] nodeContainers;

    // ----------------------------------------------------------------------- //
    // Computed data.

    private static final String NETWORK_MESSAGE_NAME = "network.message";

    private final boolean[] isPrimaryNetwork;
    private boolean isPrimaryNetworkCacheDirty = true;

    // ----------------------------------------------------------------------- //

    public NetworkBridgeAdapterWired(final Location location, final int nodeCount) {
        nodeContainers = new NodeContainerNetworkBridgeAdapterBasic[nodeCount];
        for (int i = 0; i < nodeContainers.length; i++) {
            nodeContainers[i] = new NodeContainerNetworkBridgeAdapterBasic(location, this, i);
        }
        isPrimaryNetwork = new boolean[nodeCount];
    }

    public NodeContainer getContainer(final int index) {
        return nodeContainers[index];
    }

    // ----------------------------------------------------------------------- //
    // NetworkBridgeAdapter

    @Override
    public void register(final NetworkBridge bridge) {
        for (final NodeContainerNetworkBridgeAdapterBasic nodeContainer : nodeContainers) {
            nodeContainer.setBridge(bridge);
            nodeContainer.setPort(bridge.registerPort());
        }
    }

    @Override
    public void processPacket(final int receivePort, final Packet packet) {
        for (int index = 0; index < nodeContainers.length; index++) {
            final int port = nodeContainers[index].getPort();
            if (receivePort != port && isPrimaryNetwork(index)) {
                final Node node = nodeContainers[index].getNode();
                if (node != null) {
                    node.sendToReachable(NETWORK_MESSAGE_NAME, packet);
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagList serializeNBT() {
        final NBTTagList nbt = new NBTTagList();
        for (final NodeContainerNetworkBridgeAdapterBasic nodeContainer : nodeContainers) {
            nbt.appendTag(nodeContainer.serializeNBT());
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(final NBTTagList nbt) {
        if (nbt.tagCount() == nodeContainers.length) {
            for (int i = 0; i < nodeContainers.length; i++) {
                nodeContainers[i].deserializeNBT(nbt.getCompoundTagAt(i));
            }
        } else {
            OpenComputers.log().warn("nodeContainer count mismatch. Not loading data.");
        }
    }

    // ----------------------------------------------------------------------- //

    private boolean isPrimaryNetwork(final int index) {
        validateNetworkCache();
        return !isNodeContainer(index) || isPrimaryNetwork[index];
    }

    private void markNetworkCacheDirty() {
        isPrimaryNetworkCacheDirty = true;
    }

    private void validateNetworkCache() {
        if (!isPrimaryNetworkCacheDirty) {
            return;
        }
        isPrimaryNetworkCacheDirty = false;

        final Set<Network> networks = new HashSet<>();
        for (int i = 0; i < nodeContainers.length; i++) {
            final Node node = nodeContainers[i].getNode();
            isPrimaryNetwork[i] = node != null && networks.add(node.getNetwork());
        }
    }

    private boolean isNodeContainer(final int index) {
        return index >= 0 && index < nodeContainers.length;
    }

    private boolean isSourceSelf(final Node source) {
        for (final NodeContainerNetworkBridgeAdapterBasic nodeContainer : nodeContainers) {
            final Node node = nodeContainer.getNode();
            if (node == source) {
                return true;
            }
        }

        return false;
    }

    // ----------------------------------------------------------------------- //

    private static final class NodeContainerNetworkBridgeAdapterBasic extends AbstractNodeContainer {
        // ----------------------------------------------------------------------- //
        // Computed data.
        private final NetworkBridgeAdapterWired adapter;
        private final int index;
        private NetworkBridge bridge;
        private int port;

        // ----------------------------------------------------------------------- //

        NodeContainerNetworkBridgeAdapterBasic(final Location location, final NetworkBridgeAdapterWired adapter, final int index) {
            super(location);
            this.adapter = adapter;
            this.index = index;
        }

        public void setBridge(final NetworkBridge bridge) {
            this.bridge = bridge;
        }

        public int getPort() {
            return port;
        }

        public void setPort(final int port) {
            this.port = port;
        }

        // ----------------------------------------------------------------------- //
        // NodeConnector

        @Override
        public void onConnect(final Node node) {
            super.onConnect(node);
            adapter.markNetworkCacheDirty();
        }

        @Override
        public void onDisconnect(final Node node) {
            super.onDisconnect(node);
            adapter.markNetworkCacheDirty();
        }

        @Override
        public void onMessage(final Message message) {
            super.onMessage(message);
            if (!adapter.isPrimaryNetwork(index)) {
                return;
            }

            if (ObjectUtils.notEqual(message.getName(), NETWORK_MESSAGE_NAME)) {
                return;
            }

            if (adapter.isSourceSelf(message.getSource())) {
                return;
            }

            final Object[] data = message.getData();
            if (data.length < 1 || !(data[0] instanceof Packet)) {
                return;
            }

            final Node node = getNode();
            assert node != null : "Received message although we have no node.";

            final Packet packet = ((Packet) data[0]).getHop(node);
            if (packet != null) {
                bridge.tryEnqueuePacket(port, packet);
            }
        }

        // ----------------------------------------------------------------------- //
        // AbstractNodeContainer

        @Override
        protected Node createNode() {
            return li.cil.oc.api.Network.newNode(this, Visibility.NETWORK).create();
        }
    }
}
