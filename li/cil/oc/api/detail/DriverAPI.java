package li.cil.oc.api.detail;

import li.cil.oc.api.driver.Block;
import li.cil.oc.api.driver.Item;

public interface DriverAPI {
    /**
     * Registers a new driver for a block component.
     * <p/>
     * Whenever the neighboring blocks of an Adapter block change, it checks if
     * there exists a driver for the changed block, and if it is configured to
     * interface that block type connects it to the component network.
     *
     * @param driver the driver for a block component.
     */
    void add(Block driver);

    /**
     * Registers a new driver for an item component.
     * <p/>
     * Item components can inserted into a computers component slots. They have
     * to specify their type, to determine into which slots they can fit.
     *
     * @param driver the driver for an item component.
     */
    void add(Item driver);
}
