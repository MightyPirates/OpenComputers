package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.InventoryPlayer

class Raid(playerInventory: InventoryPlayer, val raid: tileentity.Raid) extends DynamicGuiContainer(new container.Raid(playerInventory, raid)) {
  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      Localization.localizeImmediately(raid.getName),
      8, 6, 0x404040)

    fontRenderer.drawSplitString(
      Localization.Raid.Warning,
      8, 46, 0x404040, width - 16)
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GlStateManager.color(1, 1, 1) // Required under Linux.
    Textures.bind(Textures.GUI.Raid)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }
}
