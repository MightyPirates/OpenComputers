package li.cil.oc.client.renderer.gui

import li.cil.oc.client.TexturePreloader
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.util.{RenderState, PackedColor}
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.client.renderer.{Tessellator, GLAllocation}
import org.lwjgl.opengl.GL11

object BufferRenderer {
  val margin = 7

  val innerMargin = 1

  private var textureManager: Option[TextureManager] = None

  private var displayLists = 0

  def init(tm: TextureManager) = this.synchronized(if (!textureManager.isDefined) {
    textureManager = Some(tm)
    displayLists = GLAllocation.generateDisplayLists(3)
    RenderState.checkError("BufferRenderer.displayLists")
  })

  def compileBackground(bufferWidth: Int, bufferHeight: Int) =
    if (textureManager.isDefined) {
      val innerWidth = innerMargin * 2 + bufferWidth
      val innerHeight = innerMargin * 2 + bufferHeight

      GL11.glNewList(displayLists, GL11.GL_COMPILE)

      textureManager.get.bindTexture(TexturePreloader.guiBorders)

      // Top border (left corner, middle bar, right corner).
      drawBorder(
        0, 0, margin, margin,
        0, 0, 7, 7)
      drawBorder(
        margin, 0, innerWidth, margin,
        7, 0, 8, 7)
      drawBorder(
        margin + innerWidth, 0, margin, margin,
        8, 0, 15, 7)

      // Middle area (left bar, screen background, right bar).
      drawBorder(
        0, margin, margin, innerHeight,
        0, 7, 7, 8)
      drawBorder(
        margin, margin, innerWidth, innerHeight,
        7, 7, 8, 8)
      drawBorder(
        margin + innerWidth, margin, margin, innerHeight,
        8, 7, 15, 8)

      // Bottom border (left corner, middle bar, right corner).
      drawBorder(
        0, margin + innerHeight, margin, margin,
        0, 8, 7, 15)
      drawBorder(
        margin, margin + innerHeight, innerWidth, margin,
        7, 8, 8, 15)
      drawBorder(
        margin + innerWidth, margin + innerHeight, margin, margin,
        8, 8, 15, 15)

      GL11.glEndList()
    }

  def compileText(scale: Double, lines: Array[Array[Char]], colors: Array[Array[Short]], depth: PackedColor.Depth.Value) =
    if (textureManager.isDefined) {
      GL11.glNewList(displayLists + 1, GL11.GL_COMPILE)
      GL11.glPushAttrib(GL11.GL_DEPTH_BUFFER_BIT)
      GL11.glDepthMask(false)

      GL11.glScaled(scale, scale, 1)

      for (i <- 0 until math.min(25, lines.length)) {
        MonospaceFontRenderer.drawString(0, i * MonospaceFontRenderer.fontHeight, lines(i), colors(i), depth)
      }

      GL11.glEndList()
      GL11.glNewList(displayLists + 2, GL11.GL_COMPILE)

      for (i <- 25 until lines.length) {
        MonospaceFontRenderer.drawString(0, i * MonospaceFontRenderer.fontHeight, lines(i), colors(i), depth)
      }

      GL11.glPopAttrib()
      GL11.glEndList()
    }

  def drawBackground() =
    if (textureManager.isDefined) {
      GL11.glCallList(displayLists)
    }

  def drawText() =
    if (textureManager.isDefined) {
      GL11.glCallList(displayLists + 1)
      GL11.glCallList(displayLists + 2)
    }

  private def drawBorder(x: Double, y: Double, w: Double, h: Double, u1: Int, v1: Int, u2: Int, v2: Int) = {
    val u1d = u1 / 16.0
    val u2d = u2 / 16.0
    val v1d = v1 / 16.0
    val v2d = v2 / 16.0
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y + h, 0, u1d, v2d)
    t.addVertexWithUV(x + w, y + h, 0, u2d, v2d)
    t.addVertexWithUV(x + w, y, 0, u2d, v1d)
    t.addVertexWithUV(x, y, 0, u1d, v1d)
    t.draw()
  }
}
