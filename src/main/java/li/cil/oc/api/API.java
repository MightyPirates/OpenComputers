package li.cil.oc.api;

import com.typesafe.config.Config;
import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.detail.FileSystemAPI;
import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.MachineAPI;
import li.cil.oc.api.detail.ManualAPI;
import li.cil.oc.api.detail.NanomachinesAPI;
import li.cil.oc.api.detail.NetworkAPI;

/**
 * Central reference for the API.
 * <br>
 * Don't use this class directly, prefer using the other classes in this
 * package instead. This class is initialized by OpenComputers in the
 * pre-init phase, so it should not be used before the init phase.
 */
public class API {
    public static final String ID_OWNER = "OpenComputers|Core";
    public static final String VERSION = "6.0.0-alpha";

    // ----------------------------------------------------------------------- //

    /**
     * The loaded config.
     */
    public static Config config = null;

    /**
     * Whether OpenComputers uses power.
     * <br>
     * This is set in the init phase, so do not rely it before the post-init phase.
     */
    public static boolean isPowerEnabled = false;

    // ----------------------------------------------------------------------- //
    // Prefer using the static methods in the respective classes in this package
    // over accessing these instances directly.

    public static DriverAPI driver = null;
    public static FileSystemAPI fileSystem = null;
    public static ItemAPI items = null;
    public static MachineAPI machine = null;
    public static ManualAPI manual = null;
    public static NanomachinesAPI nanomachines = null;
    public static NetworkAPI network = null;

    // ----------------------------------------------------------------------- //

    private API() {
    }
}
