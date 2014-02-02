package li.cil.oc.client.gui

import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector

class Adapter(playerInventory: InventoryPlayer, val adapter: tileentity.Adapter) extends DynamicGuiContainer(new container.Adapter(playerInventory, adapter)) {
  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      StatCollector.translateToLocal(adapter.getInvName),
      8, 6, 0x404040)
  }
}
