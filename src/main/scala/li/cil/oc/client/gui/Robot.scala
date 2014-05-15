package li.cil.oc.client.gui

import java.util
import li.cil.oc.api
import li.cil.oc.Settings
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.client.{PacketSender => ClientPacketSender, Textures}
import li.cil.oc.common.container
import li.cil.oc.common.container.StaticComponentSlot
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.util.StatCollector
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

class Robot(playerInventory: InventoryPlayer, val robot: tileentity.Robot) extends GuiContainer(new container.Robot(playerInventory, robot)) with TextBuffer {
  xSize = 256
  ySize = 242
  val slots = inventorySlots.asInstanceOf[container.Robot]

  protected var powerButton: ImageButton = _

  protected def buffer = {
    robot.components.collect {
      case Some(component: api.component.TextBuffer) => component
    }.headOption.orNull
  }

  private val bufferWidth = 242.0
  private val bufferHeight = 128.0
  private val bufferMargin = BufferRenderer.innerMargin

  private val inventoryX = 168
  private val inventoryY = 140

  private val powerX = 26
  private val powerY = 142

  private val powerWidth = 140
  private val powerHeight = 12

  private val selectionSize = 20
  private val selectionsStates = 17
  private val selectionStepV = 1 / selectionsStates.toDouble

  def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  protected override def actionPerformed(button: GuiButton) {
    if (button.id == 0) {
      ClientPacketSender.sendComputerPower(robot, !robot.isRunning)
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    powerButton.toggled = robot.isRunning
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    powerButton = new ImageButton(0, guiLeft + 5, guiTop + 139, 18, 18, Textures.guiButtonPower, canToggle = true)
    add(buttonList, powerButton)
  }

  override def drawSlotInventory(slot: Slot) {
    RenderState.makeItBlend()
    super.drawSlotInventory(slot)
    GL11.glDisable(GL11.GL_BLEND)
    if (!slot.getHasStack) slot match {
      case component: StaticComponentSlot if component.tierIcon != null =>
        mc.getTextureManager.bindTexture(TextureMap.locationItemsTexture)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        drawTexturedModelRectFromIcon(slot.xDisplayPosition, slot.yDisplayPosition, component.tierIcon, 16, 16)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
      case _ =>
    }
  }

  override def drawBuffer() {
    if (buffer != null) {
      GL11.glTranslatef(8, 8, 0)
      RenderState.disableLighting()
      RenderState.makeItBlend()
      val scaleX = 48f / buffer.getWidth
      val scaleY = 14f / buffer.getHeight
      val scale = math.min(scaleX, scaleY)
      if (scaleX > scale) {
        GL11.glTranslated(buffer.renderWidth * (scaleX - scale) / 2, 0, 0)
      }
      else if (scaleY > scale) {
        GL11.glTranslated(0, buffer.renderHeight * (scaleY - scale) / 2, 0)
      }
      GL11.glScalef(scale, scale, scale)
      GL11.glScaled(this.scale, this.scale, 1)
      BufferRenderer.drawText(buffer)
    }
  }

  protected override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
    drawBufferLayer()
    GL11.glPushAttrib(0xFFFFFFFF) // Me lazy... prevents NEI render glitch.
    if (isPointInRegion(powerX, powerY, powerWidth, powerHeight, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      val format = StatCollector.translateToLocal(Settings.namespace + "gui.Robot.Power") + ": %d%% (%d/%d)"
      tooltip.add(format.format(
        ((robot.globalBuffer / robot.globalBufferSize) * 100).toInt,
        robot.globalBuffer.toInt,
        robot.globalBufferSize.toInt))
      drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }
    if (powerButton.func_82252_a) {
      val tooltip = new java.util.ArrayList[String]
      val which = if (robot.isRunning) "gui.Robot.TurnOff" else "gui.Robot.TurnOn"
      tooltip.add(StatCollector.translateToLocal(Settings.namespace + which))
      drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }
    GL11.glPopAttrib()
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    mc.renderEngine.bindTexture(Textures.guiRobot)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    drawPowerLevel()
    drawSelection()
  }

  protected override def keyTyped(char: Char, code: Int) {
    if (code == Keyboard.KEY_ESCAPE) {
      super.keyTyped(char, code)
    }
  }

  override protected def changeSize(w: Double, h: Double) = {
    val bw = w * MonospaceFontRenderer.fontWidth
    val bh = h * MonospaceFontRenderer.fontHeight
    val scaleX = math.min(bufferWidth / (bw + bufferMargin * 2.0), 1)
    val scaleY = math.min(bufferHeight / (bh + bufferMargin * 2.0), 1)
    math.min(scaleX, scaleY)
  }

  private def drawSelection() {
    RenderState.makeItBlend()
    Minecraft.getMinecraft.renderEngine.bindTexture(Textures.guiRobotSelection)
    val now = System.currentTimeMillis() / 1000.0
    val offsetV = ((now - now.toInt) * selectionsStates).toInt * selectionStepV
    val slot = robot.selectedSlot - robot.actualSlot(0)
    val x = guiLeft + inventoryX + (slot % 4) * (selectionSize - 2)
    val y = guiTop + inventoryY + (slot / 4) * (selectionSize - 2)

    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y, zLevel, 0, offsetV)
    t.addVertexWithUV(x, y + selectionSize, zLevel, 0, offsetV + selectionStepV)
    t.addVertexWithUV(x + selectionSize, y + selectionSize, zLevel, 1, offsetV + selectionStepV)
    t.addVertexWithUV(x + selectionSize, y, zLevel, 1, offsetV)
    t.draw()
  }

  private def drawPowerLevel() {
    val level = robot.globalBuffer / robot.globalBufferSize

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