package li.cil.oc.client.renderer.font;

import java.nio.ByteBuffer;

public interface IGlyphProvider {
    public void initialize();

    public ByteBuffer getGlyph(int charCode);

    public int getGlyphWidth();

    public int getGlyphHeight();
}
