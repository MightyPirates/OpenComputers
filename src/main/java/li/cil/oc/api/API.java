package li.cil.oc.api;

import li.cil.oc.api.detail.*;

/**
 * Central reference for the API.
 * <p/>
 * Don't use this class directly, prefer using the other classes in this
 * package instead. This class is initialized by OpenComputers in the
 * pre-init phase, so it should not be used before the init phase.
 */
public class API {
    public static final String ID_OWNER = "OpenComputers|Core";
    public static final String VERSION = "4.0.0-alpha.2";

    public static DriverAPI driver = null;
    public static FileSystemAPI fileSystem = null;
    public static ItemAPI items = null;
    public static MachineAPI machine = null;
    public static NetworkAPI network = null;
}
