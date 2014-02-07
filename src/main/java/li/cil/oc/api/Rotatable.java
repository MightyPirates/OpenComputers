package li.cil.oc.api;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * This interface is implemented by the computer case and robot tile entities
 * to allow item components to query the orientation of their host, i.e. to
 * allow getting the facing of the tile entity passed to their drivers'
 * {@link li.cil.oc.api.driver.Item#createEnvironment(net.minecraft.item.ItemStack, net.minecraft.tileentity.TileEntity)}
 * method.
 */
public interface Rotatable {
    /**
     * The current facing of a tile entity implementing this interface.
     * <p/>
     * Intended to be used to query the orientation of an item components' host.
     * For example:
     * <pre>
     * class SomeDriver implements li.cil.oc.api.driver.Item {
     *     // ...
     *     ManagedEnvironment createEnvironment(ItemStack stack, TileEntity tileentity) {
     *         if (tileentity instanceof Rotatable) {
     *             ForgeDirection facing = ((Rotatable)tileentity).facing();
     *             // Do something with facing.
     *         }
     *     }
     * }
     * </pre>
     *
     * @return the current facing.
     */
    ForgeDirection facing();
}
