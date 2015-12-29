package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common.container
import li.cil.oc.common.inventory.ServerInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.InventoryPlayer

class Server(playerInventory: InventoryPlayer, serverInventory: ServerInventory) extends DynamicGuiContainer(new container.Server(playerInventory, serverInventory)) with traits.LockedHotbar {
  override def lockedStack = serverInventory.container

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
}