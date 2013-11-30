package li.cil.oc.client.gui

import li.cil.oc.client.PacketSender
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import org.lwjgl.opengl.GL11

class Screen(val screen: tileentity.Screen) extends Buffer {
  protected def buffer = screen.origin.buffer

  private val bufferMargin = BufferRenderer.margin + BufferRenderer.innerMargin

  private var x, y = 0

  override protected def mouseClicked(x: Int, y: Int, button: Int) {
    super.mouseClicked(x, y, button)
    if (button == 0 && screen.tier > 0) {
      val bx = (x - this.x - bufferMargin) / MonospaceFontRenderer.fontWidth + 1
      val by = (y - this.y - bufferMargin) / MonospaceFontRenderer.fontHeight + 1
      val (bw, bh) = screen.buffer.resolution
      if (bx > 0 && by > 0 && bx <= bw && by <= bh) {
        PacketSender.sendMouseClick(buffer.owner, bx, by)
      }
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.drawScreen(mouseX, mouseY, dt)
    drawBufferLayer()
  }

  def drawBuffer() {
    GL11.glTranslatef(x, y, 0)
    BufferRenderer.drawBackground()
    if (screen.hasPower) {
      GL11.glTranslatef(bufferMargin, bufferMargin, 0)
      RenderState.makeItBlend()
      BufferRenderer.drawText()
    }
  }

  protected def changeSize(w: Double, h: Double) = {
    val bw = w * MonospaceFontRenderer.fontWidth
    val bh = h * MonospaceFontRenderer.fontHeight
    val scaleX = (width / (bw + bufferMargin * 2.0)) min 1
    val scaleY = (height / (bh + bufferMargin * 2.0)) min 1
    val scale = scaleX min scaleY
    val innerWidth = (bw * scale).toInt
    val innerHeight = (bh * scale).toInt
    x = (width - (innerWidth + bufferMargin * 2)) / 2
    y = (height - (innerHeight + bufferMargin * 2)) / 2
    BufferRenderer.compileBackground(innerWidth, innerHeight)
    scale
  }
}
