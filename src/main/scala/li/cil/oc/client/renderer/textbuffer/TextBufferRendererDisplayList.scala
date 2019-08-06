package li.cil.oc.client.renderer.textbuffer
import li.cil.oc.Settings
import li.cil.oc.client.renderer.font.FontTextureProvider.Receiver
import li.cil.oc.client.renderer.font.{FontTextureProvider, TextBufferRenderData}
import li.cil.oc.util.{PackedColor, RenderState, TextBuffer}
import net.minecraft.client.renderer.{GLAllocation, GlStateManager}
import org.lwjgl.opengl.GL11

class TextBufferRendererDisplayList extends TextBufferRenderer {
  private val list = GLAllocation.generateDisplayLists(1)

  override def render(fontTextureProvider: FontTextureProvider, currentBuffer: TextBufferRenderData): Boolean = {
    if (currentBuffer.dirty) {
      RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

      val doCompile = !RenderState.compilingDisplayList
      if (doCompile) {
        currentBuffer.dirty = false
        GL11.glNewList(list, GL11.GL_COMPILE_AND_EXECUTE)

        RenderState.checkError(getClass.getName + ".render: glNewList")
      }

      drawBuffer(currentBuffer.data, fontTextureProvider, currentBuffer.viewport._1, currentBuffer.viewport._2)

      RenderState.checkError(getClass.getName + ".render: drawString")

      if (doCompile) {
        GL11.glEndList()

        RenderState.checkError(getClass.getName + ".render: glEndList")
      }

      RenderState.checkError(getClass.getName + ".render: leaving")

      true
    }
    else {
      GL11.glCallList(list)
      GlStateManager.enableTexture2D()
      GlStateManager.depthMask(true)
      GlStateManager.color(1, 1, 1, 1)

      // Because display lists and the GlStateManager don't like each other, apparently.
      GL11.glEnable(GL11.GL_TEXTURE_2D)
      RenderState.bindTexture(0)
      GL11.glDepthMask(true)
      GL11.glColor4f(1, 1, 1, 1)

      RenderState.disableBlend()

      RenderState.checkError(getClass.getName + ".render: glCallList")
      
      true
    }
  }

  override def destroy(): Boolean = {
    GLAllocation.deleteDisplayLists(list)

    true
  }

  private def drawBuffer(buffer: TextBuffer, fontTextureProvider: FontTextureProvider, viewportWidth: Int, viewportHeight: Int) {
    val format = buffer.format
    val charWidth = fontTextureProvider.getCharWidth
    val charHeight = fontTextureProvider.getCharHeight

    GlStateManager.pushMatrix()
    RenderState.pushAttrib()

    GlStateManager.scale(0.5f, 0.5f, 1)

    GL11.glDepthMask(false)
    GL11.glDisable(GL11.GL_BLEND)
    GL11.glEnable(GL11.GL_ALPHA_TEST)
    GL11.glDisable(GL11.GL_TEXTURE_2D)
    GL11.glAlphaFunc(GL11.GL_GEQUAL, 0.5f)
    GlStateManager.alphaFunc(GL11.GL_GEQUAL, 0.5f)

    RenderState.checkError(getClass.getName + ".drawBuffer: configure state")

    // Background first. We try to merge adjacent backgrounds of the same
    // color to reduce the number of quads we have to draw.
    GL11.glBegin(GL11.GL_QUADS)
    for (y <- 0 until (viewportHeight min buffer.height)) {
      val color = buffer.color(y)
      var cbg = 0x000000
      var x = 0
      var width = 0
      for (col <- color.map(PackedColor.unpackBackground(_, format)) if x + width < viewportWidth) {
        if (col != cbg) {
          drawQuad(charWidth, charHeight, cbg, x, y, width)
          cbg = col
          x += width
          width = 0
        }
        width = width + 1
      }
      drawQuad(charWidth, charHeight, cbg, x, y, width)
    }
    GL11.glEnd()

    RenderState.checkError(getClass.getName + ".drawBuffer: background")

    GL11.glEnable(GL11.GL_TEXTURE_2D)

    if (Settings.get.textLinearFiltering) {
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
    }

    // Foreground second. We only have to flush when the color changes, so
    // unless every char has a different color this should be quite efficient.
    for (y <- 0 until (viewportHeight min buffer.height)) {
      val line = buffer.buffer(y)
      val color = buffer.color(y)
      val ty = y * charHeight
      for (i <- 0 until fontTextureProvider.getTextureCount) {
        fontTextureProvider.begin(i)
        GL11.glBegin(GL11.GL_QUADS)
        var cfg = -1
        var tx = 0f
        for (n <- 0 until viewportWidth) {
          val ch = line(n)
          val col = PackedColor.unpackForeground(color(n), format)
          // Check if color changed.
          if (col != cfg) {
            cfg = col
            GL11.glColor3f(
              ((cfg & 0xFF0000) >> 16) / 255f,
              ((cfg & 0x00FF00) >> 8) / 255f,
              ((cfg & 0x0000FF) >> 0) / 255f)
          }
          // Don't render whitespace.
          if (ch != ' ') {
            fontTextureProvider.drawCodePoint(ch, tx, ty, TextBufferRendererDisplayList.receiver)
          }
          tx += charWidth
        }
        GL11.glEnd()
        fontTextureProvider.end(i)
      }
    }

    RenderState.checkError(getClass.getName + ".drawBuffer: foreground")

    GlStateManager.bindTexture(0)
    GL11.glDepthMask(true)
    GL11.glColor3f(1, 1, 1)
    GL11.glDisable(GL11.GL_ALPHA_TEST)
    GlStateManager.disableAlpha()
    GlStateManager.disableBlend()
    RenderState.popAttrib()
    GlStateManager.popMatrix()

    RenderState.checkError(getClass.getName + ".drawBuffer: leaving")
  }

