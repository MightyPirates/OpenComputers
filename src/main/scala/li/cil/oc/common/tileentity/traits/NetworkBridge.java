package li.cil.oc.common.tileentity.traits;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.util.Location;
import li.cil.oc.util.MovingAverage;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Bridges network messages across multiple sub-networks by relaying messages
 * received from one sub-network to all other connected sub-networks.
 */
public class NetworkBridge implements INBTSerializable<NBTTagCompound> {
    public interface NetworkBridgeHost extends Location {
        int getNetworkBridgeMaxQueueSize();

        int getNetworkBridgeInterval();

        int getNetworkBridgeBandwidth();

        void onNetworkMessageProcessed();

        Node getPacketHopNode();
    }

    public interface NetworkBridgeAdapter {
        void register(final NetworkBridge bridge);

        void processPacket(final int receivePort, final Packet packet);
    }

    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final Queue<QueueEntry> queue = new LinkedList<>();
    private int cooldown = -1;

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_QUEUE = "queue";
    private static final String TAG_COOLDOWN = "cooldown";

    private final NetworkBridgeHost host;
    private final List<NetworkBridgeAdapter> adapters = new ArrayList<>();
    private final List<QueueEntry> volatileQueue = new ArrayList<>();
    private final MovingAverage averageQueueSize = new MovingAverage(20);
    private int ports;

    // ----------------------------------------------------------------------- //

    public NetworkBridge(final NetworkBridgeHost host) {
        this.host = host;
    }

    public NetworkBridgeHost getHost() {
        return host;
    }

    public void addAdapter(final NetworkBridgeAdapter adapter) {
        adapter.register(this);
        adapters.add(adapter);
    }

    public int registerPort() {
        return ports++;
    }

    public void update() {
        synchronized (queue) {
            averageQueueSize.put(queue.size());

            if (cooldown > 0) {
                cooldown--;
            }

            if (cooldown == 0) {
                for (int i = Math.min(host.getNetworkBridgeBandwidth(), queue.size()); i > 0; --i) {
                    final QueueEntry entry = queue.poll();
                    volatileQueue.add(entry);
                }

                if (!queue.isEmpty()) {
                    cooldown = host.getNetworkBridgeInterval();
                }
            }
        }

        for (final QueueEntry entry : volatileQueue) {
            processPacket(entry.port, entry.packet);
        }
        volatileQueue.clear();
    }

    // ----------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagCompound serializeNBT() {
        final NBTTagCompound nbt = new NBTTagCompound();

        final NBTTagList queueNbt = new NBTTagList();
        for (final QueueEntry entry : queue) {
            queueNbt.appendTag(entry.serializeNBT());
        }
        nbt.setTag(TAG_QUEUE, queueNbt);

        nbt.setInteger(TAG_COOLDOWN, cooldown);

        return nbt;
    }

    @Override
    public void deserializeNBT(final NBTTagCompound nbt) {
        final NBTTagList queueNbt = nbt.getTagList(TAG_QUEUE, NBT.TAG_COMPOUND);
        for (int i = 0; i < queueNbt.tagCount(); i++) {
            queue.add(new QueueEntry(queueNbt.getCompoundTagAt(i)));
        }

        cooldown = nbt.getInteger(TAG_COOLDOWN);
    }

    // ----------------------------------------------------------------------- //

    public void tryEnqueuePacket(final int receivePort, final Packet packet) {
        synchronized (queue) {
            if (queue.size() < host.getNetworkBridgeMaxQueueSize()) {
                queue.add(new QueueEntry(receivePort, packet));
                if (cooldown < 1) {
                    cooldown = host.getNetworkBridgeInterval();
                }
            }
        }
    }

    protected void processPacket(final int receivePort, final Packet packet) {
        for (final NetworkBridgeAdapter adapter : adapters) {
            adapter.processPacket(receivePort, packet);
        }
        host.onNetworkMessageProcessed();
    }

    private static final class QueueEntry implements INBTSerializable<NBTTagCompound> {
        // ----------------------------------------------------------------------- //
        // Persisted data.

        public int port;
        public Packet packet;

        // ----------------------------------------------------------------------- //
        // Computed data.

        // NBT tag names.
        private static final String TAG_PORT = "port";
        private static final String TAG_PACKET = "packet";

        // ----------------------------------------------------------------------- //

        QueueEntry(final int port, final Packet packet) {
            this.port = port;
            this.packet = packet;
        }

        QueueEntry(final NBTTagCompound nbt) {
            deserializeNBT(nbt);
        }

        // ----------------------------------------------------------------------- //
        // INBTSerializable

        @Override
        public NBTTagCompound serializeNBT() {
            final NBTTagCompound nbt = new NBTTagCompound();
            nbt.setByte(TAG_PORT, (byte) port);
            nbt.setTag(TAG_PACKET, packet.serializeNBT());
            return nbt;
        }

        @Override
        public void deserializeNBT(final NBTTagCompound nbt) {
            port = nbt.getInteger(TAG_PORT);
            packet = Network.newPacket(nbt.getCompoundTag(TAG_PACKET));
        }
    }
}
