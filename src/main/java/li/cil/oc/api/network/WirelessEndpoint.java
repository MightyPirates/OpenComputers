package li.cil.oc.api.network;

import net.minecraft.world.World;

/**
 * Interface for wireless endpoints that can be registered with the internal
 * wireless network registry.
 * <p/>
 */
public interface WirelessEndpoint {
    /**
     * The X coordinate of the endpoint in the world, in block coordinates.
     */
    int x();

    /**
     * The Y coordinate of the endpoint in the world, in block coordinates.
     */
    int y();

    /**
     * The Z coordinate of the endpoint in the world, in block coordinates.
     */
    int z();

    /**
     * The world this endpoint lives in.
     */
    World world();

    /**
     * Makes the endpoint receive a single packet.
     *
     * @param packet   the packet to receive.
     * @param distance the distance to the wireless endpoint that sent the packet.
     */
    void receivePacket(Packet packet, double distance);
}
