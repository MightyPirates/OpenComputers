package li.cil.oc.client.gui.traits

import li.cil.oc.api
import li.cil.oc.client.KeyBindings
import li.cil.oc.client.Textures
import li.cil.oc.integration.util.NEI
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.Tessellator
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

import scala.collection.mutable

trait InputBuffer extends DisplayBuffer {
  protected def buffer: api.internal.TextBuffer

  override protected def bufferColumns = if (buffer == null) 0 else buffer.getViewportWidth

  override protected def bufferRows = if (buffer == null) 0 else buffer.getViewportHeight

  protected def hasKeyboard: Boolean

  private val pressedKeys = mutable.Map.empty[Int, Char]

  private var showKeyboardMissing = 0L

  override def doesGuiPauseGame = false

  override def initGui() = {
    super.initGui()
    Keyboard.enableRepeatEvents(true)
  }

  override protected def drawBufferLayer() {
    super.drawBufferLayer()

    if (System.currentTimeMillis() - showKeyboardMissing < 1000) {
      Minecraft.getMinecraft.getTextureManager.bindTexture(Textures.guiKeyboardMissing)
      GL11.glDisable(GL11.GL_DEPTH_TEST)
      val t = Tessellator.instance
      t.startDrawingQuads()
      val x = bufferX + buffer.renderWidth - 16
      val y = bufferY + buffer.renderHeight - 16
      t.addVertexWithUV(x, y + 16, 0, 0, 1)
      t.addVertexWithUV(x + 16, y + 16, 0, 1, 1)
      t.addVertexWithUV(x + 16, y, 0, 1, 0)
      t.addVertexWithUV(x, y, 0, 0, 0)
      t.draw()
      GL11.glEnable(GL11.GL_DEPTH_TEST)

      RenderState.checkError(getClass.getName + ".drawBufferLayer: keyboard icon")
    }
  }

  override def onGuiClosed() = {
    super.onGuiClosed()
    if (buffer != null) for ((code, char) <- pressedKeys) {
      buffer.keyUp(char, code, null)
    }
    Keyboard.enableRepeatEvents(false)
  }

  override def handleKeyboardInput() {
    super.handleKeyboardInput()

    if (this.isInstanceOf[GuiContainer] && NEI.isInputFocused) return

    val code = Keyboard.getEventKey
    if (buffer != null && code != Keyboard.KEY_ESCAPE && code != Keyboard.KEY_F11) {
      if (hasKeyboard) {
        if (Keyboard.getEventKeyState) {
          val char = Keyboard.getEventCharacter
          if (!pressedKeys.contains(code) || !ignoreRepeat(char, code)) {
            buffer.keyDown(char, code, null)
            pressedKeys += code -> char
          }
        }
        else pressedKeys.remove(code) match {
          case Some(char) => buffer.keyUp(char, code, null)
          case _ => // Wasn't pressed while viewing the screen.
        }

        if (KeyBindings.isPastingClipboard) {
          buffer.clipboard(GuiScreen.getClipboardString, null)
        }
      }
      else {
        showKeyboardMissing = System.currentTimeMillis()
      }
    }
  }

  override protected def mouseClicked(x: Int, y: Int, button: Int) {
    super.mouseClicked(x, y, button)
    val isMiddleMouseButton = button == 2
    val isBoundMouseButton = KeyBindings.isPastingClipboard
    if (buffer != null && (isMiddleMouseButton || isBoundMouseButton)) {
      if (hasKeyboard) {
        buffer.clipboard(GuiScreen.getClipboardString, null)
      }
      else {
        showKeyboardMissing = System.currentTimeMillis()
      }
    }
  }

  private def ignoreRepeat(char: Char, code: Int) = {
    code == Keyboard.KEY_LCONTROL ||
      code == Keyboard.KEY_RCONTROL ||
      code == Keyboard.KEY_LMENU ||
      code == Keyboard.KEY_RMENU ||
      code == Keyboard.KEY_LSHIFT ||
      code == Keyboard.KEY_RSHIFT ||
      code == Keyboard.KEY_LMETA ||
      code == Keyboard.KEY_RMETA
  }
}
