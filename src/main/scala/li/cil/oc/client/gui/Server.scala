package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import li.cil.oc.common.container
import li.cil.oc.common.inventory.ServerInventory
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.util.StatCollector
import org.lwjgl.opengl.GL11

class Server(playerInventory: InventoryPlayer, serverInventory: ServerInventory) extends DynamicGuiContainer(new container.Server(playerInventory, serverInventory)) {

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      StatCollector.translateToLocal(serverInventory.getInventoryName),
      8, 6, 0x404040)
  }

  override def drawSecondaryBackgroundLayer() {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    mc.renderEngine.bindTexture(Textures.guiServer)
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