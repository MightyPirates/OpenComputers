package li.cil.oc.client.renderer.font;

import li.cil.oc.util.FontUtil;
import org.lwjgl.BufferUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

public class FontParserUnifont implements IFontParser {
    private final byte[][] data;

    public FontParserUnifont() throws Exception {
        System.out.println("[FontParserUnifont] Initialized Unifont parser");
        data = new byte[65536][];
        InputStream font = getClass().getResourceAsStream("/assets/opencomputers/unifont.hex");
        BufferedReader input = new BufferedReader(new InputStreamReader(font));
        String line;
        int glyphs = 0;
        while ((line = input.readLine()) != null) {
            glyphs++;
            String[] info = line.split(":");
            int code = Integer.parseInt(info[0], 16);
            byte[] glyph = new byte[info[1].length() >> 1];
            for (int i = 0; i < glyph.length; i++) {
                glyph[i] = (byte) Integer.parseInt(info[1].substring(i * 2, i * 2 + 2), 16);
            }
            data[code] = glyph;
        }
        System.out.println("[FontParserUnifont] " + glyphs + " glyphs loaded.");
    }

    private static final byte[] b_set = {(byte) 255, (byte) 255, (byte) 255, (byte) 255};
    private static final byte[] b_unset = {0, 0, 0, 0};

    @Override
    public ByteBuffer getGlyph(int charCode) {
        if (charCode >= 65536 || data[charCode] == null || data[charCode].length == 0)
            return null;
        byte[] glyph = data[charCode];
        int glyphWidth;
        if (glyph.length == 16) glyphWidth = 8;
        else if (glyph.length == 32) glyphWidth = 16;
        else return null;
        ByteBuffer buf = BufferUtils.createByteBuffer(glyphWidth * 16 * 4);
        for (byte aGlyph : glyph) {
            int c = ((int) aGlyph) & 0xFF;
            for (int j = 0; j < 8; j++) {
                if ((c & 128) > 0) buf.put(b_set);
                else buf.put(b_unset);
                c <<= 1;
            }
        }
        buf.rewind();
        return buf;
    }

    @Override
    public int getGlyphWidth() {
        return 8;
    }

    @Override
    public int getGlyphHeight() {
        return 16;
    }

    public static void main(String[] args) {
        try {
            FontParserUnifont fpu = new FontParserUnifont();
            ByteBuffer buf = fpu.getGlyph(9829);
            System.out.println(FontUtil.wcwidth("a".codePointAt(0)));
            for (int i = 0; i < buf.capacity(); i += 4) {
                if ((i % (buf.capacity() >> 4)) == 0 && i > 0) System.out.println("|");
                if (buf.get(i) != 0) System.out.print("#");
                else System.out.print("話す ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
