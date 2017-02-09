package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import li.cil.oc.integration.opencomputers
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.InventoryPlayer
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsJava._

class Robot(playerInventory: InventoryPlayer, val robot: tileentity.Robot) extends DynamicGuiContainer(new container.Robot(playerInventory, robot)) with traits.InputBuffer {
  override protected val buffer = robot.components.collect {
    case Some(buffer: api.internal.TextBuffer) => buffer
  }.headOption.orNull

  override protected val hasKeyboard = robot.info.components.map(api.Driver.driverFor(_, robot.getClass)).contains(opencomputers.DriverKeyboard)

  private val withScreenHeight = 256
  private val noScreenHeight = 108

  private val deltaY = if (buffer != null) 0 else withScreenHeight - noScreenHeight

  xSize = 256
  ySize = 256 - deltaY

  protected var powerButton: ImageButton = _

  protected var scrollButton: ImageButton = _

  // Scroll offset for robot inventory.
  private var inventoryOffset = 0
  private var isDragging = false

  private def canScroll = robot.inventorySize > 16

  private def maxOffset = robot.inventorySize / 4 - 4

  private val slotSize = 18

  private val maxBufferWidth = 240.0
  private val maxBufferHeight = 140.0

  private def bufferRenderWidth = math.min(maxBufferWidth, TextBufferRenderCache.renderer.charRenderWidth * Settings.screenResolutionsByTier(0)._1)

  private def bufferRenderHeight = math.min(maxBufferHeight, TextBufferRenderCache.renderer.charRenderHeight * Settings.screenResolutionsByTier(0)._2)

  override protected def bufferX = (8 + (maxBufferWidth - bufferRenderWidth) / 2).toInt

  override protected def bufferY = (8 + (maxBufferHeight - bufferRenderHeight) / 2).toInt

  private val inventoryX = 169
  private val inventoryY = 155 - deltaY

  private val scrollX = inventoryX + slotSize * 4 + 2
  private val scrollY = inventoryY
  private val scrollWidth = 8
  private val scrollHeight = 94

  private val power = addWidget(new ProgressBar(26, 156 - deltaY))

  private val selectionSize = 20
  private val selectionsStates = 17
  private val selectionStepV = 1 / selectionsStates.toDouble

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
    powerButton = new ImageButton(0, guiLeft + 5, guiTop + 153 - deltaY, 18, 18, Textures.GUI.ButtonPower, canToggle = true)
    scrollButton = new ImageButton(1, guiLeft + scrollX + 1, guiTop + scrollY + 1, 6, 13, Textures.GUI.ButtonScroll)
    add(buttonList, powerButton)
    add(buttonList, scrollButton)
  }

  override def drawBuffer() {
    if (buffer != null) {
      GlStateManager.translate(bufferX, bufferY, 0)
      RenderState.disableEntityLighting()
      GlStateManager.pushMatrix()
      GlStateManager.translate(-3, -3, 0)
      GlStateManager.color(1, 1, 1, 1)
      BufferRenderer.drawBackground()
      GlStateManager.popMatrix()
      RenderState.makeItBlend()
      val scaleX = bufferRenderWidth / buffer.renderWidth
      val scaleY = bufferRenderHeight / buffer.renderHeight
      val scale = math.min(scaleX, scaleY)
      if (scaleX > scale) {
        GlStateManager.translate(buffer.renderWidth * (scaleX - scale) / 2, 0, 0)
      }
      else if (scaleY > scale) {
        GlStateManager.translate(0, buffer.renderHeight * (scaleY - scale) / 2, 0)
      }
      GlStateManager.scale(scale, scale, scale)
      GlStateManager.scale(this.scale, this.scale, 1)
      BufferRenderer.drawText(buffer)
    }
  }

  override protected def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) {
    drawBufferLayer()
    RenderState.pushAttrib()
    if (isPointInRegion(power.x, power.y, power.width, power.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      val format = Localization.Computer.Power + ": %d%% (%d/%d)"
      tooltip.add(format.format(
        ((robot.globalBuffer / robot.globalBufferSize) * 100).toInt,
        robot.globalBuffer.toInt,
        robot.globalBufferSize.toInt))
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }
    if (powerButton.isMouseOver) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll(asJavaCollection(if (robot.isRunning) Localization.Computer.TurnOff.lines.toIterable else Localization.Computer.TurnOn.lines.toIterable))
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }
    RenderState.popAttrib()
  }

  override protected def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GlStateManager.color(1, 1, 1)
    if (buffer != null) Textures.bind(Textures.GUI.Robot)
    else Textures.bind(Textures.GUI.RobotNoScreen)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    power.level = robot.globalBuffer / robot.globalBufferSize
    drawWidgets()
    if (robot.inventorySize > 0) {
      drawSelection()
    }

    drawInventorySlots()
  }

  // No custom slots, we just extend DynamicGuiContainer for the highlighting.
  override protected def drawSlotBackground(x: Int, y: Int) {}

  override protected def keyTyped(char: Char, code: Int) {
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

  override protected def mouseReleased(mouseX: Int, mouseY: Int, button: Int) {
    super.mouseReleased(mouseX, mouseY, button)
    if (button == 0) {
      isDragging = false
    }
  }

  override protected def mouseClickMove(mouseX: Int, mouseY: Int, lastButtonClicked: Int, timeSinceMouseClick: Long) {
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
        slot.xPos = 1 + inventoryX + (displayIndex % 4) * slotSize
        slot.yPos = 1 + inventoryY + (displayIndex / 4) * slotSize
      }
      else {
        // Hide the rest!
        slot.xPos = -10000
        slot.yPos = -10000
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

  override protected def changeSize(w: Double, h: Double, recompile: Boolean) = {
    val bw = w * TextBufferRenderCache.renderer.charRenderWidth
    val bh = h * TextBufferRenderCache.renderer.charRenderHeight
    val scaleX = math.min(bufferRenderWidth / bw, 1)
    val scaleY = math.min(bufferRenderHeight / bh, 1)
    if (recompile) {
      BufferRenderer.compileBackground(bufferRenderWidth.toInt, bufferRenderHeight.toInt, forRobot = true)
    }
    math.min(scaleX, scaleY)
  }

  private def drawSelection() {
    val slot = robot.selectedSlot - inventoryOffset * 4
    if (slot >= 0 && slot < 16) {
      RenderState.makeItBlend()
      Textures.bind(Textures.GUI.RobotSelection)
      val now = System.currentTimeMillis() / 1000.0
      val offsetV = ((now - now.toInt) * selectionsStates).toInt * selectionStepV
      val x = guiLeft + inventoryX - 1 + (slot % 4) * (selectionSize - 2)
      val y = guiTop + inventoryY - 1 + (slot / 4) * (selectionSize - 2)

      val t = Tessellator.getInstance
      val r = t.getBuffer
      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
      r.pos(x, y, zLevel).tex(0, offsetV).endVertex()
      r.pos(x, y + selectionSize, zLevel).tex(0, offsetV + selectionStepV).endVertex()
      r.pos(x + selectionSize, y + selectionSize, zLevel).tex(1, offsetV + selectionStepV).endVertex()
      r.pos(x + selectionSize, y, zLevel).tex(1, offsetV).endVertex()
      t.draw()
    }
  }
}
