package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import li.cil.oc.common.Tier
import li.cil.oc.common.container
import li.cil.oc.common.inventory.DatabaseInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.InventoryPlayer

class Database(playerInventory: InventoryPlayer, val databaseInventory: DatabaseInventory) extends DynamicGuiContainer(new container.Database(playerInventory, databaseInventory)) with traits.LockedHotbar {
  ySize = 256

  override def lockedStack = databaseInventory.container

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) {}

  override protected def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GlStateManager.color(1, 1, 1, 1)
    Textures.bind(Textures.GUI.Database)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)

    if (databaseInventory.tier > Tier.One) {
      Textures.bind(Textures.GUI.Database1)
      drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    }

    if (databaseInventory.tier > Tier.Two) {
      Textures.bind(Textures.GUI.Database2)
      drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    }
  }
}
