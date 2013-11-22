package li.cil.oc.api.network;

import net.minecraftforge.common.ForgeDirection;

/**
 * This interface is like {@link net.minecraft.inventory.ISidedInventory} is to
 * {@link net.minecraft.inventory.IInventory}, it allows an environment to
 * specify different node access for its different sides.
 * <p/>
 * This interface is intended to be used on tile entities that are environments.
 * It is used to determine which neighbors a tile entity can connect to when
 * calling {@link li.cil.oc.api.Network#joinOrCreateNetwork}. It is used by the
 * keyboard to only interface with the side on which it is attached, as well as
 * the router to offer a different node for each side.
 */
public interface SidedEnvironment extends Environment {
    /**
     * The node this environment uses for the specified side.
     * <p/>
     * This is the side aware version of the normal {@link #node()} method.
     *
     * @param side the side to get the node for.
     * @return the node for the specified side.
     * @see #node()
     */
    Node sidedNode(ForgeDirection side);

    /**
     * Whether the environment provides a node to connect to on the specified
     * side.
     * <p/>
     * For each side the environment returns <tt>false</tt> here, it should
     * return <tt>null</tt> from {@link #sidedNode}, and for each side it
     * returns <tt>true</tt> for it should return a node.
     * <p/>
     * This is primarily intended for the client side, i.e. rendering related
     * things, since nodes are not created on the client side. Accordingly, this
     * will find little use on the server side.
     *
     * @param side the side to check for.
     * @return whether the environment provides a node for the specified side.
     */
    boolean canConnect(ForgeDirection side);
}
