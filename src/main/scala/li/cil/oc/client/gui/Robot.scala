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
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.util.StatCollector
import org.lwjgl.input.{Mouse, Keyboard}
import org.lwjgl.opengl.GL11

class Robot(playerInventory: InventoryPlayer, val robot: tileentity.Robot) extends CustomGuiContainer(new container.Robot(playerInventory, robot)) with TextBuffer {
  xSize = 256
  ySize = 242

  protected var powerButton: ImageButton = _

  protected var scrollButton: ImageButton = _

  protected def buffer = {
    robot.components.collect {
      case Some(buffer: api.component.TextBuffer) => buffer
    }.headOption.orNull
  }

  // Scroll offset for robot inventory.
  private var inventoryOffset = 0
  private var isDragging = false

  private def canScroll = robot.inventorySize > 16
  private def maxOffset = robot.inventorySize / 4 - 4

  private val slotSize = 18

  private val bufferWidth = 242.0
  private val bufferHeight = 128.0
  private val bufferMargin = BufferRenderer.innerMargin

  private val inventoryX = 169
  private val inventoryY = 141

  private val scrollX = inventoryX + slotSize * 4 + 2
  private val scrollY = inventoryY
  private val scrollWidth = 8
  private val scrollHeight = 94

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
    scrollButton.enabled = canScroll
    scrollButton.hoverOverride = isDragging
    if (robot.inventorySize < 16 + inventoryOffset * 4) {
      scrollTo(0)
    }
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    powerButton = new ImageButton(0, guiLeft + 5, guiTop + 139, 18, 18, Textures.guiButtonPower, canToggle = true)
    scrollButton = new ImageButton(1, guiLeft + scrollX + 1, guiTop + scrollY + 1, 6, 13, Textures.guiButtonScroll)
    add(buttonList, powerButton)
    add(buttonList, scrollButton)
  }

  override def drawSlotInventory(slot: Slot) {
    RenderState.makeItBlend()
    super.drawSlotInventory(slot)
    GL11.glDisable(GL11.GL_BLEND)
    if (!slot.getHasStack) slot match {
      case component: StaticComponentSlot if component.tierIcon != null =>
        mc.getTextureManager.bindTexture(TextureMap.locationItemsTexture)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LIGHTING)
        drawTexturedModelRectFromIcon(slot.xDisplayPosition, slot.yDisplayPosition, component.tierIcon, 16, 16)
        GL11.glEnable(GL11.GL_LIGHTING)
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
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }
    if (powerButton.func_82252_a) {
      val tooltip = new java.util.ArrayList[String]
      val which = if (robot.isRunning) "gui.Robot.TurnOff" else "gui.Robot.TurnOn"
      tooltip.add(StatCollector.translateToLocal(Settings.namespace + which))
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }
    GL11.glPopAttrib()
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    mc.renderEngine.bindTexture(Textures.guiRobot)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    drawPowerLevel()
    if (robot.inventorySize > 0) {
      drawSelection()
    }
  }

  protected override def keyTyped(char: Char, code: Int) {
    if (code == Keyboard.KEY_ESCAPE) {
      super.keyTyped(char, code)
    }
  }

  override protected def mouseClicked(mouseX: Int, mouseY: Int, button: Int) {
    super.mouseClicked(mouseX, mouseY, button)
    if (canScroll && button == 0 && isCoordinateOverScrollBar(mouseX - guiLeft, mouseY - guiTop)) {
      isDragging = true
      scrollMouse(mouseY)
    }
  }

  override def mouseMovedOrUp(mouseX: Int, mouseY: Int, button: Int) {
    super.mouseMovedOrUp(mouseX, mouseY, button)
    if (button == 0) {
      isDragging = false
    }
  }

  override def mouseClickMove(mouseX: Int, mouseY: Int, lastButtonClicked: Int, timeSinceMouseClick: Long) {
    super.mouseClickMove(mouseX, mouseY, lastButtonClicked, timeSinceMouseClick)
    if (isDragging) {
      scrollMouse(mouseY)
    }
  }

  private def scrollMouse(mouseY: Int) {
    scrollTo(math.round((mouseY - guiTop - scrollY + 1 - 6.5) * maxOffset / (scrollHeight - 13.0)).toInt)
  }

  override def handleMouseInput() {
    super.handleMouseInput()
    if (Mouse.hasWheel && Mouse.getEventDWheel != 0) {
      val mouseX = Mouse.getEventX * width / mc.displayWidth - guiLeft
      val mouseY = height - Mouse.getEventY * height / mc.displayHeight - 1 - guiTop
      if (isCoordinateOverInventory(mouseX, mouseY) || isCoordinateOverScrollBar(mouseX, mouseY)) {
        if (math.signum(Mouse.getEventDWheel) < 0) scrollDown()
        else scrollUp()
      }
    }
  }

  private def isCoordinateOverInventory(x: Int, y: Int) =
    x >= inventoryX && x < inventoryX + slotSize * 4 &&
      y >= inventoryY && y < inventoryY + slotSize * 4

  private def isCoordinateOverScrollBar(x: Int, y: Int) =
    x > scrollX && x < scrollX + scrollWidth &&
      y >= scrollY && y < scrollY + scrollHeight

  private def scrollUp() = scrollTo(inventoryOffset - 1)

  private def scrollDown() = scrollTo(inventoryOffset + 1)

  private def scrollTo(row: Int) {
    inventoryOffset = math.max(0, math.min(maxOffset, row))
    for (index <- 4 until 68) {
      val slot = inventorySlots.getSlot(index)
      val displayIndex = index - inventoryOffset * 4 - 4
      if (displayIndex >= 0 && displayIndex < 16) {
        slot.xDisplayPosition = 1 + inventoryX + (displayIndex % 4) * slotSize
        slot.yDisplayPosition = 1 + inventoryY + (displayIndex / 4) * slotSize
      }
      else {
        // Hide the rest!
        slot.xDisplayPosition = -10000
        slot.yDisplayPosition = -10000
      }
    }
    val yMin = guiTop + scrollY + 1
    if (maxOffset > 0) {
      scrollButton.yPosition = yMin + (scrollHeight - 15) * inventoryOffset / maxOffset
    }
    else {
      scrollButton.yPosition = yMin
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
    val slot = robot.selectedSlot - robot.actualSlot(0) - inventoryOffset * 4
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