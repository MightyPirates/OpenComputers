package li.cil.oc.api.driver;

import java.util.Map;

/**
 * Implement this on {@link li.cil.oc.api.network.Environment}s if you wish to
 * expose some (typically static) information about the device represented by
 * that environment to a {@link li.cil.oc.api.Machine} connected to it.
 * <br>
 * You may also implement this on a {@link li.cil.oc.api.machine.MachineHost}
 * in which case the <code>Machine</code> will forward that information as
 * its own (since <code>MachineHost</code>s usually use the machine's node as
 * their own, this avoids a dummy environment used solely for device info).
 * <br>
 * This is intended to permit programs to reflect on the hardware they are
 * running on, typically for purely informational purposes, but possibly to
 * toggle certain hardware specific features.
 * <br>
 * For example, graphics cards may expose their timings via this interface, so
 * that programs may determine at what speed they can redraw, and optimize
 * execution order.
 * <br>
 * While the format of the returned table of information is entirely up to you,
 * it is recommended to orient yourself on the key values and names that
 * <code>lshw</code> uses (http://www.ezix.org/project/wiki/HardwareLiSter),
 * where applicable.
 */
public interface DeviceInfo {
    /**
     * Compile a list of device information strings as key-value pairs.
     * <br>
     * For example, this may list the type of the device, a vendor (for example
     * your mod name, or something more creative if you like), specifications
     * of the device (speeds, capacities).
     * <br>
     * For example, OC's tier one memory module returns the following:
     * <table summary="Example table of device information.">
     * <tr><td>class</td><td>memory</td></tr>
     * <tr><td>description</td><td>Memory bank</td></tr>
     * <tr><td>vendor</td><td>MightyPirates GmbH &amp; Co. KG</td></tr>
     * <tr><td>product</td><td>Multipurpose RAM Type</td></tr>
     * <tr><td>clock</td><td>500</td></tr>
     * </table>
     *
     * @return the table of information on this device, or <code>null</code>.
     */
    Map<String, String> getDeviceInfo();

    /**
     * Recommended list of key values for the device info table.
     * <br>
     * You are strongly encouraged to at least define <code>class</code>, <code>description</code>,
     * <code>vendor</code> and <code>product</code>, to allow a more homogenous experience for the
     * end-user reading this information via a script.
     * <br>
     * Feel free to be somewhat... flexible with the designated uses of these fields. For example,
     * the capacity and size fields have differing meaning depending on the device in OpenComputers
     * itself (e.g. they're used for maximum number of characters for graphics cards, width is
     * used for bit depth on graphics cards, etc.), just try to stick with what's somewhat logical.
     */
    final class DeviceAttribute {
        public static final String Class = "class"; // device's class (see below), e.g. "processor"
        public static final String Description = "description"; // human-readable description of the hardware node, e.g. "Ethernet interface"
        public static final String Vendor = "vendor"; // vendor/manufacturer of the device, e.g. "Minecorp Inc."
        public static final String Product = "product"; // product name of the device, e.g. "ATY Raderps 4200X"
        public static final String Version = "version"; // version/release of the device, e.g. "2.1.0"
        public static final String Serial = "serial"; // serial number of the device
        public static final String Capacity = "capacity"; // maximum capacity reported by the device, e.g. unformatted size of a disk
        public static final String Size = "size"; // actual size of the device, e.g. actual usable space on a disk
        public static final String Clock = "clock"; // bus clock (in Hz) of the device, e.g. call speed(s) of a component
        public static final String Width = "width"; // address width of the device, in the broadest sense

        private DeviceAttribute() {
        }
    }

    /**
     * Recommended list of values for the <code>class</code> attribute (see above).
     * <br>
     * Again, feel free to be somewhat creative with those. When in doubt, use <code>generic</code>.
     */
    final class DeviceClass {
        public static final String System = "system"; // used to refer to the whole machine, e.g. "Computer", "Server", "Robot"
        public static final String Bridge = "bridge"; // internal bus converter, maybe useful for some low-level archs?
        public static final String Memory = "memory"; // memory bank that can contain data, executable code, e.g. RAM, EEPROM
        public static final String Processor = "processor"; // execution processor, e.g. CPU, cryptography support
        public static final String Address = "address"; // memory address range, e.g. video memory (again, low-level archs maybe?)
        public static final String Storage = "storage"; // storage controller, e.g. IDE controller (low-level...)
        public static final String Disk = "disk"; // random-access storage device, e.g. floppies
        public static final String Tape = "tape"; // sequential-access storage device, e.g. cassette tapes
        public static final String Bus = "bus"; // device-connecting bus, e.g. USB
        public static final String Network = "network"; // network interface, e.g. ethernet, wlan
        public static final String Display = "display"; // display adapter, e.g. graphics cards
        public static final String Input = "input"; // user input device, e.g. keyboard, mouse
        public static final String Printer = "printer"; // printing device, e.g. printer, 3D-printer
        public static final String Multimedia = "multimedia"; // audio/video device, e.g. sound cards
        public static final String Communication = "communication"; // line communication device, e.g. modem, serial ports
        public static final String Power = "power"; // energy source, e.g. battery, power supply
        public static final String Volume = "volume"; // disk volume, e.g. file system
        public static final String Generic = "generic"; // generic device (used when no other class is suitable)

        private DeviceClass() {
        }
    }

}
