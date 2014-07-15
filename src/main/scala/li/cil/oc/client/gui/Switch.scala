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
  }
}
