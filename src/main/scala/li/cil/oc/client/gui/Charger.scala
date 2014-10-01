package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.{container, tileentity}
import net.minecraft.entity.player.InventoryPlayer

class Charger(playerInventory: InventoryPlayer, val charger: tileentity.Charger) extends DynamicGuiContainer(new container.Charger(playerInventory, charger)) {
  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      Localization.localizeImmediately(charger.getInvName),
      8, 6, 0x404040)
  }
}