  private def drawQuad(charWidth: Int, charHeight: Int, color: Int, x: Int, y: Int, width: Int): Unit = if (color != 0 && width > 0) {
    val x0 = x * charWidth
    val x1 = (x + width) * charWidth
    val y0 = y * charHeight
    val y1 = (y + 1) * charHeight
    GlStateManager.color(
      ((color >> 16) & 0xFF) / 255f,
      ((color >> 8) & 0xFF) / 255f,
      (color & 0xFF) / 255f)
    GL11.glVertex3d(x0, y1, 0)
    GL11.glVertex3d(x1, y1, 0)
    GL11.glVertex3d(x1, y0, 0)
    GL11.glVertex3d(x0, y0, 0)
  }
}

object TextBufferRendererDisplayList {
  private val receiver = new Receiver {
    override def draw(x1: Double, x2: Double, y1: Double, y2: Double, u1: Double, u2: Double, v1: Double, v2: Double): Unit = {
      GL11.glTexCoord2d(u1, v2)
      GL11.glVertex2d(x1, y2)
      GL11.glTexCoord2d(u2, v2)
      GL11.glVertex2d(x2, y2)
      GL11.glTexCoord2d(u2, v1)
      GL11.glVertex2d(x2, y1)
      GL11.glTexCoord2d(u1, v1)
      GL11.glVertex2d(x1, y1)
    }
  }

  def drawString(fontTextureProvider: FontTextureProvider, s: String, x: Int, y: Int): Unit = {
    GlStateManager.pushMatrix()
    RenderState.pushAttrib()

    GlStateManager.translate(x, y, 0)
    GlStateManager.scale(0.5f, 0.5f, 1)
    GlStateManager.depthMask(false)
    GlStateManager.enableTexture2D()

    for (i <- 0 until fontTextureProvider.getTextureCount) {
      fontTextureProvider.begin(i)
      GL11.glBegin(GL11.GL_QUADS)
      var tx = 0f
      for (n <- 0 until s.length) {
        val ch = s.charAt(n)
        // Don't render whitespace.
        if (ch != ' ') {
          fontTextureProvider.drawCodePoint(ch, tx, 0, receiver)
        }
        tx += fontTextureProvider.getCharWidth
      }
      GL11.glEnd()
      fontTextureProvider.end(i)
    }

    RenderState.popAttrib()
    GlStateManager.popMatrix()
    GlStateManager.color(1, 1, 1)
  }
}