package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import li.cil.oc.common.{container, tileentity}
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector
import org.lwjgl.opengl.GL11

class Disassembler(playerInventory: InventoryPlayer, val disassembler: tileentity.Disassembler) extends DynamicGuiContainer(new container.Disassembler(playerInventory, disassembler)) {
  private def disassemblerContainer = inventorySlots.asInstanceOf[container.Disassembler]

  private val progressX = 8
  private val progressY = 65

  private val progressWidth = 160
  private val progressHeight = 12

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    fontRenderer.drawString(
      StatCollector.translateToLocal(disassembler.getInvName),
      8, 6, 0x404040)
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    super.drawGuiContainerBackgroundLayer(dt, mouseX, mouseY)
    mc.renderEngine.bindTexture(Textures.guiDisassembler)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    drawProgress()
  }

  private def drawProgress() {
    val level = disassemblerContainer.disassemblyProgress / 100.0

    val u0 = 0
    val u1 = progressWidth / 256.0 * level
    val v0 = 1 - progressHeight / 256.0
    val v1 = 1
    val x = guiLeft + progressX
    val y = guiTop + progressY
    val w = progressWidth * level

    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y, zLevel, u0, v0)
    t.addVertexWithUV(x, y + progressHeight, zLevel, u0, v1)
    t.addVertexWithUV(x + w, y + progressHeight, zLevel, u1, v1)
    t.addVertexWithUV(x + w, y, zLevel, u1, v0)
    t.draw()
  }
}
