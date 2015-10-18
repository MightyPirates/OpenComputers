package li.cil.oc.api;

import com.typesafe.config.Config;
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
    public static final String VERSION = "6.0.0-alpha";

    public static DriverAPI driver = null;
    public static FileSystemAPI fileSystem = null;
    public static ItemAPI items = null;
    public static MachineAPI machine = null;
    public static ManualAPI manual = null;
    public static NanomachinesAPI nanomachines = null;
    public static NetworkAPI network = null;

    public static Config config = null;
}
