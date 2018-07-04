package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class DiskDrive(playerInventory: InventoryPlayer, val drive: tileentity.DiskDrive) extends DynamicGuiContainer(new container.DiskDrive(playerInventory, drive)) {
  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      Localization.localizeImmediately(drive.getName),
      8, 6, 0x404040)
  }
}
