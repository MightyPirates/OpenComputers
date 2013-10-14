package li.cil.oc.client.gui

import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector

class Computer(playerInventory: InventoryPlayer, val computer: tileentity.Computer) extends DynamicGuiContainer(new container.Computer(playerInventory, computer)) {
  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    fontRenderer.drawString(
      StatCollector.translateToLocal("oc.container.computer"),
      8, 6, 0x404040)
  }

  override def doesGuiPauseGame = false
}