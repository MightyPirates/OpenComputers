package li.cil.oc.client.renderer.font

import li.cil.oc.util.FontUtil
import org.lwjgl.opengl.GL11

class DataCharRenderer extends DynamicCharRenderer {
  val parser = new FontParserUnifont()

  override def canDisplay(c: Char) = FontUtil.wcwidth(c) > 0 && parser.getGlyph(c) != null

  override def charWidth: Double = parser.getGlyphWidth

  override def charHeight: Double = parser.getGlyphHeight

  override def drawChar(charCode: Int) {
    val w = parser.getGlyphWidth * FontUtil.wcwidth(charCode)
    val h = parser.getGlyphHeight

    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
    GL11.glColor4f(1, 1, 1, 1)
    GL11.glEnable(GL11.GL_BLEND)

    val texture = GL11.glGenTextures()
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture)
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, parser.getGlyph(charCode))
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

    GL11.glBegin(GL11.GL_QUADS)
    GL11.glTexCoord2f(0, 1)
    GL11.glVertex2f(0, h)
    GL11.glTexCoord2f(1, 1)
    GL11.glVertex2f(w, h)
    GL11.glTexCoord2f(1, 0)
    GL11.glVertex2f(w, 0)
    GL11.glTexCoord2f(0, 0)
    GL11.glVertex2f(0, 0)
    GL11.glEnd()

    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    GL11.glDeleteTextures(texture)

    GL11.glPopAttrib()
  }
}
