package li.cil.oc.client.renderer.font;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.util.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.List;

public class FontParserHex implements IGlyphProvider {
    private static final byte[] OPAQUE = {(byte) 255, (byte) 255, (byte) 255, (byte) 255};
    private static final byte[] TRANSPARENT = {0, 0, 0, 0};

    private final TIntObjectMap<byte[]> glyphs = new TIntObjectHashMap<>();

    private static int hex2int(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'F') {
            return c - ('A' - 10);
        } else if (c >= 'a' && c <= 'f') {
            return c - ('a' - 10);
        } else {
            throw new RuntimeException("invalid char: " + c);
        }
    }

    @Override
    public void initialize() {
        try {
            glyphs.clear();

            OpenComputers.log().info("Loading Unicode glyphs...");
            long time = System.currentTimeMillis();
            int glyphCount = 0;

            ResourceLocation loc = new ResourceLocation(Settings.resourceDomain(), "font.hex");
            for (IResource resource : (List<IResource>) Minecraft.getMinecraft().getResourceManager().getAllResources(loc)) {
                final InputStream font = resource.getInputStream();
                try {
                    final BufferedReader input = new BufferedReader(new InputStreamReader(font));
                    String line;
                    while ((line = input.readLine()) != null) {
                        final String info = line.substring(0, line.indexOf(':'));
                        final int charCode = Integer.parseInt(info, 16);
                        if (charCode < 0 || charCode >= FontUtils.codepoint_limit()) {
                            OpenComputers.log().warn(String.format("Unicode font contained unexpected glyph: U+%04X, ignoring", charCode));
                            continue; // Out of bounds.
                        }
                        final int expectedWidth = FontUtils.wcwidth(charCode);
                        if (expectedWidth < 1) continue; // Skip control characters.
                        // Two chars representing one byte represent one row of eight pixels.
                        int glyphStrOfs = info.length() + 1;
                        final byte[] glyph = new byte[(line.length() - glyphStrOfs) >> 1];
                        final int glyphWidth = glyph.length / getGlyphHeight();
                        if (expectedWidth == glyphWidth) {
                            for (int i = 0; i < glyph.length; i++, glyphStrOfs += 2) {
                                glyph[i] = (byte) ((hex2int(line.charAt(glyphStrOfs)) << 4) | (hex2int(line.charAt(glyphStrOfs + 1))));
                            }
                            if (!glyphs.containsKey(charCode)) {
                                glyphCount++;
                            }
                            glyphs.put(charCode, glyph);
                        } else if (Settings.get().logHexFontErrors()) {
                            OpenComputers.log().warn(String.format("Size of glyph for code point U+%04X (%s) in font (%d) does not match expected width (%d), ignoring.", charCode, String.valueOf((char) charCode), glyphWidth, expectedWidth));
                        }
                    }
                } finally {
                    try {
                        font.close();
                    } catch (IOException ex) {
                        OpenComputers.log().warn("Error parsing font.", ex);
                    }
                }
            }

            OpenComputers.log().info("Loaded " + glyphCount + " glyphs in " + (System.currentTimeMillis() - time) + " milliseconds.");
        } catch (IOException ex) {
            OpenComputers.log().warn("Failed loading glyphs.", ex);
        }
    }

    @Override
    public ByteBuffer getGlyph(int charCode) {
        if (!glyphs.containsKey(charCode))
            return null;
        final byte[] glyph = glyphs.get(charCode);
        if (glyph == null || glyph.length == 0)
            return null;
        final ByteBuffer buffer = BufferUtils.createByteBuffer(glyph.length * getGlyphWidth() * 4);
        for (byte aGlyph : glyph) {
            int c = ((int) aGlyph) & 0xFF;
            // Grab all bits by grabbing the leftmost one then shifting.
            for (int j = 0; j < 8; j++) {
                final boolean isBitSet = (c & 0x80) > 0;
                if (isBitSet) buffer.put(OPAQUE);
                else buffer.put(TRANSPARENT);
                c <<= 1;
            }
        }
        buffer.rewind();
        return buffer;
    }

    @Override
    public int getGlyphWidth() {
        return 8;
    }

    @Override
    public int getGlyphHeight() {
        return 16;
    }
}
