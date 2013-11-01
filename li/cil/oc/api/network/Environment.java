package li.cil.oc.api.network;

import net.minecraft.world.World;

/**
 * The environment of a node.
 * <p/>
 * For blocks/tile entities this will usually be the tile entity. For items
 * this will usually be an object created when a component is added to a
 * compatible inventory (e.g. put into a computer).
 * <p/>
 * Tile entities should implement this interface if they want to be connected
 * to the component network of their neighboring blocks. If you cannot do that,
 * as mentioned above, you will have to provide a driver that creates a managed
 * environment for the block you wish to connect instead.
 * <p/>
 * If a tile entity implements this interface, it will automatically be added
 * and removed from the network when its chunk is loaded and unloaded. What you
 * will have to ensure is that it is added/removed to/from its network when
 * the corresponding block is added/removed (e.g. placed or broken by the
 * player). When a block is added to the world, you should always use
 * {@link li.cil.oc.api.Network#joinOrCreateNetwork(World, int, int, int)},
 * which will take care of all the heavy lifting for you. For removing it, use
 * {@link Network#remove(li.cil.oc.api.network.Node)} of your node's network,
 * passing along your node, i.e. <tt>node.network.remove(node);</tt>
 * <p/>
 * Item environments are always managed, so you will always have to provide a
 * driver for items that should interact with the component network.
 * <p/>
 * To interact with environments from Lua you will have to do two things:
 * <ul>
 * <ol>Make the environment's {@link #node()} a {@link Component} and ensure
 * its {@link Component#visibility()} is set to a value where it can
 * be seen by computers in the network.</ol>
 * <ol>Annotate methods in the environment as {@link LuaCallback}s.</ol>
 * </ul>
 */
public interface Environment {
    /**
     * The node this environment wraps.
     * <p/>
     * The node is the environments gateway to the component network, and thus
     * its preferred way to interact with other components in the same network.
     *
     * @return the node this environment wraps.
     */
    Node node();

    /**
     * This is called when a node is added to a network.
     * <p/>
     * This is also called for the node itself, if it was added to the network.
     * For example, after a tile entity with a node was placed, this method
     * will be called on it after its node has been connected to an existing
     * network or created a new network.
     * <p/>
     * At this point the node's network is never <tt>null</tt> and you can use
     * it to query it for other nodes. Use this to perform initialization logic,
     * such as building lists of nodes of a certain type in the network.
     */
    void onConnect(Node node);

    /**
     * This is called when a node is removed from the network.
     * <p/>
     * This is also called for the node itself, when it has been removed from
     * its network.
     * <p/>
     * At this point the node's network is no longer available (<tt>null</tt>).
     * Use this to perform clean-up logic such as removing references to the
     * removed node.
     */
    void onDisconnect(Node node);

    /**
     * This is the generic message handler.
     * <p/>
     * It is called whenever this environments Node receives a message that was
     * sent via one of the <tt>send</tt> methods in the {@link Network}.
     *
     * @param message the message to handle.
     */
    abstract void onMessage(Message message);
}
