package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.IInventory

class DiskDrive(playerInventory: InventoryPlayer, val drive: IInventory) extends DynamicGuiContainer(new container.DiskDrive(playerInventory, drive)) {
  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(drive.getInventoryName),
      8, 6, 0x404040)
  }
}
