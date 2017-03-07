package li.cil.oc.api.network;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

/**
 * The environment of a node.
 * <p/>
 * For blocks/tile entities this will usually be the tile entity. For items
 * this will usually be an object created when a component is added to a
 * compatible inventory (e.g. put into a computer).
 * <p/>
 * Tile entities should provide this capability if they want to be connected
 * to the component network of their neighboring blocks.
 * <p/>
 * To get some more control over which sides of your block may connect to a
 * network, make sure to only provide this capability on the relevant sides.
 * <p/>
 * When a tile entity provides this capability a good way of connecting and
 * disconnecting is the following pattern:
 * <pre>
 *     // In the TileEntity:
 *     void updateEntity() {
 *         super.updateEntity();
 *         if (Loader.isModLoaded("opencomputers"))
 *             env.connect();
 *     }
 *
 *     // In the Environment:
 *     void connect() {
 *         if (getNode() != null && getNode().getNetwork() == null) {
 *             api.Network.joinOrCreateNetwork(this);
 *         }
 *     }
 *
 *     void onChunkUnload() {
 *         super.onChunkUnload()
 *         if (node != null) node.remove()
 *     }
 *
 *     void invalidate() {
 *         super.invalidate()
 *         if (node != null) node.remove()
 *     }
 * </pre>
 * <p/>
 * Item environments are always managed, so you will always have to provide a
 * driver as a capability of the item for it to interact with the component network.
 * <p/>
 * To interact with environments from user code you will have to do two things:
 * <ol>
 * <li>Make the environment's {@link #getNode} a {@link Component} and ensure
 * its {@link Component#getVisibility} is set to a value where it can
 * be seen by computers in the network.</li>
 * <li>Annotate methods in the environment as {@link li.cil.oc.api.machine.Callback}s.</li>
 * </ol>
 */
public interface Environment extends INBTSerializable<NBTTagCompound> {
    /**
     * Get the host of this environment, giving context information about the
     * environment's position in the world.
     *
     * @return the environment's host.
     */
    EnvironmentHost getHost();

    /**
     * The node this environment hosts.
     * <p/>
     * The node is the environments gateway to the component network, and thus
     * its preferred way to interact with other components in the same network.
     *
     * @return the node this environment wraps.
     */
    @Nullable
    Node getNode();

    /**
     * This is called when a node is added to a network.
     * <p/>
     * This is also called for the node itself, if it was added to the network.
     * <p/>
     * At this point the node's network is never <tt>null</tt> and you can use
     * it to query it for other nodes. Use this to perform initialization logic,
     * such as building lists of nodes of a certain type in the network.
     * <p/>
     * For example, if node A is added to a network with nodes B and C, these
     * calls are made:
     * <ul>
     * <li>A.onConnect(A)</li>
     * <li>A.onConnect(B)</li>
     * <li>A.onConnect(C)</li>
     * <li>B.onConnect(A)</li>
     * <li>C.onConnect(A)</li>
     * </ul>
     */
    void onConnect(final Node node);

    /**
     * This is called when a node is removed from the network.
     * <p/>
     * This is also called for the node itself, when it has been removed from
     * its network. Note that this is called on the node that is being removed
     * <em>only once</em> with the node itself as the parameter.
     * <p/>
     * At this point the node's network is no longer available (<tt>null</tt>).
     * Use this to perform clean-up logic such as removing references to the
     * removed node.
     * <p/>
     * For example, if node A is removed from a network with nodes A, B and C,
     * these calls are made:
     * <ul>
     * <li>A.onDisconnect(A)</li>
     * <li>B.onDisconnect(A)</li>
     * <li>C.onDisconnect(A)</li>
     * </ul>
     */
    void onDisconnect(final Node node);

    /**
     * This is the generic message handler.
     * <p/>
     * It is called whenever this environments {@link Node} receives a message
     * that was sent via one of the <tt>send</tt> methods in the {@link Network}
     * or the <tt>Node</tt> itself.
     *
     * @param message the message to handle.
     */
    void onMessage(final Message message);
}
