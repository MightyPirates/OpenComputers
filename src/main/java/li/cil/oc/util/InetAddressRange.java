
package li.cil.oc.util;

import com.google.common.net.InetAddresses;

import java.net.InetAddress;

// Originally by SquidDev
public final class InetAddressRange {
    private final byte[] min;
    private final byte[] max;

    InetAddressRange(byte[] min, byte[] max) {
        this.min = min;
        this.max = max;
    }

    public boolean matches(InetAddress address) {
        byte[] entry = address.getAddress();
        if (entry.length != min.length) return false;

        for (int i = 0; i < entry.length; i++) {
            int value = 0xFF & entry[i];
            if (value < (0xFF & min[i]) || value > (0xFF & max[i])) return false;
        }

        return true;
    }

    public static InetAddressRange parse(String addressStr, String prefixSizeStr) {
        int prefixSize;
        try {
            prefixSize = Integer.parseInt(prefixSizeStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Malformed address range entry '%s': Cannot extract size of CIDR mask from '%s'.",
                    addressStr + '/' + prefixSizeStr, prefixSizeStr));
        }

        InetAddress address;
        try {
            address = InetAddresses.forString(addressStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Malformed address range entry '%s': Cannot extract IP address from '%s'.",
                    addressStr + '/' + prefixSizeStr, addressStr));
        }

        // Mask the bytes of the IP address.
        byte[] minBytes = address.getAddress(), maxBytes = address.getAddress();
        int size = prefixSize;
        for (int i = 0; i < minBytes.length; i++) {
            if (size <= 0) {
                minBytes[i] = (byte) 0;
                maxBytes[i] = (byte) 0xFF;
            } else if (size < 8) {
                minBytes[i] = (byte) (minBytes[i] & 0xFF << (8 - size));
                maxBytes[i] = (byte) (maxBytes[i] | ~(0xFF << (8 - size)));
            }

            size -= 8;
        }

        return new InetAddressRange(minBytes, maxBytes);
    }
}