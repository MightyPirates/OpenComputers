package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.InventoryPlayer

class GuiDisassembler(playerInventory: InventoryPlayer, val disassembler: tileentity.Disassembler) extends DynamicGuiContainer(new container.ContainerDisassembler(playerInventory, disassembler)) {
  val progress = addWidget(new ProgressBar(18, 65))

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    fontRenderer.drawString(
      Localization.localizeImmediately(disassembler.getName),
      8, 6, 0x404040)
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GlStateManager.color(1, 1, 1)
    Textures.bind(Textures.GUI.Disassembler)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    progress.level = inventoryContainer.disassemblyProgress / 100.0
    drawWidgets()
  }
}
