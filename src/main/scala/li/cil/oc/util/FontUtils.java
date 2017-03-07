package li.cil.oc.util;

import li.cil.oc.OpenComputers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class FontUtils {
    // Note: we load the widths from a file (one byte per width) because the Scala
    // compiler craps its pants when we try to have it as an array in the source
    // file... seems having an array with 0x10000 entries leads to stack overflows,
    // who would have known!
    private static final byte[] WIDTHS = new byte[0x10000];

    static {
        // Note to self: NOT VIA THE FUCKING RESOURCE SYSTEM BECAUSE IT'S FUCKING CLIENT ONLY YOU IDIOT.
        final InputStream is = FontUtils.class.getResourceAsStream("/assets/opencomputers/wcwidth.bin");
        if (is != null) {
            try {
                final int read = is.read(WIDTHS);
                final int remaining = WIDTHS.length - read;
                Arrays.fill(WIDTHS, read, remaining, (byte) -1);
                is.close();
            } catch (final IOException e) {
                Arrays.fill(WIDTHS, (byte) -1);
                OpenComputers.log().warn("Failed parsing character widths. Font rendering will probably be derpy as all hell.", e);
            }
        } else {
            Arrays.fill(WIDTHS, (byte) -1);
            OpenComputers.log().warn("Failed opening character widths. Font rendering will probably be derpy as all hell.");
        }
    }

    public int wcwidth(final int ch) {
        return (ch < 0 || ch >= WIDTHS.length) ? -1 : WIDTHS[ch];
    }

    // ----------------------------------------------------------------------- //

    private FontUtils() {
    }
}
