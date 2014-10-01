package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.{container, tileentity}
import net.minecraft.entity.player.InventoryPlayer

class DiskDrive(playerInventory: InventoryPlayer, val drive: tileentity.DiskDrive) extends DynamicGuiContainer(new container.DiskDrive(playerInventory, drive)) {
  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      Localization.localizeImmediately(drive.getInvName),
      8, 6, 0x404040)
  }
}
