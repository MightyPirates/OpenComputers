package li.cil.oc.api.internal;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * This interface is implemented by the computer case and robot tile entities
 * to allow item components to query the orientation of their host, i.e. to
 * allow getting the facing of the tile entity passed to their drivers'
 * {@link li.cil.oc.api.driver.Item#createEnvironment(net.minecraft.item.ItemStack, li.cil.oc.api.network.EnvironmentHost)}
 * method.
 * <p/>
 * This interface is <em>not meant to be implemented</em>, just used.
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

    /**
     * Converts a facing relative to the block's <em>local</em> coordinate
     * system to a <tt>global orientation</tt>, using south as the standard
     * orientation.
     * <p/>
     * For example, if the block is facing east, calling this with south will
     * return east, calling it with west will return south and so on.
     *
     * @param value the value to translate.
     * @return the translated orientation.
     */
    ForgeDirection toGlobal(ForgeDirection value);

    /**
     * Converts a <tt>global</tt> orientation to a facing relative to the
     * block's <em>local</em> coordinate system, using south as the standard
     * orientation.
     * <p/>
     * For example, if the block is facing east, calling this with south will
     * return east, calling it with west will return north and so on.
     *
     * @param value the value to translate.
     * @return the translated orientation.
     */
    ForgeDirection toLocal(ForgeDirection value);
}
