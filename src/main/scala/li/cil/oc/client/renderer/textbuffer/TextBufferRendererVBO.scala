package li.cil.oc.client.renderer.textbuffer

import java.nio.{ByteBuffer, IntBuffer}

import li.cil.oc.client.renderer.font.FontTextureProvider.Receiver
import li.cil.oc.client.renderer.font.{FontTextureProvider, TextBufferRenderData}
import li.cil.oc.client.renderer.textbuffer.TextBufferRenderCache.fontTextureProvider
import li.cil.oc.util.{PackedColor, RenderState, TextBuffer}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.{GLAllocation, GlStateManager}
import net.minecraft.profiler.Profiler
import net.minecraft.util.math.MathHelper
import org.lwjgl.opengl.{GL11, GL15, GLContext}

class TextBufferRendererVBO extends TextBufferRenderer {
  private val bgBufferVbo = GL15.glGenBuffers()
  private val fgBufferVbo = GL15.glGenBuffers()
  private var bgBufferElems = 0
  private var fgBufferElems = 0

  override def render(profiler: Profiler, fontTextureProvider: FontTextureProvider, currentBuffer: TextBufferRenderData): Boolean = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    if (currentBuffer.dirty) {
      val vboBuffer = TextBufferRendererVBO.getVboBuffer()

      profiler.startSection("vbo_build")
      vboBuffer.drawBuffer(currentBuffer.data, fontTextureProvider, currentBuffer.viewport._1, currentBuffer.viewport._2)
      profiler.endStartSection("vbo_upload")
      vboBuffer.uploadBuffer(bgBufferVbo, fgBufferVbo)
      RenderState.checkError(getClass.getName + ".render: uploading vbo)")
      profiler.endStartSection("gl_draw_bg")

      bgBufferElems = vboBuffer.bgBufferElements()
      fgBufferElems = vboBuffer.fgBufferElements()

      currentBuffer.dirty = false
    } else {
      profiler.startSection("gl_draw_bg")
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

    GlStateManager.glVertexPointer(2, GL11.GL_INT, 12, 4)
    GlStateManager.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 12, 0)

    GL11.glDrawArrays(GL11.GL_QUADS, 0, bgBufferElems)
    RenderState.checkError(getClass.getName + ".render: drawing bg arrays")

    profiler.endStartSection("gl_draw_fg")

    fontTextureProvider.begin(0)

    GlStateManager.enableAlpha()
    GlStateManager.alphaFunc(GL11.GL_GEQUAL, 0.5f)
    GlStateManager.enableTexture2D()

    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fgBufferVbo)
    GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY)

    GlStateManager.glVertexPointer(2, GL11.GL_FLOAT, 20, 4)
    GlStateManager.glTexCoordPointer(2, GL11.GL_FLOAT, 20, 12)
    GlStateManager.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 20, 0)

    GL11.glDrawArrays(GL11.GL_QUADS, 0, fgBufferElems)

    fontTextureProvider.end(0)

    RenderState.checkError(getClass.getName + ".render: drawing fg arrays")

    GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY)
    GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
    GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

    RenderState.popAttrib()
    GlStateManager.popMatrix()

    profiler.endSection()

    RenderState.checkError(getClass.getName + ".render: leaving")

    true
  }

  override def destroy(): Boolean = {
    GL15.glDeleteBuffers(bgBufferVbo)
    GL15.glDeleteBuffers(fgBufferVbo)

    true
  }
}

object TextBufferRendererVBO {
  private val vboBuffers = new ThreadLocal[VBOBuffer] {
    override def initialValue(): VBOBuffer = new VBOBuffer()
  }

  def getVboBuffer(): VBOBuffer = vboBuffers.get()

  def isSupported(fontTextureProvider: FontTextureProvider): Boolean = GLContext.getCapabilities.OpenGL15 && fontTextureProvider.getTextureCount == 1
}

class VBOBuffer extends Receiver {
  val BG_ENTRY_SIZE: Int = 4 * 12
  val FG_ENTRY_SIZE: Int = 4 * 20

  var bgBufferByte: ByteBuffer = GLAllocation.createDirectByteBuffer(MathHelper.smallestEncompassingPowerOfTwo(80 * 25 * BG_ENTRY_SIZE * 4))
  var fgBufferByte: ByteBuffer = GLAllocation.createDirectByteBuffer(MathHelper.smallestEncompassingPowerOfTwo(80 * 25 * FG_ENTRY_SIZE * 4))
  var bgBuffer: IntBuffer = bgBufferByte.asIntBuffer()
  var fgBuffer: IntBuffer = fgBufferByte.asIntBuffer()

  private var bgBufferCount = 0
  private var fgBufferCount = 0
  private var currentColorFg = -1

