package li.cil.oc.client.gui.traits

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.api
import li.cil.oc.client.KeyBindings
import li.cil.oc.client.Textures
import li.cil.oc.integration.util.ItemSearch
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.inventory.ContainerScreen
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.util.InputMappings
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11

import scala.collection.mutable

trait InputBuffer extends DisplayBuffer {
  protected def buffer: api.internal.TextBuffer

  override protected def bufferColumns = if (buffer == null) 0 else buffer.getViewportWidth

  override protected def bufferRows = if (buffer == null) 0 else buffer.getViewportHeight

  protected def hasKeyboard: Boolean

  private val pressedKeys = mutable.Set.empty[Int]

  private var showKeyboardMissing = 0L

  override def isPauseScreen = false

  override protected def init() = {
    super.init()
    Minecraft.getInstance.keyboardHandler.setSendRepeatsToGui(true)
  }

  override protected def drawBufferLayer(stack: MatrixStack) {
    super.drawBufferLayer(stack)

    if (System.currentTimeMillis() - showKeyboardMissing < 1000) {
      Textures.bind(Textures.GUI.KeyboardMissing)
      RenderSystem.disableDepthTest()

      val x = bufferX + buffer.renderWidth - 16
      val y = bufferY + buffer.renderHeight - 16

      val t = Tessellator.getInstance
      val r = t.getBuilder
      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
      r.vertex(stack.last.pose, x, y + 16, 0).uv(0, 1).endVertex()
      r.vertex(stack.last.pose, x + 16, y + 16, 0).uv(1, 1).endVertex()
      r.vertex(stack.last.pose, x + 16, y, 0).uv(1, 0).endVertex()
      r.vertex(stack.last.pose, x, y, 0).uv(0, 0).endVertex()
      t.end()

      RenderSystem.enableDepthTest()

      RenderState.checkError(getClass.getName + ".drawBufferLayer: keyboard icon")
    }
  }

  override def removed() = {
    super.removed()
    Minecraft.getInstance.keyboardHandler.setSendRepeatsToGui(false)
    if (buffer != null) for (code <- pressedKeys) {
      buffer.keyUp('\u0000', code, null)
    }
  }

  def onInput(input: InputMappings.Input): Boolean = {
    if (KeyBindings.clipboardPaste.isActiveAndMatches(input)) {
      if (buffer != null) {
        if (hasKeyboard) buffer.clipboard(Minecraft.getInstance.keyboardHandler.getClipboard, null)
        else showKeyboardMissing = System.currentTimeMillis()
      }
      return true
    }
    false
  }

  override def charTyped(codePt: Char, mods: Int): Boolean = {
    if (!this.isInstanceOf[ContainerScreen[_]] || !ItemSearch.isInputFocused) {
      if (buffer != null) {
        if (hasKeyboard) {
          buffer.keyDown(codePt, 0, null)
          buffer.keyUp(codePt, 0, null)
        }
        else showKeyboardMissing = System.currentTimeMillis()
        return true
      }
    }
    super.charTyped(codePt, mods)
  }

  override def keyPressed(keyCode: Int, scanCode: Int, mods: Int): Boolean = {
    if (onInput(InputMappings.getKey(keyCode, scanCode))) return true
    if (!this.isInstanceOf[ContainerScreen[_]] || !ItemSearch.isInputFocused) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE && shouldCloseOnEsc) {
        onClose()
        return true
      }
      if (buffer != null && keyCode != GLFW.GLFW_KEY_UNKNOWN) {
        if (hasKeyboard) {
          if (pressedKeys.add(keyCode) || !ignoreRepeat(keyCode)) {
            buffer.keyDown('\u0000', keyCode, null)
          }
        }
        else showKeyboardMissing = System.currentTimeMillis()
        return true
      }
    }
    super.keyPressed(keyCode, scanCode, mods)
  }

  override def keyReleased(keyCode: Int, scanCode: Int, mods: Int): Boolean = {
    if (pressedKeys.remove(keyCode)) {
      buffer.keyUp('\u0000', keyCode, null)
      return true
    }
    // Wasn't pressed while viewing the screen.
    super.keyReleased(keyCode, scanCode, mods)
  }

  override def mouseClicked(x: Double, y: Double, button: Int): Boolean = {
    if (onInput(InputMappings.Type.MOUSE.getOrCreate(button))) return true
    if (buffer != null && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
      if (hasKeyboard) buffer.clipboard(Minecraft.getInstance.keyboardHandler.getClipboard, null)
      else showKeyboardMissing = System.currentTimeMillis()
      return true
    }
    super.mouseClicked(x, y, button)
  }

  private def ignoreRepeat(keyCode: Int) = {
    keyCode == GLFW.GLFW_KEY_LEFT_CONTROL ||
      keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL ||
      keyCode == GLFW.GLFW_KEY_MENU ||
      keyCode == GLFW.GLFW_KEY_LEFT_ALT ||
      keyCode == GLFW.GLFW_KEY_RIGHT_ALT ||
      keyCode == GLFW.GLFW_KEY_LEFT_SHIFT ||
      keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT ||
      keyCode == GLFW.GLFW_KEY_LEFT_SUPER ||
      keyCode == GLFW.GLFW_KEY_RIGHT_SUPER
  }
}
