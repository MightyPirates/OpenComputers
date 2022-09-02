package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.api
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.INestedGuiEventHandler
import net.minecraft.client.gui.screen
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.text.StringTextComponent
import org.lwjgl.glfw.GLFW

class Screen(val buffer: api.internal.TextBuffer, val hasMouse: Boolean, val hasKeyboardCallback: () => Boolean, val hasPower: () => Boolean)
  extends screen.Screen(StringTextComponent.EMPTY) with traits.InputBuffer with INestedGuiEventHandler {

  override protected def hasKeyboard = hasKeyboardCallback()

  override protected def bufferX = 8 + x

  override protected def bufferY = 8 + y

  private val bufferMargin = BufferRenderer.margin + BufferRenderer.innerMargin

  private var didClick = false

  private var x, y = 0

  private var mx, my = -1

  override def mouseScrolled(mouseX: Double, mouseY: Double, scroll: Double): Boolean = {
    if (hasMouse) {
      toBufferCoordinates(mouseX, mouseY) match {
        case Some((bx, by)) =>
          buffer.mouseScroll(bx, by, math.signum(scroll).asInstanceOf[Int], null)
          return true
        case _ => // Ignore when out of bounds.
      }
    }
    super.mouseScrolled(mouseX, mouseY, scroll)
  }

  override def mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    if (hasMouse) {
      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
        clickOrDrag(mouseX, mouseY, button)
        return true
      }
    }
    super.mouseClicked(mouseX, mouseY, button)
  }

  override def mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = {
    if (hasMouse) {
      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
        clickOrDrag(mouseX, mouseY, button)
        return true
      }
    }
    super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
  }

  override def mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    if (hasMouse) {
      if (didClick) {
        toBufferCoordinates(mouseX, mouseY) match {
          case Some((bx, by)) => buffer.mouseUp(bx, by, button, null)
          case _ => buffer.mouseUp(-1.0, -1.0, button, null)
        }
      }
      val hasClicked = didClick
      didClick = false
      mx = -1
      my = -1
      if (hasClicked) return true
    }
    super.mouseReleased(mouseX, mouseY, button)
  }

  private def clickOrDrag(mouseX: Double, mouseY: Double, button: Int) {
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

  private def toBufferCoordinates(mouseX: Double, mouseY: Double): Option[(Double, Double)] = {
    val bx = (mouseX - x - bufferMargin) / scale / TextBufferRenderCache.renderer.charRenderWidth
    val by = (mouseY - y - bufferMargin) / scale / TextBufferRenderCache.renderer.charRenderHeight
    val bw = buffer.getViewportWidth
    val bh = buffer.getViewportHeight
    if (bx >= 0 && by >= 0 && bx < bw && by < bh) Some((bx, by))
    else None
  }

  override protected def init(): Unit = {
    super.init()
    minecraft.mouseHandler.releaseMouse()
    KeyBinding.releaseAll()
  }

  override def render(stack: MatrixStack, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.render(stack, mouseX, mouseY, dt)
    drawBufferLayer(stack)
  }

  override def drawBuffer(stack: MatrixStack) {
    stack.translate(x, y, 0)
    BufferRenderer.drawBackground()
    if (hasPower()) {
      stack.translate(bufferMargin, bufferMargin, 0)
      stack.scale(scale.toFloat, scale.toFloat, 1)
      RenderState.makeItBlend()
      BufferRenderer.drawText(stack, buffer)
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
