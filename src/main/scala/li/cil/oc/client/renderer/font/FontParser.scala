package li.cil.oc.client.renderer.font

import java.nio.ByteBuffer

trait FontParser {
  def getGlyph(charCode: Int): ByteBuffer

  def getGlyphWidth: Int

  def getGlyphHeight: Int
}
