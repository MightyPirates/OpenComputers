package li.cil.oc.client.renderer.font;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.util.FontUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

public class FontParserHex implements IGlyphProvider {
    private static final byte[] OPAQUE = {(byte) 255, (byte) 255, (byte) 255, (byte) 255};
    private static final byte[] TRANSPARENT = {0, 0, 0, 0};

    private final Int2ObjectMap<byte[]> glyphs = new Int2ObjectOpenHashMap<>();

    @Override
    public void initialize() {
        try {
            final InputStream font = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(Settings.resourceDomain(), "font.hex")).getInputStream();
            try {
                OpenComputers.log().info("Initializing unicode glyph provider.");
                final BufferedReader input = new BufferedReader(new InputStreamReader(font));
                String line;
                int glyphCount = 0;
                while ((line = input.readLine()) != null) {
                    final String[] info = line.split(":");
                    final int charCode = Integer.parseInt(info[0], 16);
                    if (charCode < 0 || charCode >= FontUtils.codepoint_limit()) {
                        OpenComputers.log().warn(String.format("Unicode font contained unexpected glyph: U+%04X, ignoring", charCode));
                        continue; // Out of bounds.
                    }
                    final int expectedWidth = FontUtils.wcwidth(charCode);
                    if (expectedWidth < 1) continue; // Skip control characters.
                    // Two chars representing one byte represent one row of eight pixels.
                    final byte[] glyph = new byte[info[1].length() >> 1];
                    final int glyphWidth = glyph.length / getGlyphHeight();
                    if (expectedWidth == glyphWidth) {
                        for (int i = 0; i < glyph.length; i++) {
                            glyph[i] = (byte) Integer.parseInt(info[1].substring(i * 2, i * 2 + 2), 16);
                        }
                        if (!glyphs.containsKey(charCode)) {
                            glyphCount++;
                        }
                        glyphs.put(charCode, glyph);
                    } else if (Settings.get().logHexFontErrors()) {
                        OpenComputers.log().warn(String.format("Size of glyph for code point U+%04X (%s) in font (%d) does not match expected width (%d), ignoring.", charCode, String.valueOf((char) charCode), glyphWidth, expectedWidth));
                    }
                }
                OpenComputers.log().info("Loaded " + glyphCount + " glyphs.");
            } finally {
                try {
                    font.close();
                } catch (IOException ex) {
                    OpenComputers.log().warn("Error parsing font.", ex);
                }
            }
        } catch (IOException ex) {
            OpenComputers.log().warn("Failed loading glyphs.", ex);
        }
    }

    @Override
    public ByteBuffer getGlyph(int charCode) {
        if (!glyphs.containsKey(charCode))
            return null;
        final byte[] glyph = glyphs.get(charCode);
        if (glyph == null || glyph.length <= 0)
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
