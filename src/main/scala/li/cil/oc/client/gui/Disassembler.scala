package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.common.{container, tileentity}
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector
import org.lwjgl.opengl.GL11

class Disassembler(playerInventory: InventoryPlayer, val disassembler: tileentity.Disassembler) extends DynamicGuiContainer(new container.Disassembler(playerInventory, disassembler)) {
  private def disassemblerContainer = inventorySlots.asInstanceOf[container.Disassembler]

  val progress = addWidget(new ProgressBar(18, 65))

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    fontRendererObj.drawString(
      StatCollector.translateToLocal(disassembler.getInventoryName),
      8, 6, 0x404040)
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    super.drawGuiContainerBackgroundLayer(dt, mouseX, mouseY)
    mc.renderEngine.bindTexture(Textures.guiDisassembler)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    progress.level = disassemblerContainer.disassemblyProgress / 100.0
    drawWidgets()
  }
}
