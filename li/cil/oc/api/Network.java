package li.cil.oc.api;

import li.cil.oc.api.detail.Builder;
import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.network.Environment;
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

    /**
     * Factory function for creating new nodes.
     * <p/>
     * Use this to create a node for your environment (e.g. tile entity). This
     * will return a builder that can be used to further specialize the node,
     * making it either a component node (for Lua callbacks), a connector node
     * (for power interaction) or both.
     *
     * @param host       the environment the node is created for.
     * @param visibility the visibility of the node.
     * @return a new node builder.
     */
    public static Builder.NodeBuilder newNode(Environment host, Visibility visibility) {
        if (instance != null)
            return instance.newNode(host, visibility);
        return null;
    }

    // ----------------------------------------------------------------------- //

    private Network() {
    }

    public static NetworkAPI instance = null;
}
