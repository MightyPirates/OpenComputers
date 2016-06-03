package li.cil.oc.api.driver;

import java.util.Map;

/**
 * Implement this on {@link li.cil.oc.api.network.Environment}s if you wish to
 * expose some (typically static) information about the device represented by
 * that environment to a {@link li.cil.oc.api.Machine} connected to it.
 * <p/>
 * This is intended to permit programs to reflect on the hardware they are
 * running on, typically for purely informational purposes, but possibly to
 * toggle certain hardware specific features.
 * <p/>
 * For example, graphics cards may expose their timings via this interface, so
 * that programs may determine at what speed they can redraw, and optimize
 * execution order.
 * <p/>
 * While the format of the returned table of information is entirely up to you,
 * it is recommended to orient yourself on the key values and names that
 * <code>lshw</code> uses (http://www.ezix.org/project/wiki/HardwareLiSter),
 * where applicable.
 */
public interface DeviceInfo {
    /**
     * Compile a list of device information strings as key-value pairs.
     * <p/>
     * For example, this may list the type of the device, a vendor (for example
     * your mod name, or something more creative if you like), specifications
     * of the device (speeds, capacities).
     * <p/>
     * For example, OC's tier one memory module returns the following:
     * <table>
     * <tr></tr>
     * </table>
     *
     * @return the table of information on this device, or <code>null</code>.
     */
    Map<String, String> getDeviceInfo();
}
