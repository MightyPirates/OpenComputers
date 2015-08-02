package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common.container
import li.cil.oc.common.inventory.ServerInventory
import net.minecraft.entity.player.InventoryPlayer
import org.lwjgl.opengl.GL11

class Server(playerInventory: InventoryPlayer, serverInventory: ServerInventory) extends DynamicGuiContainer(new container.Server(playerInventory, serverInventory)) with traits.LockedHotbar {
  override def lockedStack = serverInventory.container

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(serverInventory.getInventoryName),
      8, 6, 0x404040)
  }

  override def drawSecondaryBackgroundLayer() {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    mc.renderEngine.bindTexture(Textures.guiServer)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }
}