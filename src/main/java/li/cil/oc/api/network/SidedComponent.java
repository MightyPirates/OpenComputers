package li.cil.oc.api.network;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * This is an extended version of {@link li.cil.oc.api.network.SimpleComponent}
 * which allows controlling connectivity on a side-by-side basis.
 * <p/>
 * Like the <tt>SimpleComponent</tt> interface, this is intended to be used
 * with tile entities that should act as OC components. Please see the
 * <tt>SimpleComponent</tt> interface for more information.
 */
public interface SidedComponent {
    /**
     * Whether this component can connect to a node on the specified side.
     * <p/>
     * The provided side is relative to the component, i.e. when the tile
     * entity sits at (0, 0, 0) and is asked for its southern node (positive
     * Z axis) it has to return the connectivity for the face between it and
     * the block at (0, 0, 1).
     *
     * @param side the side to check for.
     * @return whether the component may be connected to from the specified side.
     */
    boolean canConnectNode(ForgeDirection side);
}
