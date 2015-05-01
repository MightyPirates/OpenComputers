package li.cil.oc.client.gui

import li.cil.oc.client.PacketSender
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

class Waypoint(val waypoint: tileentity.Waypoint) extends GuiScreen {
  var guiLeft = 0
  var guiTop = 0
  var xSize = 0
  var ySize = 0

  var textField: GuiTextField = _

  override def updateScreen(): Unit = {
    super.updateScreen()
    if (mc.thePlayer.getDistanceSq(waypoint.x + 0.5, waypoint.y + 0.5, waypoint.z + 0.5) > 64) {
      mc.thePlayer.closeScreen()
    }
  }

  override def doesGuiPauseGame(): Boolean = false

  override def initGui(): Unit = {
    super.initGui()

    val screenSize = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight)
    val guiSize = new ScaledResolution(mc, 176, 24)
    val (midX, midY) = (screenSize.getScaledWidth / 2, screenSize.getScaledHeight / 2)
    guiLeft = midX - guiSize.getScaledWidth / 2
    guiTop = midY - guiSize.getScaledHeight / 2
    xSize = guiSize.getScaledWidth
    ySize = guiSize.getScaledHeight

    textField = new GuiTextField(fontRendererObj, guiLeft + 7, guiTop + 8, 164 - 12, 12)
    textField.setMaxStringLength(32)
    textField.setEnableBackgroundDrawing(false)
    textField.setCanLoseFocus(false)
    textField.setFocused(true)
    textField.setTextColor(0xFFFFFF)
    textField.setText(waypoint.label)

    Keyboard.enableRepeatEvents(true)
  }

  override def onGuiClosed(): Unit = {
    super.onGuiClosed()
    Keyboard.enableRepeatEvents(false)
  }

  override def keyTyped(char: Char, code: Int): Unit = {
    if (!textField.textboxKeyTyped(char, code)) {
      if (code == Keyboard.KEY_RETURN) {
        val label = textField.getText.take(32)
        if (label != waypoint.label) {
          waypoint.label = label
          PacketSender.sendWaypointLabel(waypoint)
          mc.thePlayer.closeScreen()
        }
      }
      else super.keyTyped(char, code)
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.drawScreen(mouseX, mouseY, dt)
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    mc.renderEngine.bindTexture(Textures.guiWaypoint)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    textField.drawTextBox()
  }
}
