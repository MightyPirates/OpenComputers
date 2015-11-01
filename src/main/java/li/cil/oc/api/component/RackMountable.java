package li.cil.oc.api.component;

import li.cil.oc.api.internal.StateAware;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.ComponentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Use this interface on environments provided by drivers for items that can
 * be installed in a server rack.
 * <p/>
 * The provided environment can be used for updating the part in its installed
 * state. The nodes provided by the getters in this interface are used to
 * access nodes provided by the environment (e.g. multiple "interfacing"
 * nodes for a switch), and connect the nodes to the corresponding buses as
 * defined by the rack's configuration.
 * <p/>
 * Note: mountables may implement the {@link ComponentHost} interface and
 * {@link IInventory}. In this case, if they contain a redstone card and have
 * a state of <tt>State.IsWorking</tt> the rack will visually connect to
 * redstone, for example. Same goes for abstract bus cards, and potentially
 * more things in the future.
 * <p/>
 * Furthermore, implementing {@link Analyzable} will allow specifying more
 * information when the analyzer is used on the mountable while it's in a rack.
 */
public interface RackMountable extends ManagedEnvironment, StateAware {
    /**
     * Returns some data describing the state of the mountable.
     * <p/>
     * This is called on the server side to synchronize data to the client after
     * the rack's {@link li.cil.oc.api.internal.Rack#markChanged(int)}
     * method has been called for the slot this mountable is in. It will there
     * be passed on with the render event to allow state specific rendering of
     * the mountable in the rack.
     *
     * @return the data to synchronize to the clients.
     */
    NBTTagCompound getData();

    /**
     * The number of nodes exposed by the environment.
     */
    int getNodeCount();

    /**
     * Returns the node at the specified index.
     */
    Node getNodeAt(int index);

    /**
     * This gets called when the server rack is activated by a player, and
     * hits the space occupied by this mountable.
     *
     * @param player the player activating the mountable.
     */
    void onActivate(EntityPlayer player);
}
