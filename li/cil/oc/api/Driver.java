package li.cil.oc.api;

import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.driver.Block;
import li.cil.oc.api.driver.Item;

/**
 * This API allows registering new drivers with the mod.
 * <p/>
 * Drivers are used to make items and third-party blocks available in the mod's
 * component network, and optionally to Lua programs.
 *
 * @see Network
 * @see Block
 * @see Item
 */
public final class Driver {
    /**
     * Registers a new block driver.
     * <p/>
     * This must be called in the init phase, <em>not</em> the pre- or post-init
     * phases.
     *
     * @param driver the driver to register.
     */
    public static void add(Block driver) {
        if (instance != null) instance.add(driver);
    }

    /**
     * Registers a new item driver.
     * <p/>
     * This must be called in the init phase, <em>not</em> the pre- or post-init
     * phases.
     *
     * @param driver the driver to register.
     */
    public static void add(Item driver) {
        if (instance != null) instance.add(driver);
    }

    // ----------------------------------------------------------------------- //

    private Driver() {
    }

    public static DriverAPI instance = null;
}