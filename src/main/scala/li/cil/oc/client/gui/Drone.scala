package li.cil.oc.client.gui

import java.util

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.entity
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import org.lwjgl.opengl.GL11

class Drone(playerInventory: InventoryPlayer, val drone: entity.Drone) extends DynamicGuiContainer(new container.Drone(playerInventory, drone)) {
  xSize = 176
  ySize = 148

  protected var powerButton: ImageButton = _

  private val bufferWidth = 80
  private val bufferHeight = 32
  private val bufferX = 10
  private val bufferY = 10

  private val inventoryX = 97
  private val inventoryY = 7

  private val power = addWidget(new ProgressBar(28, 48))

  private val selectionSize = 20
  private val selectionsStates = 17
  private val selectionStepV = 1 / selectionsStates.toDouble

  def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  protected override def actionPerformed(button: GuiButton) {
    if (button.id == 0) {
      ClientPacketSender.sendDronePower(drone, !drone.isRunning)
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    powerButton.toggled = drone.isRunning
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    powerButton = new ImageButton(0, guiLeft + 7, guiTop + 45, 18, 18, Textures.guiButtonPower, canToggle = true)
    add(buttonList, powerButton)
  }

  override protected def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS) // Me lazy... prevents NEI render glitch.
    if (func_146978_c(power.x, power.y, power.width, power.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      val format = Localization.Computer.Power + ": %d%% (%d/%d)"
//      tooltip.add(format.format(
//        ((drone.globalBuffer / drone.globalBufferSize) * 100).toInt,
//        drone.globalBuffer.toInt,
//        drone.globalBufferSize.toInt))
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }
    if (powerButton.func_146115_a) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(if (drone.isRunning) Localization.Computer.TurnOff else Localization.Computer.TurnOn)
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }
    GL11.glPopAttrib()
  }

  override protected def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    mc.renderEngine.bindTexture(Textures.guiDrone)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
//    power.level = robot.globalBuffer / robot.globalBufferSize
    power.level = 0.5
    drawWidgets()
    if (drone.inventory.getSizeInventory > 0) {
      drawSelection()
    }

    GL11.glPushMatrix()
    GL11.glTranslatef(guiLeft, guiTop, 0)
    for (slot <- 0 until inventorySlots.inventorySlots.size()) {
      drawSlotInventory(inventorySlots.inventorySlots.get(slot).asInstanceOf[Slot])
    }
    GL11.glPopMatrix()

    RenderState.makeItBlend()
  }

  protected override def drawGradientRect(par1: Int, par2: Int, par3: Int, par4: Int, par5: Int, par6: Int) {
    super.drawGradientRect(par1, par2, par3, par4, par5, par6)
    RenderState.makeItBlend()
  }

  // No custom slots, we just extend DynamicGuiContainer for the highlighting.
  override protected def drawSlotBackground(x: Int, y: Int) {}

  private def drawSelection() {
    val slot = drone.selectedSlot
    if (slot >= 0 && slot < 16) {
      RenderState.makeItBlend()
      Minecraft.getMinecraft.renderEngine.bindTexture(Textures.guiRobotSelection)
      val now = System.currentTimeMillis() / 1000.0
      val offsetV = ((now - now.toInt) * selectionsStates).toInt * selectionStepV
      val x = guiLeft + inventoryX - 1 + (slot % 4) * (selectionSize - 2)
      val y = guiTop + inventoryY - 1 + (slot / 4) * (selectionSize - 2)

      val t = Tessellator.instance
      t.startDrawingQuads()
      t.addVertexWithUV(x, y, zLevel, 0, offsetV)
      t.addVertexWithUV(x, y + selectionSize, zLevel, 0, offsetV + selectionStepV)
      t.addVertexWithUV(x + selectionSize, y + selectionSize, zLevel, 1, offsetV + selectionStepV)
      t.addVertexWithUV(x + selectionSize, y, zLevel, 1, offsetV)
      t.draw()
    }
  }
}