  def bgBufferElements(): Int = bgBufferCount * 4

  def fgBufferElements(): Int = fgBufferCount * 4

  def uploadBuffer(bgBufferVbo: Int, fgBufferVbo: Int) = {
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bgBufferVbo)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bgBufferByte, GL15.GL_STATIC_DRAW)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, fgBufferVbo)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fgBufferByte, GL15.GL_STATIC_DRAW)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
  }

  def drawBuffer(buffer: TextBuffer, fontTextureProvider: FontTextureProvider, viewportWidth: Int, viewportHeight: Int) {
    val format = buffer.format
    val charWidth = fontTextureProvider.getCharWidth
    val charHeight = fontTextureProvider.getCharHeight

    bgBufferByte.clear()
    fgBufferByte.clear()
    bgBuffer.clear()
    fgBuffer.clear()

    // Background first. We try to merge adjacent backgrounds of the same
    // color to reduce the number of quads we have to draw.
    for (y <- 0 until (viewportHeight min buffer.height)) {
      val color = buffer.color(y)
      var cbg = 0x000000
      var x = 0
      var width = 0
      for (col <- color.map(PackedColor.unpackBackground(_, format)) if x + width < viewportWidth) {
        if (col != cbg) {
          drawBgQuad(charWidth, charHeight, cbg, x, y, width)
          cbg = col
          x += width
          width = 0
        }
        width = width + 1
      }
      drawBgQuad(charWidth, charHeight, cbg, x, y, width)
    }

    // Foreground second. We only have to flush when the color changes, so
    // unless every char has a different color this should be quite efficient.
    for (y <- 0 until (viewportHeight min buffer.height)) {
      val line = buffer.buffer(y)
      val color = buffer.color(y)
      val ty = y * charHeight
      for (_ <- 0 until fontTextureProvider.getTextureCount) {
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

    bgBufferCount = bgBuffer.position() / (BG_ENTRY_SIZE / 4)
    fgBufferCount = fgBuffer.position() / (FG_ENTRY_SIZE / 4)

    bgBufferByte.rewind()
    fgBufferByte.rewind()
    bgBuffer.rewind()
    fgBuffer.rewind()

    bgBufferByte.limit(bgBufferCount * BG_ENTRY_SIZE)
    fgBufferByte.limit(fgBufferCount * FG_ENTRY_SIZE)
  }

  override def draw(x1: Float, x2: Float, y1: Float, y2: Float, u1: Float, u2: Float, v1: Float, v2: Float): Unit = {
    growFgBuffer(FG_ENTRY_SIZE)

    val xf1 = java.lang.Float.floatToRawIntBits(x1)
    val xf2 = java.lang.Float.floatToRawIntBits(x2)
    val yf1 = java.lang.Float.floatToRawIntBits(y1)
    val yf2 = java.lang.Float.floatToRawIntBits(y2)
    val uf1 = java.lang.Float.floatToRawIntBits(u1)
    val uf2 = java.lang.Float.floatToRawIntBits(u2)
    val vf1 = java.lang.Float.floatToRawIntBits(v1)
    val vf2 = java.lang.Float.floatToRawIntBits(v2)
    val colorSwapped = currentColorFg & 0xFF00FF00 | ((currentColorFg & 0xFF0000) >> 16) | ((currentColorFg & 0xFF) << 16)

    fgBuffer.put(colorSwapped)
    fgBuffer.put(xf1).put(yf2).put(uf1).put(vf2)
    fgBuffer.put(colorSwapped)
    fgBuffer.put(xf2).put(yf2).put(uf2).put(vf2)
    fgBuffer.put(colorSwapped)
    fgBuffer.put(xf2).put(yf1).put(uf2).put(vf1)
    fgBuffer.put(colorSwapped)
    fgBuffer.put(xf1).put(yf1).put(uf1).put(vf1)
  }

  private def drawBgQuad(charWidth: Int, charHeight: Int, color: Int, x: Int, y: Int, width: Int): Unit = if (color != 0 && width > 0) {
    val x0 = x * charWidth
    val x1 = (x + width) * charWidth
    val y0 = y * charHeight
    val y1 = (y + 1) * charHeight

    growBgBuffer(BG_ENTRY_SIZE)

    val colorSwapped = color & 0xFF00FF00 | ((color & 0xFF0000) >> 16) | ((color & 0xFF) << 16)

    bgBuffer.put(colorSwapped)
    bgBuffer.put(x0).put(y1)
    bgBuffer.put(colorSwapped)
    bgBuffer.put(x1).put(y1)
    bgBuffer.put(colorSwapped)
    bgBuffer.put(x1).put(y0)
    bgBuffer.put(colorSwapped)
    bgBuffer.put(x0).put(y0)
  }

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