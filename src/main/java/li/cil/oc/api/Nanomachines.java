package li.cil.oc.api;

import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Collections;

/**
 * This API allows interfacing with nanomachines.
 * <p/>
 * It allows registering custom behavior providers as well as querying for all
 * presently registered providers and getting a controller for a player.
 */
public class Nanomachines {
    /**
     * Register a new behavior provider.
     * <p/>
     * When a controller is reconfigured it will draw behaviors from all
     * registered providers and build a new random connection graph to
     * those behaviors.
     *
     * @param provider the provider to add.
     */
    public static void addProvider(BehaviorProvider provider) {
        if (API.nanomachines != null)
            API.nanomachines.addProvider(provider);
    }

    /**
     * Get a list of all currently registered providers.
     *
     * @return the list of all currently registered providers.
     */
    public static Iterable<BehaviorProvider> getProviders() {
        if (API.nanomachines != null)
            return API.nanomachines.getProviders();
        return Collections.emptyList();
    }

    /**
     * Check whether a player has a nanomachine controller installed.
     *
     * @param player the player to check for.
     * @return <tt>true</tt> if the player has a controller, <tt>false</tt> otherwise.
     */
    public static boolean hasController(EntityPlayer player) {
        if (API.nanomachines != null)
            return API.nanomachines.hasController(player);
        return false;
    }

    /**
     * Get the nanomachine controller of the specified player.
     * <p/>
     * If the player has a controller installed, this will initialize the
     * controller if it has not already been loaded. If the player has no
     * controller, this will return <tt>null</tt>.
     *
     * @param player the player to get the controller for.
     * @return the controller for the specified player.
     */
    public static Controller getController(EntityPlayer player) {
        if (API.nanomachines != null)
            return API.nanomachines.getController(player);
        return null;
    }

    /**
     * Install a controller for the specified player if it doesn't already
     * have one.
     * <p/>
     * This will also initialize the controller if it has not already been
     * initialized.
     *
     * @param player the player to install a nanomachine controller for.
     */
    public static Controller installController(EntityPlayer player) {
        if (API.nanomachines != null)
            return API.nanomachines.installController(player);
        return null;
    }

    /**
     * Uninstall a controller from the specified player if it has one.
     * <p/>
     * This will disable all active behaviors before disposing the controller.
     *
     * @param player the player to uninstall a nanomachine controller from.
     */
    public static void uninstallController(EntityPlayer player) {
        if (API.nanomachines != null)
            API.nanomachines.uninstallController(player);
    }

    // ----------------------------------------------------------------------- //

    private Nanomachines() {
    }
}
