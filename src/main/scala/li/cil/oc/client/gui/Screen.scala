package li.cil.oc.client.gui

import li.cil.oc.api
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.client.renderer.textbuffer.TextBufferRenderCache
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse

class Screen(val buffer: api.internal.TextBuffer, val hasMouse: Boolean, val hasKeyboardCallback: () => Boolean, val hasPower: () => Boolean) extends traits.InputBuffer {
  override protected def hasKeyboard = hasKeyboardCallback()

  override protected def bufferX = 8 + x

  override protected def bufferY = 8 + y

  private val bufferMargin = BufferRenderer.margin + BufferRenderer.innerMargin

  private var didClick = false

  private var x, y = 0

  private var mx, my = -1

  override def handleMouseInput() {
    super.handleMouseInput()
    if (hasMouse && Mouse.hasWheel && Mouse.getEventDWheel != 0) {
      val mouseX = Mouse.getEventX * width / mc.displayWidth
      val mouseY = height - Mouse.getEventY * height / mc.displayHeight - 1
      toBufferCoordinates(mouseX, mouseY) match {
        case Some((bx, by)) =>
          val scroll = math.signum(Mouse.getEventDWheel)
          buffer.mouseScroll(bx, by, scroll, null)
        case _ => // Ignore when out of bounds.
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

  override protected def mouseReleased(mouseX: Int, mouseY: Int, button: Int) {
    super.mouseReleased(mouseX, mouseY, button)
    if (hasMouse && button >= 0) {
      if (didClick) {
        toBufferCoordinates(mouseX, mouseY) match {
          case Some((bx, by)) => buffer.mouseUp(bx, by, button, null)
          case _ => buffer.mouseUp(-1.0, -1.0, button, null)
        }
      }
      didClick = false
      mx = -1
      my = -1
    }
  }

  private def clickOrDrag(mouseX: Int, mouseY: Int, button: Int) {
    toBufferCoordinates(mouseX, mouseY) match {
      case Some((bx, by)) if bx.toInt != mx || (by*2).toInt != my =>
        if (mx >= 0 && my >= 0) buffer.mouseDrag(bx, by, button, null)
        else buffer.mouseDown(bx, by, button, null)
        didClick = true
        mx = bx.toInt
        my = (by*2).toInt // for high precision mode, sends some unnecessary packets when not using it, but eh
      case _ =>
    }
  }

  private def toBufferCoordinates(mouseX: Int, mouseY: Int): Option[(Double, Double)] = {
    val bx = (mouseX - x - bufferMargin) / scale / (TextBufferRenderCache.fontTextureProvider.getCharWidth / 2)
    val by = (mouseY - y - bufferMargin) / scale / (TextBufferRenderCache.fontTextureProvider.getCharHeight / 2)
    val bw = buffer.getViewportWidth
    val bh = buffer.getViewportHeight
    if (bx >= 0 && by >= 0 && bx < bw && by < bh) Some((bx, by))
    else None
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.drawScreen(mouseX, mouseY, dt)
    drawBufferLayer()
  }

  override def drawBuffer() {
    GlStateManager.translate(x, y, 0)
    BufferRenderer.drawBackground()
    if (hasPower()) {
      GlStateManager.translate(bufferMargin, bufferMargin, 0)
      GlStateManager.scale(scale, scale, 1)
      RenderState.makeItBlend()
      BufferRenderer.drawText(buffer)
    }
  }

  override protected def changeSize(w: Double, h: Double, recompile: Boolean) = {
    val bw = buffer.renderWidth
    val bh = buffer.renderHeight
    val scaleX = math.min(width / (bw + bufferMargin * 2.0), 1)
    val scaleY = math.min(height / (bh + bufferMargin * 2.0), 1)
    val scale = math.min(scaleX, scaleY)
    val innerWidth = (bw * scale).toInt
    val innerHeight = (bh * scale).toInt
    x = (width - (innerWidth + bufferMargin * 2)) / 2
    y = (height - (innerHeight + bufferMargin * 2)) / 2
    if (recompile) {
      BufferRenderer.compileBackground(innerWidth, innerHeight)
    }
    scale
  }
}
