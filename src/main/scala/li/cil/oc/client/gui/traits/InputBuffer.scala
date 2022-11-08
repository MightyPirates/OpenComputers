package li.cil.oc.client.gui.traits

import java.util.Arrays

import com.mojang.blaze3d.matrix.MatrixStack
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

  private val pressedKeys = mutable.Map.empty[Int, Char]

  private var showKeyboardMissing = 0L

  private var hasQueuedKey = false
  private var queuedKey = 0
  private var queuedChar = '\u0000'

  protected def pushQueuedKey(keyCode: Int): Unit = {
    flushQueuedKey()
    hasQueuedKey = true
    queuedKey = keyCode
    queuedChar = GLFWTranslator.keyToChar(keyCode)
  }

  protected def pushQueuedChar(char: Char): Unit = {
    if (hasQueuedKey) {
      queuedChar = char
      flushQueuedKey()
    }
  }

  protected def flushQueuedKey(): Unit = {
    if (hasQueuedKey) {
      hasQueuedKey = false
      if (!pressedKeys.contains(queuedKey)) {
        val lwjglCode = GLFWTranslator.glfwToLWJGL(queuedKey)
        if (lwjglCode > 0) {
          pressedKeys(queuedKey) = queuedChar
          if (buffer != null) buffer.keyDown(queuedChar, lwjglCode, null)
        }
        else if (queuedChar > 0) {
          pressedKeys(queuedKey) = queuedChar
          if (buffer != null) buffer.keyDown(queuedChar, 0, null)
        }
      }
    }
  }

  override def isPauseScreen = false

  override protected def init() = {
    super.init()
    Minecraft.getInstance.keyboardHandler.setSendRepeatsToGui(true)
  }

  override protected def drawBufferLayer(stack: MatrixStack) {
    super.drawBufferLayer(stack)

    if (System.currentTimeMillis() - showKeyboardMissing < 1000) {
      Textures.bind(Textures.GUI.KeyboardMissing)

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

      RenderState.checkError(getClass.getName + ".drawBufferLayer: keyboard icon")
    }
  }

  override def tick(): Unit = {
    super.tick()
    flushQueuedKey()
  }

  override def removed() = {
    super.removed()
    Minecraft.getInstance.keyboardHandler.setSendRepeatsToGui(false)
    if (buffer != null) {
      flushQueuedKey()
      for ((code, char) <- pressedKeys) {
        val lwjglCode = GLFWTranslator.glfwToLWJGL(code)
        if (lwjglCode > 0) buffer.keyUp(char, lwjglCode, null)
        else buffer.keyUp(char, 0, null)
      }
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
        if (hasKeyboard) pushQueuedChar(codePt)
        else showKeyboardMissing = System.currentTimeMillis()
        return true
      }
    }
    super.charTyped(codePt, mods)
  }

  override def keyPressed(keyCode: Int, scanCode: Int, mods: Int): Boolean = {
    if (!this.isInstanceOf[ContainerScreen[_]] || !ItemSearch.isInputFocused) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE && shouldCloseOnEsc) {
        onClose()
        return true
      }
      if (onInput(InputMappings.getKey(keyCode, scanCode))) return true
      if (buffer != null && keyCode != GLFW.GLFW_KEY_UNKNOWN) {
        if (hasKeyboard) pushQueuedKey(keyCode)
        else showKeyboardMissing = System.currentTimeMillis()
        return true
      }
    }
    super.keyPressed(keyCode, scanCode, mods)
  }

  override def keyReleased(keyCode: Int, scanCode: Int, mods: Int): Boolean = {
    if (!this.isInstanceOf[ContainerScreen[_]] || !ItemSearch.isInputFocused) {
      flushQueuedKey()
      pressedKeys.remove(keyCode) match {
        case Some(char) => {
          val lwjglCode = GLFWTranslator.glfwToLWJGL(keyCode)
          if (lwjglCode > 0) {
            buffer.keyUp(char, lwjglCode, null)
            return true
          }
          else if (char > 0) {
            buffer.keyUp(char, 0, null)
            return true
          }
        }
        case None =>
      }
      // Wasn't pressed while viewing the screen.
    }
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

object GLFWTranslator {
  private val toLWJGL = new Array[Int](GLFW.GLFW_KEY_LAST + 1)
  Arrays.fill(toLWJGL, -1)

  /** Printable keys. */
  toLWJGL(GLFW.GLFW_KEY_SPACE) = 0x39
  toLWJGL(GLFW.GLFW_KEY_APOSTROPHE) = 0x28
  toLWJGL(GLFW.GLFW_KEY_COMMA) = 0x33
  toLWJGL(GLFW.GLFW_KEY_MINUS) = 0x0C
  toLWJGL(GLFW.GLFW_KEY_PERIOD) = 0x34
  toLWJGL(GLFW.GLFW_KEY_SLASH) = 0x35
  toLWJGL(GLFW.GLFW_KEY_0) = 0x0B
  toLWJGL(GLFW.GLFW_KEY_1) = 0x02
  toLWJGL(GLFW.GLFW_KEY_2) = 0x03
  toLWJGL(GLFW.GLFW_KEY_3) = 0x04
  toLWJGL(GLFW.GLFW_KEY_4) = 0x05
  toLWJGL(GLFW.GLFW_KEY_5) = 0x06
  toLWJGL(GLFW.GLFW_KEY_6) = 0x07
  toLWJGL(GLFW.GLFW_KEY_7) = 0x08
  toLWJGL(GLFW.GLFW_KEY_8) = 0x09
  toLWJGL(GLFW.GLFW_KEY_9) = 0x0A
  toLWJGL(GLFW.GLFW_KEY_SEMICOLON) = 0x27
  toLWJGL(GLFW.GLFW_KEY_EQUAL) = 0x0D
  toLWJGL(GLFW.GLFW_KEY_A) = 0x1E
  toLWJGL(GLFW.GLFW_KEY_B) = 0x30
  toLWJGL(GLFW.GLFW_KEY_C) = 0x2E
  toLWJGL(GLFW.GLFW_KEY_D) = 0x20
  toLWJGL(GLFW.GLFW_KEY_E) = 0x12
  toLWJGL(GLFW.GLFW_KEY_F) = 0x21
  toLWJGL(GLFW.GLFW_KEY_G) = 0x22
  toLWJGL(GLFW.GLFW_KEY_H) = 0x23
  toLWJGL(GLFW.GLFW_KEY_I) = 0x17
  toLWJGL(GLFW.GLFW_KEY_J) = 0x24
  toLWJGL(GLFW.GLFW_KEY_K) = 0x25
  toLWJGL(GLFW.GLFW_KEY_L) = 0x26
  toLWJGL(GLFW.GLFW_KEY_M) = 0x32
  toLWJGL(GLFW.GLFW_KEY_N) = 0x31
  toLWJGL(GLFW.GLFW_KEY_O) = 0x18
  toLWJGL(GLFW.GLFW_KEY_P) = 0x19
  toLWJGL(GLFW.GLFW_KEY_Q) = 0x10
  toLWJGL(GLFW.GLFW_KEY_R) = 0x13
  toLWJGL(GLFW.GLFW_KEY_S) = 0x1F
  toLWJGL(GLFW.GLFW_KEY_T) = 0x14
  toLWJGL(GLFW.GLFW_KEY_U) = 0x16
  toLWJGL(GLFW.GLFW_KEY_V) = 0x2F
  toLWJGL(GLFW.GLFW_KEY_W) = 0x11
  toLWJGL(GLFW.GLFW_KEY_X) = 0x2D
  toLWJGL(GLFW.GLFW_KEY_Y) = 0x15
  toLWJGL(GLFW.GLFW_KEY_Z) = 0x2C
  toLWJGL(GLFW.GLFW_KEY_LEFT_BRACKET) = 0x1A
  toLWJGL(GLFW.GLFW_KEY_BACKSLASH) = 0x2B
  toLWJGL(GLFW.GLFW_KEY_RIGHT_BRACKET) = 0x1B
  toLWJGL(GLFW.GLFW_KEY_GRAVE_ACCENT) = 0x29
  toLWJGL(GLFW.GLFW_KEY_WORLD_1) = 0x00
  toLWJGL(GLFW.GLFW_KEY_WORLD_2) = 0x00

  /** Function keys. */
  toLWJGL(GLFW.GLFW_KEY_ESCAPE) = 0x01
  toLWJGL(GLFW.GLFW_KEY_ENTER) = 0x1C
  toLWJGL(GLFW.GLFW_KEY_TAB) = 0x0F
  toLWJGL(GLFW.GLFW_KEY_BACKSPACE) = 0x0E
  toLWJGL(GLFW.GLFW_KEY_INSERT) = 0xD2
  toLWJGL(GLFW.GLFW_KEY_DELETE) = 0xD3
  toLWJGL(GLFW.GLFW_KEY_RIGHT) = 0xCD
  toLWJGL(GLFW.GLFW_KEY_LEFT) = 0xCB
  toLWJGL(GLFW.GLFW_KEY_DOWN) = 0xD0
  toLWJGL(GLFW.GLFW_KEY_UP) = 0xC8
  toLWJGL(GLFW.GLFW_KEY_PAGE_UP) = 0xC9
  toLWJGL(GLFW.GLFW_KEY_PAGE_DOWN) = 0xD1
  toLWJGL(GLFW.GLFW_KEY_HOME) = 0xC7
  toLWJGL(GLFW.GLFW_KEY_END) = 0xCF
  toLWJGL(GLFW.GLFW_KEY_CAPS_LOCK) = 0x3A
  toLWJGL(GLFW.GLFW_KEY_SCROLL_LOCK) = 0x46
  toLWJGL(GLFW.GLFW_KEY_NUM_LOCK) = 0x45
  toLWJGL(GLFW.GLFW_KEY_PRINT_SCREEN) = 0xB7
  toLWJGL(GLFW.GLFW_KEY_PAUSE) = 0xC5
  toLWJGL(GLFW.GLFW_KEY_F1) = 0x3B
  toLWJGL(GLFW.GLFW_KEY_F2) = 0x3C
  toLWJGL(GLFW.GLFW_KEY_F3) = 0x3D
  toLWJGL(GLFW.GLFW_KEY_F4) = 0x3E
  toLWJGL(GLFW.GLFW_KEY_F5) = 0x3F
  toLWJGL(GLFW.GLFW_KEY_F6) = 0x40
  toLWJGL(GLFW.GLFW_KEY_F7) = 0x41
  toLWJGL(GLFW.GLFW_KEY_F8) = 0x42
  toLWJGL(GLFW.GLFW_KEY_F9) = 0x43
  toLWJGL(GLFW.GLFW_KEY_F10) = 0x44
  toLWJGL(GLFW.GLFW_KEY_F11) = 0x57
  toLWJGL(GLFW.GLFW_KEY_F12) = 0x58
  toLWJGL(GLFW.GLFW_KEY_F13) = 0x64
  toLWJGL(GLFW.GLFW_KEY_F14) = 0x65
  toLWJGL(GLFW.GLFW_KEY_F15) = 0x66
  toLWJGL(GLFW.GLFW_KEY_F16) = 0x67
  toLWJGL(GLFW.GLFW_KEY_F17) = 0x68
  toLWJGL(GLFW.GLFW_KEY_F18) = 0x69
  toLWJGL(GLFW.GLFW_KEY_F19) = 0x71
  toLWJGL(GLFW.GLFW_KEY_F20) = -1
  toLWJGL(GLFW.GLFW_KEY_F21) = -1
  toLWJGL(GLFW.GLFW_KEY_F22) = -1
  toLWJGL(GLFW.GLFW_KEY_F23) = -1
  toLWJGL(GLFW.GLFW_KEY_F24) = -1
  toLWJGL(GLFW.GLFW_KEY_F25) = -1
  toLWJGL(GLFW.GLFW_KEY_KP_0) = 0x52
  toLWJGL(GLFW.GLFW_KEY_KP_1) = 0x4F
  toLWJGL(GLFW.GLFW_KEY_KP_2) = 0x50
  toLWJGL(GLFW.GLFW_KEY_KP_3) = 0x51
  toLWJGL(GLFW.GLFW_KEY_KP_4) = 0x4B
  toLWJGL(GLFW.GLFW_KEY_KP_5) = 0x4C
  toLWJGL(GLFW.GLFW_KEY_KP_6) = 0x4D
  toLWJGL(GLFW.GLFW_KEY_KP_7) = 0x47
  toLWJGL(GLFW.GLFW_KEY_KP_8) = 0x48
  toLWJGL(GLFW.GLFW_KEY_KP_9) = 0x49
  toLWJGL(GLFW.GLFW_KEY_KP_DECIMAL) = 0x53
  toLWJGL(GLFW.GLFW_KEY_KP_DIVIDE) = 0xB5
  toLWJGL(GLFW.GLFW_KEY_KP_MULTIPLY) = 0x37
  toLWJGL(GLFW.GLFW_KEY_KP_SUBTRACT) = 0x4A
  toLWJGL(GLFW.GLFW_KEY_KP_ADD) = 0x4E
  toLWJGL(GLFW.GLFW_KEY_KP_ENTER) = 0x9C
  toLWJGL(GLFW.GLFW_KEY_KP_EQUAL) = 0x8D
  toLWJGL(GLFW.GLFW_KEY_LEFT_SHIFT) = 0x2A
  toLWJGL(GLFW.GLFW_KEY_LEFT_CONTROL) = 0x1D
  toLWJGL(GLFW.GLFW_KEY_LEFT_ALT) = 0x38
  toLWJGL(GLFW.GLFW_KEY_LEFT_SUPER) = 0xDB
  toLWJGL(GLFW.GLFW_KEY_RIGHT_SHIFT) = 0x36
  toLWJGL(GLFW.GLFW_KEY_RIGHT_CONTROL) = 0x9D
  toLWJGL(GLFW.GLFW_KEY_RIGHT_ALT) = 0xB8
  toLWJGL(GLFW.GLFW_KEY_RIGHT_SUPER) = 0xDC
  toLWJGL(GLFW.GLFW_KEY_MENU) = 0xDD

  def glfwToLWJGL(keyCode: Int): Int = if (keyCode >= 0 && keyCode < toLWJGL.size) toLWJGL(keyCode) else -1

  def keyToChar(keyCode: Int): Char = {
    if (keyCode == GLFW.GLFW_KEY_ENTER) '\r'
    else if (keyCode == GLFW.GLFW_KEY_KP_ENTER) '\r'
    else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) '\b'
    else '\u0000'
  }
}
