package li.cil.oc.client.gui

import li.cil.oc.client.PacketSender
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.common.component
import li.cil.oc.util.RenderState
import li.cil.oc.util.mods.NEI
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import scala.collection.mutable

trait Buffer extends GuiScreen {
  protected def buffer: component.Buffer

  private val pressedKeys = mutable.Map.empty[Int, Char]

  protected var currentWidth, currentHeight = -1

  private var shouldRecompileDisplayLists = true

  protected var scale = 0.0

  def recompileDisplayLists() {
    shouldRecompileDisplayLists = true
  }

  override def doesGuiPauseGame = false

  override def initGui() = {
    super.initGui()
    MonospaceFontRenderer.init(Minecraft.getMinecraft.renderEngine)
    BufferRenderer.init(Minecraft.getMinecraft.renderEngine)
    Keyboard.enableRepeatEvents(true)
    buffer.owner.currentGui = Some(this)
    recompileDisplayLists()
  }

  override def onGuiClosed() = {
    super.onGuiClosed()
    buffer.owner.currentGui = None
    for ((code, char) <- pressedKeys) {
      PacketSender.sendKeyUp(buffer.owner, char, code)
    }
    Keyboard.enableRepeatEvents(false)
  }

  protected def drawBufferLayer() {
    if (shouldRecompileDisplayLists) {
      shouldRecompileDisplayLists = false
      val (w, h) = buffer.resolution
      currentWidth = w
      currentHeight = h
      scale = changeSize(currentWidth, currentHeight)
      BufferRenderer.compileText(scale, buffer.lines, buffer.color, buffer.depth)
    }
    GL11.glPushMatrix()
    RenderState.disableLighting()
    drawBuffer()
    GL11.glPopMatrix()
  }

  protected def drawBuffer()

  override def handleKeyboardInput() {
    super.handleKeyboardInput()

    if (NEI.isInputFocused) return

    val code = Keyboard.getEventKey
    if (code != Keyboard.KEY_ESCAPE && code != Keyboard.KEY_F11)
      if (code == Keyboard.KEY_INSERT && GuiScreen.isShiftKeyDown) {
        if (Keyboard.getEventKeyState)
          PacketSender.sendClipboard(buffer.owner, GuiScreen.getClipboardString)
      }
      else if (Keyboard.getEventKeyState) {
        val char = Keyboard.getEventCharacter
        PacketSender.sendKeyDown(buffer.owner, char, code)
        pressedKeys += code -> char
      }
      else pressedKeys.remove(code) match {
        case Some(char) => PacketSender.sendKeyUp(buffer.owner, char, code)
        case _ => // Wasn't pressed while viewing the screen.
      }
  }

  override protected def mouseClicked(x: Int, y: Int, button: Int) {
    super.mouseClicked(x, y, button)
    if (button == 2) {
      PacketSender.sendClipboard(buffer.owner, GuiScreen.getClipboardString)
    }
  }

  protected def changeSize(w: Double, h: Double): Double
}
