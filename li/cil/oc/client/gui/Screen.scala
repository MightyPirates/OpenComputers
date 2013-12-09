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
  
  private var mx, my = 0

  override protected def mouseClicked(mouseX: Int, mouseY: Int, button: Int) {
    super.mouseClicked(mouseX, mouseY, button)
    if (button == 0 && screen.tier > 0) {
      click(mouseX, mouseY, force = true)
    }
  }

  protected override def mouseClickMove(mouseX: Int, mouseY: Int, button: Int, timeSinceLast: Long) {
    super.mouseClicked(mouseX, mouseY, button)
    if (button == 0 && screen.tier > 0 && timeSinceLast > 10) {
      click(mouseX, mouseY)
    }
  }

  private def click(mouseX: Int, mouseY: Int, force: Boolean = false) = {
    val bx = (mouseX - x - bufferMargin) / MonospaceFontRenderer.fontWidth + 1
    val by = (mouseY - y - bufferMargin) / MonospaceFontRenderer.fontHeight + 1
    val (bw, bh) = screen.buffer.resolution
    if (bx > 0 && by > 0 && bx <= bw && by <= bh) {
      if (bx != mx || by != my || force) {
        mx = bx
        my = by
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
    val scaleX = math.min(width / (bw + bufferMargin * 2.0), 1)
    val scaleY = math.min(height / (bh + bufferMargin * 2.0), 1)
    val scale = math.min(scaleX, scaleY)
    val innerWidth = (bw * scale).toInt
    val innerHeight = (bh * scale).toInt
    x = (width - (innerWidth + bufferMargin * 2)) / 2
    y = (height - (innerHeight + bufferMargin * 2)) / 2
    BufferRenderer.compileBackground(innerWidth, innerHeight)
    scale
  }
}
