package li.cil.oc.api.prefab;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;

/**
 * TileEntities can implement the {@link li.cil.oc.api.network.SidedEnvironment}
 * interface to allow them to interact with the component network, by providing
 * a separate {@link li.cil.oc.api.network.Node} for each block face, and
 * connecting it to said network. This allows more control over connectivity
 * than the simple {@link li.cil.oc.api.network.Environment}.
 * <p/>
 * Nodes in such a network can communicate with each other, or just use the
 * network as an index structure to find other nodes connected to them.
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class TileEntitySidedEnvironment extends TileEntity implements SidedEnvironment {
    // See constructor.
    protected Node[] nodes = new Node[6];

    // See updateEntity().
    protected boolean addedToNetwork = false;

    /**
     * This expects a node per face that is used to represent this tile entity.
     * <p/>
     * You must only create new nodes using the factory method in the network
     * API, {@link li.cil.oc.api.Network#newNode(li.cil.oc.api.network.Environment, li.cil.oc.api.network.Visibility)}.
     * <p/>
     * For example:
     * <pre>
     * // The first parameters to newNode is the host() of the node, which will
     * // usually be this tile entity. The second one is it's reachability,
     * // which determines how other nodes in the same network can query this
     * // node. See {@link li.cil.oc.api.network.Network#nodes(li.cil.oc.api.network.Node)}.
     * super(Network.newNode(???, Visibility.Network)
     *       // This call allows the node to consume energy from the
     *       // component network it is in and act as a consumer, or to
     *       // inject energy into that network and act as a producer.
     *       // If you do not need energy remove this call.
     *       .withConnector()
     *       // This call marks the tile entity as a component. This means you
     *       // can mark methods in it using the {@link li.cil.oc.api.network.Callback}
     *       // annotation, making them callable from user code. The first
     *       // parameter is the name by which the component will be known in
     *       // the computer, in this case it could be accessed as
     *       // <tt>component.example</tt>. The second parameter is the
     *       // component's visibility. This is like the node's reachability,
     *       // but only applies to computers. For example, network cards can
     *       // only be <em>seen</em> by the computer they're installed in, but
     *       // can be <em>reached</em> by all other network cards in the same
     *       // network. If you do not need callbacks remove this call.
     *       .withComponent("example", Visibility.Neighbors)
     *       // Finalizes the construction of the node and returns it.
     *       .create(), ...);
     * </pre>
     */
    protected TileEntitySidedEnvironment(final Node... nodes) {
        System.arraycopy(nodes, 0, this.nodes, 0, Math.min(nodes.length, this.nodes.length));
    }

    // ----------------------------------------------------------------------- //

    // canConnect() is for the client side, to determine how cables are
    // rendered, for example, so you'll have to provide that logic yourself.
    // Nodes are only created on the server side, so checking whether a node
    // exists for a side won't work on the client.

    @Override
    public Node sidedNode(final ForgeDirection side) {
        return side == ForgeDirection.UNKNOWN ? null : nodes[side.ordinal()];
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        super.updateEntity();
        // On the first update, try to add our node to nearby networks. We do
        // this in the update logic, not in validate() because we need to access
        // neighboring tile entities, which isn't possible in validate().
        // We could alternatively check node != null && node.network() == null,
        // but this has somewhat better performance, and makes it clearer.
        if (!addedToNetwork) {
            addedToNetwork = true;
            // Note that joinOrCreateNetwork will try to connect each of our
            // sided nodes to their respective neighbor (sided) node.
            Network.joinOrCreateNetwork(this);
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        // Make sure to remove the node from its network when its environment,
        // meaning this tile entity, gets unloaded.
        for (Node node : nodes) {
            if (node != null) node.remove();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        // Make sure to remove the node from its network when its environment,
        // meaning this tile entity, gets unloaded.
        for (Node node : nodes) {
            if (node != null) node.remove();
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void readFromNBT(final NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        int index = 0;
        for (Node node : nodes) {
            // The host check may be superfluous for you. It's just there to allow
            // some special cases, where getNode() returns some node managed by
            // some other instance (for example when you have multiple internal
            // nodes in this tile entity).
            if (node != null && node.host() == this) {
                // This restores the node's address, which is required for networks
                // to continue working without interruption across loads. If the
                // node is a power connector this is also required to restore the
                // internal energy buffer of the node.
                node.load(nbt.getCompoundTag("oc:node" + index));
            }
            ++index;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        int index = 0;
        for (Node node : nodes) {
            // See readFromNBT() regarding host check.
            if (node != null && node.host() == this) {
                final NBTTagCompound nodeNbt = new NBTTagCompound();
                node.save(nodeNbt);
                nbt.setCompoundTag("oc:node" + index, nodeNbt);
            }
            ++index;
        }
    }
}
