package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

class GuiAdapter(playerInventory: InventoryPlayer, val adapter: tileentity.TileEntityAdapter) extends DynamicGuiContainer(new container.ContainerAdapter(playerInventory, adapter)) {
  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      Localization.localizeImmediately(adapter.getName),
      8, 6, 0x404040)
  }
}
