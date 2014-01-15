package li.cil.oc.client.gui

import java.util
import li.cil.oc.Settings
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.{ResourceLocation, StatCollector}
import org.lwjgl.opengl.GL11

class Rack(playerInventory: InventoryPlayer, val rack: tileentity.Rack) extends DynamicGuiContainer(new container.Rack(playerInventory, rack)) {
  protected val powerIcon = new ResourceLocation(Settings.resourceDomain, "textures/gui/power.png")

  protected var powerButtons = new Array[ImageButton](4)

  def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  protected override def actionPerformed(button: GuiButton) {
    if (button.id >= 0 && button.id <= 3) {
      ClientPacketSender.sendServerPower(rack, button.id, !rack.isRunning(button.id))
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    for (i <- 0 to 3) {
      powerButtons(i).toggled = rack.isRunning(i)
    }
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    for (i <- 0 to 3) {
      powerButtons(i) = new ImageButton(i, guiLeft + 94, guiTop + 7 + i * 18, 18, 18, powerIcon)
      add(buttonList, powerButtons(i))
    }
  }

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    GL11.glPushAttrib(0xFFFFFFFF) // Prevents NEI render glitch.
    fontRenderer.drawString(
      StatCollector.translateToLocal(rack.getInvName),
      8, 6, 0x404040)
    for (i <- 0 to 3 if powerButtons(i).func_82252_a) {
      val tooltip = new java.util.ArrayList[String]
      val which = if (rack.isRunning(i)) "gui.Robot.TurnOff" else "gui.Robot.TurnOn"
      tooltip.add(StatCollector.translateToLocal(Settings.namespace + which))
      drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }
    GL11.glPopAttrib()
  }
}
