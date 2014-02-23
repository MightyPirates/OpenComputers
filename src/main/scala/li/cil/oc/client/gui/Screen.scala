package li.cil.oc.client.gui

import li.cil.oc.client.PacketSender
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.common
import li.cil.oc.util.RenderState
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11

class Screen(val buffer: common.component.Buffer, val hasMouse: Boolean, val hasPower: () => Boolean) extends Buffer {
  private val bufferMargin = BufferRenderer.margin + BufferRenderer.innerMargin

  private var x, y = 0

  private var mx, my = 0

  override def handleMouseInput() {
    super.handleMouseInput()
    if (Mouse.hasWheel && Mouse.getEventDWheel != 0) {
      val mouseX = Mouse.getEventX * width / mc.displayWidth
      val mouseY = height - Mouse.getEventY * height / mc.displayHeight - 1
      val bx = (mouseX - x - bufferMargin) / MonospaceFontRenderer.fontWidth + 1
      val by = (mouseY - y - bufferMargin) / MonospaceFontRenderer.fontHeight + 1
      val (bw, bh) = buffer.resolution
      if (bx > 0 && by > 0 && bx <= bw && by <= bh) {
        val scroll = math.signum(Mouse.getEventDWheel)
        PacketSender.sendMouseScroll(buffer, bx, by, scroll)
      }
    }
  }

  override protected def mouseClicked(mouseX: Int, mouseY: Int, button: Int) {
    super.mouseClicked(mouseX, mouseY, button)
    if (hasMouse) {
      if (button == 0 || button == 1) {
        clickOrDrag(mouseX, mouseY, button)
      }
    }
  }

  protected override def mouseClickMove(mouseX: Int, mouseY: Int, button: Int, timeSinceLast: Long) {
    super.mouseClickMove(mouseX, mouseY, button, timeSinceLast)
    if (hasMouse && timeSinceLast > 10) {
      if (button == 0 || button == 1) {
        clickOrDrag(mouseX, mouseY, button)
      }
    }
  }

  protected override def mouseMovedOrUp(mouseX: Int, mouseY: Int, which: Int) {
    super.mouseMovedOrUp(mouseX, mouseY, which)
    if (which == 0) {
      mx = 0
      my = 0
    }
  }

  private def clickOrDrag(mouseX: Int, mouseY: Int, button: Int) {
    val bx = ((mouseX - x - bufferMargin) / scale / MonospaceFontRenderer.fontWidth).toInt + 1
    val by = ((mouseY - y - bufferMargin) / scale / MonospaceFontRenderer.fontHeight).toInt + 1
    val (bw, bh) = buffer.resolution
    if (bx > 0 && by > 0 && bx <= bw && by <= bh) {
      if (bx != mx || by != my) {
        PacketSender.sendMouseClick(buffer, bx, by, mx > 0 && my > 0, button)
        mx = bx
        my = by
      }
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.drawScreen(mouseX, mouseY, dt)
    drawBufferLayer()
  }

  override def drawBuffer() {
    GL11.glTranslatef(x, y, 0)
    BufferRenderer.drawBackground()
    if (hasPower()) {
      GL11.glTranslatef(bufferMargin, bufferMargin, 0)
      RenderState.makeItBlend()
      BufferRenderer.drawText()
    }
  }

  override protected def changeSize(w: Double, h: Double) = {
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
