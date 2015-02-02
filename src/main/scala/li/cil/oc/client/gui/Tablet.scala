package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.item.TabletWrapper
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot

class Tablet(playerInventory: InventoryPlayer, val tablet: TabletWrapper) extends DynamicGuiContainer(new container.Tablet(playerInventory, tablet)) {
  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(tablet.getInventoryName),
      8, 6, 0x404040)
  }

  override def doesGuiPauseGame = false

  protected override def handleMouseClick(slot: Slot, slotNumber: Int, button: Int, shift: Int) {
    if (slot == null || slot.getStack != tablet.stack) {
      super.handleMouseClick(slot, slotNumber, button, shift)
    }
  }

  protected override def checkHotbarKeys(slot: Int) = false
}
