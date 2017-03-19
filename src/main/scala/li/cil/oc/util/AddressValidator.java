package li.cil.oc.util;

import com.google.common.net.InetAddresses;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Objects;
import java.util.regex.Matcher;

@FunctionalInterface
public interface AddressValidator {
    boolean isValid(final InetAddress inetAddress, final String host);

    // ----------------------------------------------------------------------- //

    static AddressValidator create(final String value) {
        try {
            final Matcher matcher = Constants.CIDR_PATTERN.matcher(value);
            if (matcher.find()) {
                final String address = matcher.group(1);
                final String prefix = matcher.group(2);
                final int addr = InetAddresses.coerceToInteger(InetAddresses.forString(address));
                final int mask = 0xFFFFFFFF << (32 - Integer.valueOf(prefix));
                final int min = addr & mask;
                final int max = min | ~mask;
                return (inetAddress, host) -> {
                    if (inetAddress instanceof Inet4Address) {
                        final int numeric = InetAddresses.coerceToInteger(inetAddress);
                        return min <= numeric && numeric <= max;
                    } else {
                        return true; // Can't check IPv6 addresses so we pass them.
                    }
                };
            } else {
                final InetAddress address = InetAddress.getByName(value);
                return (inetAddress, host) -> Objects.equals(host, value) || inetAddress == address;
            }
        } catch (final Throwable t) {
            OpenComputers.log().warn("Invalid entry in internet blacklist / whitelist: " + value, t);
            return (inetAddress, host) -> true;
        }
    }
}
