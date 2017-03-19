package li.cil.oc.util;

import com.google.common.base.Throwables;
import li.cil.oc.server.component.DebugCard;
import org.apache.commons.codec.binary.Hex;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

@FunctionalInterface
public interface DebugCardAccess {
    @Nullable
    String checkAccess(@Nullable final DebugCard.AccessContext ctx);

    // ----------------------------------------------------------------------- //

    DebugCardAccess Allowed = (ctx) -> null;
    DebugCardAccess Forbidden = (ctx) -> "debug card is disabled";

    final class Whitelist implements DebugCardAccess {
        private final File noncesFile;
        private final SecureRandom rng;
        private final HashMap<String, String> values = new HashMap<>();

        public Whitelist(final File noncesFile) {
            try {
                this.noncesFile = noncesFile;
                this.rng = SecureRandom.getInstance("SHA1PRNG");
                load();
            } catch (final Throwable e) {
                throw Throwables.propagate(e);
            }
        }

        public void save() throws IOException {
            final File noncesDir = noncesFile.getParentFile();
            if (!noncesDir.exists() && !noncesDir.mkdirs()) {
                throw new IOException("Cannot create nonces directory: " + noncesDir);
            }

            try (final PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(noncesFile), StandardCharsets.UTF_8), false)) {
                values.forEach((p, n) -> writer.println(p + " " + n));
            }
        }

        public void load() throws IOException {
            values.clear();

            if (!noncesFile.exists()) {
                return;
            }

            final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(noncesFile), StandardCharsets.UTF_8));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                final String[] data = line.split(" ", 2);
                if (data.length == 2) {
                    values.put(data[0], data[1]);
                }
            }
        }

        @Nullable
        public String getNonce(final String player) {
            return values.get(player.toLowerCase(Locale.ROOT));
        }

        public boolean isWhitelisted(final String player) {
            return values.containsKey(player.toLowerCase(Locale.ROOT));
        }

        public Set<String> getWhitelist() {
            return values.keySet();
        }

        public void add(final String player) throws IOException {
            if (!values.containsKey(player.toLowerCase(Locale.ROOT))) {
                values.put(player.toLowerCase(Locale.ROOT), generateNonce());
                save();
            }
        }

        public void remove(final String player) throws IOException {
            if (values.remove(player.toLowerCase(Locale.ROOT)) != null) {
                save();
            }
        }

        public void invalidate(final String player) throws IOException {
            if (values.containsKey(player.toLowerCase(Locale.ROOT))) {
                values.put(player.toLowerCase(Locale.ROOT), generateNonce());
                save();
            }
        }

        @Nullable
        @Override
        public String checkAccess(@Nullable final DebugCard.AccessContext ctx) {
            if (ctx != null) {
                final String x = values.get(ctx.player().toLowerCase(Locale.ROOT));
                if (x != null) {
                    return x.equals(ctx.nonce()) ? null : "debug card is invalidated, please re-bind it to yourself";
                } else {
                    return "you are not whitelisted to use debug card";
                }
            } else {
                return "debug card is whitelisted, Shift+Click with it to bind card to yourself";
            }
        }

        private String generateNonce() {
            final byte[] buf = new byte[16];
            rng.nextBytes(buf);
            return new String(Hex.encodeHex(buf, true));
        }
    }
}
