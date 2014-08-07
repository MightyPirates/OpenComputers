package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import li.cil.oc.common.Tier
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.inventory.{Container, Slot}
import net.minecraft.util.{IIcon, StatCollector}
import org.lwjgl.opengl.GL11

abstract class DynamicGuiContainer(container: Container) extends CustomGuiContainer(container) {
  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
    fontRendererObj.drawString(
      StatCollector.translateToLocal("container.inventory"),
      8, ySize - 96 + 2, 0x404040)
  }

  protected def drawSecondaryBackgroundLayer() {}

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    mc.renderEngine.bindTexture(Textures.guiBackground)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    drawSecondaryBackgroundLayer()

    RenderState.makeItBlend()
    GL11.glDisable(GL11.GL_LIGHTING)

    GL11.glPushMatrix()
    GL11.glTranslatef(guiLeft, guiTop, 0)
    for (slot <- 0 until inventorySlots.inventorySlots.size()) {
      drawSlotInventory(inventorySlots.inventorySlots.get(slot).asInstanceOf[Slot], mouseX, mouseY)
    }
    GL11.glPopMatrix()
  }

  def drawSlotInventory(slot: Slot, mouseX: Int, mouseY: Int) {
    slot match {
      case component: ComponentSlot if component.tier == Tier.None => // Ignore.
      case _ =>
        if (slot.slotNumber < container.inventorySlots.size() - 36) {
          drawSlotBackground(slot.xDisplayPosition - 1, slot.yDisplayPosition - 1)
        }
        if (!slot.getHasStack) slot match {
          case component: ComponentSlot if component.tierIcon != null =>
            mc.getTextureManager.bindTexture(TextureMap.locationItemsTexture)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            drawTexturedModelRectFromIcon(slot.xDisplayPosition, slot.yDisplayPosition, component.tierIcon, 16, 16)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
          case _ =>
        }
    }
  }

  private def drawSlotBackground(x: Int, y: Int) {
    GL11.glColor4f(1, 1, 1, 1)
    mc.getTextureManager.bindTexture(Textures.guiSlot)
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y + 18, zLevel + 1, 0, 1)
    t.addVertexWithUV(x + 18, y + 18, zLevel + 1, 1, 1)
    t.addVertexWithUV(x + 18, y, zLevel + 1, 1, 0)
    t.addVertexWithUV(x, y, zLevel + 1, 0, 0)
    t.draw()
  }

  protected override def drawGradientRect(par1: Int, par2: Int, par3: Int, par4: Int, par5: Int, par6: Int) {
    super.drawGradientRect(par1, par2, par3, par4, par5, par6)
    RenderState.makeItBlend()
    GL11.glDisable(GL11.GL_LIGHTING)
  }

  override def drawTexturedModelRectFromIcon(x: Int, y: Int, icon: IIcon, width: Int, height: Int) {
    GL11.glColor4f(1, 1, 1, 1)
    RenderState.makeItBlend()
    GL11.glDisable(GL11.GL_LIGHTING)
    super.drawTexturedModelRectFromIcon(x, y, icon, width, height)
  }
}
