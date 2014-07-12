package li.cil.oc.client.renderer.font;

import java.nio.ByteBuffer;

public interface IFontParser {
	public ByteBuffer getGlyph(int charCode);
	public int getGlyphWidth();
	public int getGlyphHeight();
}
