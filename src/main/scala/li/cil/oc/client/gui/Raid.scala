package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import org.lwjgl.opengl.GL11

class Raid(playerInventory: InventoryPlayer, val raid: tileentity.Raid) extends DynamicGuiContainer(new container.Raid(playerInventory, raid)) {
  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(raid.getInventoryName),
      8, 6, 0x404040)

    fontRendererObj.drawSplitString(
      Localization.Raid.Warning,
      8, 46, 0x404040, width - 16)
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    mc.renderEngine.bindTexture(Textures.guiRaid)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }
}
