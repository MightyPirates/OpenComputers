package li.cil.oc.client.renderer.textbuffer

import java.nio.{ByteBuffer, IntBuffer}

import li.cil.oc.Settings
import li.cil.oc.client.renderer.font.FontTextureProvider.Receiver
import li.cil.oc.client.renderer.font.{FontTextureProvider, TextBufferRenderData}
import li.cil.oc.util.{PackedColor, RenderState, TextBuffer}
import net.minecraft.client.renderer.{GLAllocation, GlStateManager}
import org.lwjgl.opengl.{GL11, GL15}

class TextBufferRendererVBO extends TextBufferRenderer with Receiver {
  private val bgBufferVbo = GL15.glGenBuffers()
  private val fgBufferVbo = GL15.glGenBuffers()
  private var currentColorFg = -1
  private var bgBufferCount = 0
  private var fgBufferCount = 0

  override def render(fontTextureProvider: FontTextureProvider, currentBuffer: TextBufferRenderData): Boolean = {
    if (currentBuffer.dirty) {
      RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

      VBORenderBuffers.bgBufferByte.clear()
      VBORenderBuffers.fgBufferByte.clear()
      VBORenderBuffers.bgBuffer.clear()
      VBORenderBuffers.fgBuffer.clear()

      drawBuffer(currentBuffer.data, fontTextureProvider, currentBuffer.viewport._1, currentBuffer.viewport._2)

      bgBufferCount = VBORenderBuffers.bgBuffer.position() / (VBORenderBuffers.BG_ENTRY_SIZE / 4)
      fgBufferCount = VBORenderBuffers.fgBuffer.position() / (VBORenderBuffers.FG_ENTRY_SIZE / 4)

      VBORenderBuffers.bgBufferByte.rewind()
      VBORenderBuffers.fgBufferByte.rewind()
      VBORenderBuffers.bgBuffer.rewind()
      VBORenderBuffers.fgBuffer.rewind()

      VBORenderBuffers.bgBufferByte.limit(bgBufferCount * VBORenderBuffers.BG_ENTRY_SIZE)
      VBORenderBuffers.fgBufferByte.limit(fgBufferCount * VBORenderBuffers.FG_ENTRY_SIZE)

      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bgBufferVbo)
      GL15.glBufferData(GL15.GL_ARRAY_BUFFER, VBORenderBuffers.bgBufferByte, GL15.GL_STATIC_DRAW)
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fgBufferVbo)
      GL15.glBufferData(GL15.GL_ARRAY_BUFFER, VBORenderBuffers.fgBufferByte, GL15.GL_STATIC_DRAW)
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

