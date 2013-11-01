package li.cil.oc.api;

import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.world.World;

final public class Network {
    /**
     * Tries to add a tile entity network node at the specified coordinates to
     * adjacent networks.
     *
     * @param world the world the tile entity lives in.
     * @param x     the X coordinate of the tile entity.
     * @param y     the Y coordinate of the tile entity.
     * @param z     the Z coordinate of the tile entity.
     */
    public static void joinOrCreateNetwork(World world, int x, int y, int z) {
        if (instance != null) instance.joinOrCreateNetwork(world, x, y, z);
    }

    public static Node createNode(Environment host, String name, Visibility visibility) {
        if (instance != null)
            return instance.createNode(host, name, visibility);
        return null;
    }

    public static Component createComponent(Node node) {
        if (instance != null)
            return instance.createComponent(node);
        return null;
    }

    public static Node createProducer(Node node) {
        if (instance != null)
            return instance.createProducer(node);
        return null;
    }

    public static Node createConsumer(Node node) {
        if (instance != null)
            return instance.createConsumer(node);
        return null;
    }

    // ----------------------------------------------------------------------- //

    private Network() {
    }

    public static NetworkAPI instance = null;
}
