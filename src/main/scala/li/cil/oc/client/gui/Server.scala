package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common.container
import li.cil.oc.common.inventory.ServerInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot

class Server(playerInventory: InventoryPlayer, serverInventory: ServerInventory) extends DynamicGuiContainer(new container.Server(playerInventory, serverInventory)) {
  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(serverInventory.getName),
      8, 6, 0x404040)
  }

  override def drawSecondaryBackgroundLayer() {
    GlStateManager.color(1, 1, 1)
    Textures.bind(Textures.GUI.Server)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }

  override def doesGuiPauseGame = false

  protected override def handleMouseClick(slot: Slot, slotNumber: Int, button: Int, shift: Int) {
    if (slot == null || slot.getStack != serverInventory.container) {
      super.handleMouseClick(slot, slotNumber, button, shift)
    }
  }

  protected override def checkHotbarKeys(slot: Int) = false
}