      currentBuffer.dirty = false
    }

    GlStateManager.pushMatrix()
    RenderState.pushAttrib()

    GlStateManager.scale(0.5f, 0.5f, 1)
    GlStateManager.color(1, 1, 1, 1)

    GlStateManager.depthMask(false)
    GlStateManager.disableBlend()
    GlStateManager.disableTexture2D()
    GlStateManager.disableAlpha()

    RenderState.checkError(getClass.getName + ".render: configure state")

    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bgBufferVbo)
    GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY)
    GlStateManager.glEnableClientState(GL11.GL_COLOR_ARRAY)

    GlStateManager.glVertexPointer(2, GL11.GL_FLOAT, 12, 4)
    GlStateManager.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 12, 0)

    GL11.glDrawArrays(GL11.GL_QUADS, 0, bgBufferCount * 4)
    RenderState.checkError(getClass.getName + ".render: drawing bg arrays")

    fontTextureProvider.begin(0)

    GlStateManager.enableAlpha()
    GlStateManager.alphaFunc(GL11.GL_GEQUAL, 0.5f)
    GlStateManager.enableTexture2D()

    if (Settings.get.textLinearFiltering) {
      GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
    }

    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fgBufferVbo)
    GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY)
    GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
    GlStateManager.glEnableClientState(GL11.GL_COLOR_ARRAY)

    GlStateManager.glVertexPointer(2, GL11.GL_FLOAT, 20, 4)
    GlStateManager.glTexCoordPointer(2, GL11.GL_FLOAT, 20, 12)
    GlStateManager.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 20, 0)

    GL11.glDrawArrays(GL11.GL_QUADS, 0, fgBufferCount * 4)

    fontTextureProvider.end(0)

    RenderState.checkError(getClass.getName + ".render: drawing fg arrays")

    GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY)
    GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
    GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

    RenderState.popAttrib()
    GlStateManager.popMatrix()

    RenderState.checkError(getClass.getName + ".render: leaving")

    true
  }

  override def destroy(): Boolean = {
    true
  }

  private def drawBuffer(buffer: TextBuffer, fontTextureProvider: FontTextureProvider, viewportWidth: Int, viewportHeight: Int) {
    val format = buffer.format
    val charWidth = fontTextureProvider.getCharWidth
    val charHeight = fontTextureProvider.getCharHeight

    // Background first. We try to merge adjacent backgrounds of the same
    // color to reduce the number of quads we have to draw.
    for (y <- 0 until (viewportHeight min buffer.height)) {
      val color = buffer.color(y)
      var cbg = 0x000000
      var x = 0
      var width = 0
      for (col <- color.map(PackedColor.unpackBackground(_, format)) if x + width < viewportWidth) {
        if (col != cbg) {
          drawBgQuad(charWidth, charHeight, cbg | 0xFF000000, x, y, width)
          cbg = col
          x += width
          width = 0
        }
        width = width + 1
      }
      drawBgQuad(charWidth, charHeight, cbg | 0xFF000000, x, y, width)
    }

    // Foreground second. We only have to flush when the color changes, so
    // unless every char has a different color this should be quite efficient.
    for (y <- 0 until (viewportHeight min buffer.height)) {
      val line = buffer.buffer(y)
      val color = buffer.color(y)
      val ty = y * charHeight
      for (i <- 0 until fontTextureProvider.getTextureCount) {
        var tx = 0f
        for (n <- 0 until viewportWidth) {
          val ch = line(n)
          // Don't render whitespace.
          if (ch != ' ') {
            currentColorFg = PackedColor.unpackForeground(color(n), format) | 0xFF000000
            fontTextureProvider.drawCodePoint(ch, tx, ty, this)
          }
          tx += charWidth
        }
      }
    }
  }

  override def draw(x1: Double, x2: Double, y1: Double, y2: Double, u1: Double, u2: Double, v1: Double, v2: Double): Unit = {
    VBORenderBuffers.growFgBuffer(VBORenderBuffers.FG_ENTRY_SIZE)

    val xf1 = java.lang.Float.floatToRawIntBits(x1.toFloat)
    val xf2 = java.lang.Float.floatToRawIntBits(x2.toFloat)
    val yf1 = java.lang.Float.floatToRawIntBits(y1.toFloat)
    val yf2 = java.lang.Float.floatToRawIntBits(y2.toFloat)
    val uf1 = java.lang.Float.floatToRawIntBits(u1.toFloat)
    val uf2 = java.lang.Float.floatToRawIntBits(u2.toFloat)
    val vf1 = java.lang.Float.floatToRawIntBits(v1.toFloat)
    val vf2 = java.lang.Float.floatToRawIntBits(v2.toFloat)
    val colorSwapped = currentColorFg & 0xFF00FF00 | ((currentColorFg & 0xFF0000) >> 16) | ((currentColorFg & 0xFF) << 16)

    VBORenderBuffers.fgBuffer.put(colorSwapped)
    VBORenderBuffers.fgBuffer.put(xf1).put(yf2).put(uf1).put(vf2)
    VBORenderBuffers.fgBuffer.put(colorSwapped)
    VBORenderBuffers.fgBuffer.put(xf2).put(yf2).put(uf2).put(vf2)
    VBORenderBuffers.fgBuffer.put(colorSwapped)
    VBORenderBuffers.fgBuffer.put(xf2).put(yf1).put(uf2).put(vf1)
    VBORenderBuffers.fgBuffer.put(colorSwapped)
    VBORenderBuffers.fgBuffer.put(xf1).put(yf1).put(uf1).put(vf1)
  }

  private def drawBgQuad(charWidth: Int, charHeight: Int, color: Int, x: Int, y: Int, width: Int): Unit = if (color != 0 && width > 0) {
    val x0 = x * charWidth
    val x1 = (x + width) * charWidth
    val y0 = y * charHeight
    val y1 = (y + 1) * charHeight

    VBORenderBuffers.growBgBuffer(VBORenderBuffers.BG_ENTRY_SIZE)

    val xf0 = java.lang.Float.floatToRawIntBits(x0)
    val xf1 = java.lang.Float.floatToRawIntBits(x1)
    val yf0 = java.lang.Float.floatToRawIntBits(y0)
    val yf1 = java.lang.Float.floatToRawIntBits(y1)
    val colorSwapped = color & 0xFF00FF00 | ((color & 0xFF0000) >> 16) | ((color & 0xFF) << 16)

    VBORenderBuffers.bgBuffer.put(colorSwapped)
    VBORenderBuffers.bgBuffer.put(xf0).put(yf1)
    VBORenderBuffers.bgBuffer.put(colorSwapped)
    VBORenderBuffers.bgBuffer.put(xf1).put(yf1)
    VBORenderBuffers.bgBuffer.put(colorSwapped)
    VBORenderBuffers.bgBuffer.put(xf1).put(yf0)
    VBORenderBuffers.bgBuffer.put(colorSwapped)
    VBORenderBuffers.bgBuffer.put(xf0).put(yf0)
  }
}

object VBORenderBuffers {
  var bgBufferByte: ByteBuffer = GLAllocation.createDirectByteBuffer(131072)
  var fgBufferByte: ByteBuffer = GLAllocation.createDirectByteBuffer(262144)
  var bgBuffer: IntBuffer = bgBufferByte.asIntBuffer()
  var fgBuffer: IntBuffer = fgBufferByte.asIntBuffer()
  val BG_ENTRY_SIZE: Int = 4 * 12
  val FG_ENTRY_SIZE: Int = 4 * 20

  def growBgBuffer(size: Int): Unit = {
    while (bgBufferByte.remaining() < size) {
      val oldBgBufferByte = bgBufferByte
      oldBgBufferByte.position(0)
      bgBufferByte = GLAllocation.createDirectByteBuffer(oldBgBufferByte.capacity() * 2)
      bgBufferByte.put(oldBgBufferByte)
      bgBuffer = bgBufferByte.asIntBuffer()
      bgBuffer.position(bgBufferByte.position() >> 2)
    }
  }

  def growFgBuffer(size: Int): Unit = {
    while (fgBufferByte.remaining() < size) {
      val oldFgBufferByte = fgBufferByte
      oldFgBufferByte.position(0)
      fgBufferByte = GLAllocation.createDirectByteBuffer(oldFgBufferByte.capacity() * 2)
      fgBufferByte.put(oldFgBufferByte)
      fgBuffer = fgBufferByte.asIntBuffer()
      fgBuffer.position(fgBufferByte.position() >> 2)
    }
  }
}