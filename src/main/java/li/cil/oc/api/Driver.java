package li.cil.oc.api;

import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.driver.Block;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * This API allows registering new drivers with the mod.
 * <p/>
 * Drivers are used to make items and third-party blocks available in the mod's
 * component network, and optionally to user programs. If you implement a new
 * block that should interact with the mod's component network it is enough to
 * have it implement {@link li.cil.oc.api.network.Environment} - no driver is
 * needed in that case.
 *
 * @see Network
 * @see Block
 * @see Item
 */
public final class Driver {
    /**
     * Registers a new block driver.
     * <p/>
     * Whenever the neighboring blocks of an Adapter block change, it checks if
     * there exists a driver for the changed block, and if it is configured to
     * interface that block type connects it to the component network.
     * <p/>
     * This must be called in the init phase, <em>not</em> the pre- or post-init
     * phases.
     *
     * @param driver the driver to register.
     */
    public static void add(final Block driver) {
        if (instance != null)
            instance.add(driver);
    }

    /**
     * Registers a new item driver.
     * <p/>
     * Item components can inserted into a computers component slots. They have
     * to specify their type, to determine into which slots they can fit.
     * <p/>
     * This must be called in the init phase, <em>not</em> the pre- or post-init
     * phases.
     *
     * @param driver the driver to register.
     */
    public static void add(final Item driver) {
        if (instance != null)
            instance.add(driver);
    }

    /**
     * Registers a new type converter.
     * <p/>
     * Type converters are used to automatically convert values returned from
     * callbacks to a "simple" format that can be pushed to any architecture.
     * <p/>
     * This must be called in the init phase, <em>not</em> the pre- or post-init
     * phases.
     *
     * @param converter the converter to register.
     */
    public static void add(final Converter converter) {
        if (instance != null)
            instance.add(converter);
    }

    /**
     * Looks up a driver for the block at the specified position in the
     * specified world.
     * <p/>
     * Note that several drivers for a single block can exist. Because of this
     * block drivers are always encapsulated in a 'compound' driver, which is
     * what will be returned here. In other words, you should will <em>not</em>
     * get actual instances of drivers registered via {@link #add(li.cil.oc.api.driver.Block)}.
     *
     * @param world the world containing the block.
     * @param x     the X coordinate of the block.
     * @param y     the Y coordinate of the block.
     * @param z     the Z coordinate of the block.
     * @return a driver for the block, or <tt>null</tt> if there is none.
     */
    public static Block driverFor(World world, int x, int y, int z) {
        if (instance != null)
            return instance.driverFor(world, x, y, z);
        return null;
    }

    /**
     * Looks up a driver for the specified item stack.
     * <p/>
     * Note that unlike for blocks, there can always only be one item driver
     * per item. If there are multiple ones, the first one that was registered
     * will be used.
     *
     * @param stack the item stack to get a driver for.
     * @return a driver for the item, or <tt>null</tt> if there is none.
     */
    public static Item driverFor(ItemStack stack) {
        if (instance != null)
            return instance.driverFor(stack);
        return null;
    }

    // ----------------------------------------------------------------------- //

    private Driver() {
    }

    public static DriverAPI instance = null;
}