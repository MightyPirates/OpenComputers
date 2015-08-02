package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.item.TabletWrapper
import net.minecraft.entity.player.InventoryPlayer

class Tablet(playerInventory: InventoryPlayer, val tablet: TabletWrapper) extends DynamicGuiContainer(new container.Tablet(playerInventory, tablet)) with traits.LockedHotbar {
  override def lockedStack = tablet.stack

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(tablet.getInventoryName),
      8, 6, 0x404040)
  }
}