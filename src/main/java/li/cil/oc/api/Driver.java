package li.cil.oc.api;

import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.driver.Block;
import li.cil.oc.api.driver.Item;

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
     * This must be called in the init phase, <em>not</em> the pre- or post-init
     * phases.
     *
     * @param driver the driver to register.
     */
    public static void add(final Block driver) {
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
    public static void add(final Item driver) {
        if (instance != null) instance.add(driver);
    }

    // ----------------------------------------------------------------------- //

    private Driver() {
    }

    public static DriverAPI instance = null;
}