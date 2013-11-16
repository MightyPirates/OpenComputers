package li.cil.oc.client.gui

import li.cil.oc.Config
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

class Robot(playerInventory: InventoryPlayer, val robot: tileentity.Robot) extends GuiContainer(new container.Robot(playerInventory, robot)) {
  protected val background = new ResourceLocation(Config.resourceDomain, "textures/gui/robot.png")

  xSize = 236
  ySize = 222

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    mc.renderEngine.bindTexture(background)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }

  override def drawSlotInventory(slot: Slot) {
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    super.drawSlotInventory(slot)
    GL11.glDisable(GL11.GL_BLEND)
  }
}