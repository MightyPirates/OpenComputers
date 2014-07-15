package li.cil.oc.client.gui

import li.cil.oc.common.{container, tileentity}
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector

class Switch(playerInventory: InventoryPlayer, val switch: tileentity.Switch) extends DynamicGuiContainer(new container.Switch(playerInventory, switch)) {
  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      StatCollector.translateToLocal(switch.getInvName),
      8, 6, 0x404040)

    fontRenderer.drawString(
      StatCollector.translateToLocal("Transfer rate"),
      14, 20, 0x404040)
    fontRenderer.drawString(
      StatCollector.translateToLocal("Packets / cycle"),
      14, 39, 0x404040)
    fontRenderer.drawString(
      StatCollector.translateToLocal("Queue size"),
      14, 58, 0x404040)

    fontRenderer.drawString(
      StatCollector.translateToLocal("4hz"),
      108, 20, 0x404040)
    fontRenderer.drawString(
      StatCollector.translateToLocal("0 / 1"),
      108, 39, 0x404040)
    fontRenderer.drawString(
      StatCollector.translateToLocal("0 / 20"),
      108, 58, 0x404040)
  }
}
