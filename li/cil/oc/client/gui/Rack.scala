package li.cil.oc.client.gui

import li.cil.oc.Settings
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector

class Rack(playerInventory: InventoryPlayer, val rack: tileentity.Rack) extends DynamicGuiContainer(new container.Rack(playerInventory, rack)) {
  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      StatCollector.translateToLocal(Settings.namespace + "container.ServerRack"),
      8, 6, 0x404040)
  }
}
