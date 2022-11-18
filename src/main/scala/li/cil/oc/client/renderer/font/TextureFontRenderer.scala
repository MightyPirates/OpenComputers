package li.cil.oc.client.renderer.font

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.IVertexBuilder
import li.cil.oc.Settings
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.util.PackedColor
import li.cil.oc.util.RenderState
import li.cil.oc.util.TextBuffer
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.util.math.vector.Matrix4f
import net.minecraft.util.math.vector.Vector4f
import org.lwjgl.opengl.GL11

/**
  * Base class for texture based font rendering.
  *
  * Provides common logic for the static one (using an existing texture) and the
  * dynamic one (generating textures on the fly from a font).
  */
abstract class TextureFontRenderer {
  protected final val basicChars = """☺☻♥♦♣♠•◘○◙♂♀♪♫☼►◄↕‼¶§▬↨↑↓→←∟↔▲▼ !"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{|}~⌂ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥₧ƒáíóúñÑªº¿⌐¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αßΓπΣσµτΦΘΩδ∞φε∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■"""

  def charRenderWidth = charWidth / 2

  def charRenderHeight = charHeight / 2

  /**
    * If drawString() is called inside display lists this should be called
    * beforehand, outside the display list, to ensure no characters have to
    * be generated inside the draw call.
    */
  def generateChars(chars: Array[Char]) {
    RenderSystem.enableTexture()
    for (char <- chars) {
      generateChar(char)
    }
  }

  def drawBuffer(stack: MatrixStack, renderBuff: IRenderTypeBuffer, buffer: TextBuffer, viewportWidth: Int, viewportHeight: Int) {
    val format = buffer.format

    stack.pushPose()

    stack.scale(0.5f, 0.5f, 1)

    // Background first. We try to merge adjacent backgrounds of the same
    // color to reduce the number of quads we have to draw.
    var quadBuilder: IVertexBuilder = null
    for (y <- 0 until (viewportHeight min buffer.height)) {
      val color = buffer.color(y)
      var cbg = 0x000000
      var x = 0
      var width = 0
      for (col <- color.map(PackedColor.unpackBackground(_, format)) if x + width < viewportWidth) {
        if (col != cbg) {
          if (quadBuilder == null) quadBuilder = renderBuff.getBuffer(RenderTypes.FONT_QUAD)
          drawQuad(quadBuilder, stack.last.pose, cbg, x, y, width)
          cbg = col
          x += width
          width = 0
        }
        width = width + 1
      }
      drawQuad(quadBuilder, stack.last.pose, cbg, x, y, width)
    }

    // Foreground second. We only have to flush when the texture changes.
    for (i <- 0 until textureCount) {
      // Initialized early because our RenderCache drops empty buffers.
      val fontBuilder = renderBuff.getBuffer(selectType(i))
      for (y <- 0 until (viewportHeight min buffer.height)) {
        val line = buffer.buffer(y)
        val color = buffer.color(y)
        val ty = y * charHeight
        var tx = 0f
        for (n <- 0 until viewportWidth) {
          val ch = line(n)
          // Don't render whitespace.
          if (ch != ' ') {
            val col = PackedColor.unpackForeground(color(n), format)
            drawChar(fontBuilder, stack.last.pose, col, tx, ty, ch)
          }
          tx += charWidth
        }
      }
    }

    stack.popPose()
  }

  def drawString(stack: MatrixStack, s: String, x: Int, y: Int): Unit = {
    stack.pushPose()
    RenderState.pushAttrib()

    stack.translate(x, y, 0)
    stack.scale(0.5f, 0.5f, 1)
    RenderSystem.depthMask(false)

    for (i <- 0 until textureCount) {
      bindTexture(i)
      GL11.glBegin(GL11.GL_QUADS)
      var tx = 0f
      for (n <- 0 until s.length) {
        val ch = s.charAt(n)
        // Don't render whitespace.
        if (ch != ' ') {
          drawChar(stack.last.pose, tx, 0, ch)
        }
        tx += charWidth
      }
      GL11.glEnd()
    }

    RenderState.popAttrib()
    stack.popPose()
    RenderSystem.color3f(1, 1, 1)
  }

  protected def charWidth: Int

  protected def charHeight: Int

  protected def textureCount: Int

  protected def bindTexture(index: Int): Unit

  protected def selectType(index: Int): RenderType

  protected def generateChar(char: Char): Unit

  protected def drawChar(matrix: Matrix4f, tx: Float, ty: Float, char: Char): Unit

  protected def drawChar(builder: IVertexBuilder, matrix: Matrix4f, color: Int, tx: Float, ty: Float, char: Char): Unit

  private def drawQuad(builder: IVertexBuilder, matrix: Matrix4f, color: Int, x: Int, y: Int, width: Int) = if (color != 0 && width > 0) {
    val x0 = x * charWidth
    val x1 = (x + width) * charWidth
    val y0 = y * charHeight
    val y1 = (y + 1) * charHeight
    val r = ((color >> 16) & 0xFF) / 255f
    val g = ((color >> 8) & 0xFF) / 255f
    val b = (color & 0xFF) / 255f
    builder.vertex(matrix, x0, y1, 0).color(r, g, b, 1f).endVertex()
    builder.vertex(matrix, x1, y1, 0).color(r, g, b, 1f).endVertex()
    builder.vertex(matrix, x1, y0, 0).color(r, g, b, 1f).endVertex()
    builder.vertex(matrix, x0, y0, 0).color(r, g, b, 1f).endVertex()
  }
}
