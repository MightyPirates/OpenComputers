package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.{OpenGlHelper, RenderHelper, Tessellator}
import net.minecraft.inventory.{Slot, Container}
import net.minecraft.util.{IIcon, EnumChatFormatting, MathHelper, StatCollector}
import org.lwjgl.opengl.{GL12, GL11}
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.ItemStack
import net.minecraft.client.Minecraft
import li.cil.oc.util.RenderState
import li.cil.oc.common.container.ComponentSlot
import net.minecraft.client.renderer.texture.TextureMap

abstract class DynamicGuiContainer(container: Container) extends GuiContainer(container) {
  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {

    fontRendererObj.drawString(
      StatCollector.translateToLocal("container.inventory"),
      8, ySize - 96 + 2, 0x404040)
    for (i1 <- 0 until inventorySlots.inventorySlots.size()) {
      val slot: Slot = inventorySlots.inventorySlots.get(i1).asInstanceOf[Slot]
      this.drawSlotInventory(slot, mouseX, mouseY)

    }


  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    mc.renderEngine.bindTexture(Textures.guiBackground)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }

  // TODO private now?
  def drawSlotInventory(slot: Slot, mouseX: Int, mouseY: Int) {
    if (slot.slotNumber < container.inventorySlots.size() - 36) {
      GL11.glDisable(GL11.GL_LIGHTING)
      drawSlotBackground(slot.xDisplayPosition - 1, slot.yDisplayPosition - 1)
      GL11.glEnable(GL11.GL_LIGHTING)
    }
    RenderState.makeItBlend()


    GL11.glDisable(GL11.GL_BLEND)
    if (!slot.getHasStack) slot match {
      case component: ComponentSlot if component.tierIcon != null =>
        mc.getTextureManager.bindTexture(TextureMap.locationItemsTexture)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        drawTexturedModelRectFromIcon(slot.xDisplayPosition, slot.yDisplayPosition, component.tierIcon, 16, 16)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
      case something =>
    }
  }


  private def drawSlotBackground(x: Int, y: Int) {
    GL11.glColor4f(1, 1, 1, 1)
    mc.renderEngine.bindTexture(Textures.guiSlot)
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y + 18, zLevel, 0, 1)
    t.addVertexWithUV(x + 18, y + 18, zLevel, 1, 1)
    t.addVertexWithUV(x + 18, y, zLevel, 1, 0)
    t.addVertexWithUV(x, y, zLevel, 0, 0)
    t.draw()
  }


}
