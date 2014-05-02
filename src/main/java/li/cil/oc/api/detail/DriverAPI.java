package li.cil.oc.api.detail;

import li.cil.oc.api.driver.Block;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface DriverAPI {
    /**
     * Registers a new driver for a block component.
     * <p/>
     * Whenever the neighboring blocks of an Adapter block change, it checks if
     * there exists a driver for the changed block, and if it is configured to
     * interface that block type connects it to the component network.
     * <p/>
     * This must be called in the init phase, <em>not</em> the pre- or post-init
     * phases.
     *
     * @param driver the driver for a block component.
     */
    void add(Block driver);

    /**
     * Registers a new driver for an item component.
     * <p/>
     * Item components can inserted into a computers component slots. They have
     * to specify their type, to determine into which slots they can fit.
     * <p/>
     * This must be called in the init phase, <em>not</em> the pre- or post-init
     * phases.
     *
     * @param driver the driver for an item component.
     */
    void add(Item driver);

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
    void add(Converter converter);

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
    Block driverFor(World world, int x, int y, int z);

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
    Item driverFor(ItemStack stack);
}
