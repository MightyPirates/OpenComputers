package li.cil.oc.client.gui

import li.cil.oc.common.{container, tileentity}
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector

class Router(playerInventory: InventoryPlayer, val router: tileentity.Router) extends DynamicGuiContainer(new container.Router(playerInventory, router)) {
  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      StatCollector.translateToLocal(router.getInvName),
      8, 6, 0x404040)
  }
}
