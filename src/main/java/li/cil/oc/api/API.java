package li.cil.oc.api;

import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.detail.FileSystemAPI;
import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.MachineAPI;
import li.cil.oc.api.detail.ManualAPI;
import li.cil.oc.api.detail.NetworkAPI;

/**
 * Central reference for the API.
 * <p/>
 * Don't use this class directly, prefer using the other classes in this
 * package instead. This class is initialized by OpenComputers in the
 * pre-init phase, so it should not be used before the init phase.
 */
public class API {
    public static final String ID_OWNER = "OpenComputers|Core";
    public static final String VERSION = "5.2.3";

    public static DriverAPI driver = null;
    public static FileSystemAPI fileSystem = null;
    public static ItemAPI items = null;
    public static MachineAPI machine = null;
    public static ManualAPI manual = null;
    public static NetworkAPI network = null;
}
