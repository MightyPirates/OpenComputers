package li.cil.oc.client.gui

import java.util

import li.cil.oc.Localization
import li.cil.oc.client.{Textures, PacketSender => ClientPacketSender}
import li.cil.oc.common.{container, tileentity}
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector
import org.lwjgl.opengl.GL11

class Case(playerInventory: InventoryPlayer, val computer: tileentity.Case) extends DynamicGuiContainer(new container.Case(playerInventory, computer)) {
  protected var powerButton: ImageButton = _

  def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  protected override def actionPerformed(button: GuiButton) {
    if (button.id == 0) {
      ClientPacketSender.sendComputerPower(computer, !computer.isRunning)
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    powerButton.toggled = computer.isRunning
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    powerButton = new ImageButton(0, guiLeft + 70, guiTop + 33, 18, 18, Textures.guiButtonPower, canToggle = true)
    add(buttonList, powerButton)
  }

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS) // Me lazy... prevents NEI render glitch.
    fontRenderer.drawString(
      StatCollector.translateToLocal(computer.getInvName),
      8, 6, 0x404040)
    if (powerButton.func_82252_a) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(if (computer.isRunning) Localization.Robot.TurnOff else Localization.Robot.TurnOn)
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }
    GL11.glPopAttrib()
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    super.drawGuiContainerBackgroundLayer(dt, mouseX, mouseY)
    mc.renderEngine.bindTexture(Textures.guiComputer)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }

  override def doesGuiPauseGame = false
}