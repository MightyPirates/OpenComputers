package li.cil.oc.client.gui

import li.cil.oc.Config
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

class Robot(playerInventory: InventoryPlayer, val robot: tileentity.Robot) extends GuiContainer(new container.Robot(playerInventory, robot)) with Buffer {
  xSize = 256
  ySize = 242

  protected val background = new ResourceLocation(Config.resourceDomain, "textures/gui/robot.png")

  protected val buffer = robot.buffer

  private val bufferWidth = 242.0
  private val bufferHeight = 128.0
  private val bufferMargin = BufferRenderer.innerMargin

  override def drawSlotInventory(slot: Slot) {
    RenderState.makeItBlend()
    super.drawSlotInventory(slot)
    GL11.glDisable(GL11.GL_BLEND)
  }

  def drawBuffer() {
    GL11.glTranslatef(guiLeft + 8, guiTop + 8, 0)
    RenderState.disableLighting()
    RenderState.makeItBlend()
    BufferRenderer.drawText()
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    mc.renderEngine.bindTexture(background)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }

  protected override def keyTyped(char: Char, code: Int) {
    if (code == Keyboard.KEY_ESCAPE) {
      super.keyTyped(char, code)
    }
  }

  protected def changeSize(w: Double, h: Double) = {
    val bw = w * MonospaceFontRenderer.fontWidth
    val bh = h * MonospaceFontRenderer.fontHeight
    val scaleX = (bufferWidth / (bw + bufferMargin * 2.0)) min 1
    val scaleY = (bufferHeight / (bh + bufferMargin * 2.0)) min 1
    scaleX min scaleY
  }
}