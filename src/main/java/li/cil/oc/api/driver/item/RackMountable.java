package li.cil.oc.api.driver.item;

import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Use this interface on environments provided by drivers for items that can
 * be installed in a server rack.
 * <p/>
 * The provided environment can be used for updating the part in its installed
 * state. The nodes provided by the getters in this interface are used to
 * access nodes provided by the environment (e.g. multiple "interfacing"
 * nodes for a switch), and connect the nodes to the corresponding buses as
 * defined by the rack's configuration.
 */
public interface RackMountable extends ManagedEnvironment {
    /**
     * The number of nodes exposed by the environment.
     */
    int getNodeCount();

    /**
     * Returns the node at ths specified index.
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
