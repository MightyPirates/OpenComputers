package li.cil.oc.client.gui

import li.cil.oc.Settings
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.util.{StatCollector, ResourceLocation}
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

class Robot(playerInventory: InventoryPlayer, val robot: tileentity.Robot) extends GuiContainer(new container.Robot(playerInventory, robot)) with Buffer {
  xSize = 256
  ySize = 242

  private val background = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot.png")
  private val selection = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot_selection.png")

  protected val buffer = robot.buffer

  private val bufferWidth = 242.0
  private val bufferHeight = 128.0
  private val bufferMargin = BufferRenderer.innerMargin

  private val inventoryX = 176
  private val inventoryY = 140

  private val powerX = 8
  private val powerY = 142

  private val powerWidth = 160
  private val powerHeight = 12

  private val selectionSize = 20
  private val selectionsStates = 17
  private val selectionStepV = 1 / selectionsStates.toDouble

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

  protected override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
    if (isPointInRegion(powerX, powerY, powerWidth, powerHeight, mouseX, mouseY)) {
      GL11.glPushAttrib(0xFFFFFFFF) // Me lazy... prevents NEI render glitch.
      val tooltip = new java.util.ArrayList[String]
      val format = StatCollector.translateToLocal(Settings.namespace + "text.Robot.Power") + ": %d%% (%d/%d)"
      tooltip.add(format.format(
        (robot.globalPower * 100).toInt,
        (robot.globalPower * Settings.get.bufferRobot).toInt,
        Settings.get.bufferRobot.toInt))
      drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
      GL11.glPopAttrib()
    }
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    mc.renderEngine.bindTexture(background)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    drawPowerLevel()
    drawSelection()
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

  private def drawSelection() {
    RenderState.makeItBlend()
    Minecraft.getMinecraft.renderEngine.bindTexture(selection)
    val now = System.currentTimeMillis() / 1000.0
    val offsetV = ((now - now.toInt) * selectionsStates).toInt * selectionStepV
    val x = guiLeft + inventoryX + (robot.selectedSlot % 4) * (selectionSize - 2)
    val y = guiTop + inventoryY + (robot.selectedSlot / 4) * (selectionSize - 2)

    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y, zLevel, 0, offsetV)
    t.addVertexWithUV(x, y + selectionSize, zLevel, 0, offsetV + selectionStepV)
    t.addVertexWithUV(x + selectionSize, y + selectionSize, zLevel, 1, offsetV + selectionStepV)
    t.addVertexWithUV(x + selectionSize, y, zLevel, 1, offsetV)
    t.draw()
  }

  private def drawPowerLevel() {
    val level = robot.globalPower

    val u0 = 0
    val u1 = powerWidth / 256.0 * level
    val v0 = 1 - powerHeight / 256.0
    val v1 = 1
    val x = guiLeft + powerX
    val y = guiTop + powerY
    val w = powerWidth * level

    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y, zLevel, u0, v0)
    t.addVertexWithUV(x, y + powerHeight, zLevel, u0, v1)
    t.addVertexWithUV(x + w, y + powerHeight, zLevel, u1, v1)
    t.addVertexWithUV(x + w, y, zLevel, u1, v0)
    t.draw()
  }
}