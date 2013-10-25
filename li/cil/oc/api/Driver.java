package li.cil.oc.api;

import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.driver.Block;
import li.cil.oc.api.driver.Item;

final public class Driver {
    /**
     * Registers a new block driver.
     * <p/>
     * This must be called in the init phase, *not* the pre- or post-init phases.
     *
     * @param driver the driver to register.
     */
    public static void add(Block driver) {
        if (instance != null) instance.add(driver);
    }

    /**
     * Registers a new item driver.
     * <p/>
     * This must be called in the init phase, *not* the pre- or post-init phases.
     *
     * @param driver the driver to register.
     */
    public static void add(Item driver) {
        if (instance != null) instance.add(driver);
    }

    // ----------------------------------------------------------------------- //

    private Driver() {
    }

    /**
     * Initialized in pre-init.
     */
    public static DriverAPI instance = null;
}