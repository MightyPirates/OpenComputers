package li.cil.oc.client.renderer.font;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.util.FontUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.logging.Level;

public class FontParserUnifont implements IGlyphProvider {
    private static final byte[] OPAQUE = {(byte) 255, (byte) 255, (byte) 255, (byte) 255};
    private static final byte[] TRANSPARENT = {0, 0, 0, 0};

    private final byte[][] glyphs = new byte[0x10000][];

    @Override
    public void initialize() {
        for (int i = 0; i < glyphs.length; ++i) {
            glyphs[i] = null;
        }
        final InputStream font = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(Settings.resourceDomain(), "unifont.hex")).getInputStream();
        if (font == null) {
            OpenComputers.log().warning("Failed opening unifont file.");
            return;
        }
        try {
            OpenComputers.log().info("Initializing Unifont glyph provider.");
            final BufferedReader input = new BufferedReader(new InputStreamReader(font));
            String line;
            int glyphCount = 0;
            while ((line = input.readLine()) != null) {
                final String[] info = line.split(":");
                final int charCode = Integer.parseInt(info[0], 16);
                final int expectedWidth = FontUtil.wcwidth(charCode);
                if (expectedWidth < 1) continue; // Skip control characters.
                final byte[] glyph = new byte[info[1].length() >> 1];
                final int glyphWidth = glyph.length / getGlyphHeight();
                if (expectedWidth == glyphWidth) {
                    for (int i = 0; i < glyph.length; i++) {
                        glyph[i] = (byte) Integer.parseInt(info[1].substring(i * 2, i * 2 + 2), 16);
                    }
                    if (glyphs[charCode] == null) {
                        glyphCount++;
                    }
                    glyphs[charCode] = glyph;
                } else if (Settings.get().logUnifontErrors()) {
                    OpenComputers.log().warning(String.format("Size of glyph for code point U+%04X (%s) in Unifont (%d) does not match expected width (%d), ignoring.", charCode, String.valueOf((char) charCode), glyphWidth, expectedWidth));
                }
            }
            OpenComputers.log().info("Loaded " + glyphCount + " glyphs.");
        } catch (IOException ex) {
            OpenComputers.log().log(Level.WARNING, "Failed loading glyphs.", ex);
        } finally {
            try {
                font.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public ByteBuffer getGlyph(int charCode) {
        if (charCode < 0 || charCode >= glyphs.length || glyphs[charCode] == null || glyphs[charCode].length == 0)
            return null;
        final byte[] glyph = glyphs[charCode];
        final ByteBuffer buffer = BufferUtils.createByteBuffer(glyph.length * getGlyphWidth() * 4);
        for (byte aGlyph : glyph) {
            int c = ((int) aGlyph) & 0xFF;
            for (int j = 0; j < 8; j++) {
                if ((c & 128) > 0) buffer.put(OPAQUE);
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
