package li.cil.oc.client.gui

import li.cil.oc.Config
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector

class Case(playerInventory: InventoryPlayer, val computer: tileentity.Case) extends DynamicGuiContainer(new container.Case(playerInventory, computer)) {
  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      StatCollector.translateToLocal(Config.namespace + "container.Case"),
      8, 6, 0x404040)
  }

  override def doesGuiPauseGame = false
}