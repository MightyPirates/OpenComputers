package li.cil.oc.util;

import java.net.InetAddress;

@FunctionalInterface
public interface AddressValidator {
    boolean isValid(final InetAddress inetAddress, final String host);
}
