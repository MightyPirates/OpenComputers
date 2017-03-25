package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class GuiCharger(playerInventory: InventoryPlayer, val charger: tileentity.TileEntityCharger) extends DynamicGuiContainer(new container.ContainerCharger(playerInventory, charger)) {
  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      Localization.localizeImmediately(charger.getName),
      8, 6, 0x404040)
  }
}
