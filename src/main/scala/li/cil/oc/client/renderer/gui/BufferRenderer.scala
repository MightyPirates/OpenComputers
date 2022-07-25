package li.cil.oc.client.renderer.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.texture.TextureManager
import org.lwjgl.opengl.GL11

object BufferRenderer {
  val margin = 7

  val innerMargin = 1

  private var textureManager: Option[TextureManager] = None

  private var displayLists = 0

  def init(tm: TextureManager) = this.synchronized(if (textureManager.isEmpty) {
    RenderState.checkError(getClass.getName + ".displayLists: entering (aka: wasntme)")

    textureManager = Some(tm)
    displayLists = GL11.glGenLists(2)

    RenderState.checkError(getClass.getName + ".displayLists: leaving")
  })

  def compileBackground(bufferWidth: Int, bufferHeight: Int, forRobot: Boolean = false) =
    if (textureManager.isDefined) {
      RenderState.checkError(getClass.getName + ".compileBackground: entering (aka: wasntme)")

      val innerWidth = innerMargin * 2 + bufferWidth
      val innerHeight = innerMargin * 2 + bufferHeight

      GL11.glNewList(displayLists, GL11.GL_COMPILE)

      Textures.bind(Textures.GUI.Borders)

      GL11.glBegin(GL11.GL_QUADS)

      val margin = if (forRobot) 2 else 7
      val (c0, c1, c2, c3) = if (forRobot) (5, 7, 9, 11) else (0, 7, 9, 16)

      // Top border (left corner, middle bar, right corner).
      drawBorder(
        0, 0, margin, margin,
        c0, c0, c1, c1)
      drawBorder(
        margin, 0, innerWidth, margin,
        c1 + 0.25, c0, c2 - 0.25, c1)
      drawBorder(
        margin + innerWidth, 0, margin, margin,
        c2, c0, c3, c1)

      // Middle area (left bar, screen background, right bar).
      drawBorder(
        0, margin, margin, innerHeight,
        c0, c1 + 0.25, c1, c2 - 0.25)
      drawBorder(
        margin, margin, innerWidth, innerHeight,
        c1 + 0.25, c1 + 0.25, c2 - 0.25, c2 - 0.25)
      drawBorder(
        margin + innerWidth, margin, margin, innerHeight,
        c2, c1 + 0.25, c3, c2 - 0.25)

      // Bottom border (left corner, middle bar, right corner).
      drawBorder(
        0, margin + innerHeight, margin, margin,
        c0, c2, c1, c3)
      drawBorder(
        margin, margin + innerHeight, innerWidth, margin,
        c1 + 0.25, c2, c2 - 0.25, c3)
      drawBorder(
        margin + innerWidth, margin + innerHeight, margin, margin,
        c2, c2, c3, c3)

      GL11.glEnd()

      GL11.glEndList()

      RenderState.checkError(getClass.getName + ".compileBackground: leaving")
    }

  def drawBackground() =
    if (textureManager.isDefined) {
      GL11.glCallList(displayLists)
    }

  def drawText(stack: MatrixStack, screen: api.internal.TextBuffer) =
    if (textureManager.isDefined) {
      RenderState.pushAttrib()
      RenderSystem.depthMask(false)
      val changed = screen.renderText(stack)
      RenderSystem.depthMask(true)
      RenderState.popAttrib()
      changed
    }
    else false

  private def drawBorder(x: Double, y: Double, w: Double, h: Double, u1: Double, v1: Double, u2: Double, v2: Double) = {
    val u1d = u1 / 16.0
    val u2d = u2 / 16.0
    val v1d = v1 / 16.0
    val v2d = v2 / 16.0
    GL11.glTexCoord2d(u1d, v2d)
    GL11.glVertex3d(x, y + h, 0)
    GL11.glTexCoord2d(u2d, v2d)
    GL11.glVertex3d(x + w, y + h, 0)
    GL11.glTexCoord2d(u2d, v1d)
    GL11.glVertex3d(x + w, y, 0)
    GL11.glTexCoord2d(u1d, v1d)
    GL11.glVertex3d(x, y, 0)
  }
}
