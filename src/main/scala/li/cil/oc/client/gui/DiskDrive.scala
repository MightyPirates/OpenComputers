package li.cil.oc.client.gui

import li.cil.oc.common.{container, tileentity}
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector

class DiskDrive(playerInventory: InventoryPlayer, val drive: tileentity.DiskDrive) extends DynamicGuiContainer(new container.DiskDrive(playerInventory, drive)) {
  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      StatCollector.translateToLocal(drive.getInventoryName),
      8, 6, 0x404040)
  }
}
