package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.internal.TextBuffer
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import li.cil.oc.integration.opencomputers
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.INestedGuiEventHandler
import net.minecraft.client.gui.widget.button.Button
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.StringTextComponent
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11

import scala.collection.JavaConverters.asJavaCollection
import scala.collection.convert.ImplicitConversionsToJava._

class Robot(id: Int, playerInventory: PlayerInventory, val robot: tileentity.Robot)
  extends DynamicGuiContainer(new container.Robot(container.ContainerTypes.ROBOT, id, playerInventory, robot),
    playerInventory, StringTextComponent.EMPTY)
  with traits.InputBuffer with INestedGuiEventHandler {

  override protected val buffer: TextBuffer = robot.components.collect {
    case Some(buffer: api.internal.TextBuffer) => buffer
  }.headOption.orNull

  override protected val hasKeyboard: Boolean = robot.info.components.map(api.Driver.driverFor(_, robot.getClass)).contains(opencomputers.DriverKeyboard)

  private val withScreenHeight = 256
  private val noScreenHeight = 108

  private val deltaY = if (buffer != null) 0 else withScreenHeight - noScreenHeight

  imageWidth = 256
  imageHeight = 256 - deltaY

  protected var powerButton: ImageButton = _

  protected var scrollButton: ImageButton = _

  // Scroll offset for robot inventory.
  private var inventoryOffset = 0

  private def canScroll = robot.inventorySize > 16

  private def maxOffset = robot.inventorySize / 4 - 4

  private val slotSize = 18

  private val maxBufferWidth = 240.0
  private val maxBufferHeight = 140.0

  private def bufferRenderWidth = math.min(maxBufferWidth, TextBufferRenderCache.renderer.charRenderWidth * Settings.screenResolutionsByTier(0)._1)

  private def bufferRenderHeight = math.min(maxBufferHeight, TextBufferRenderCache.renderer.charRenderHeight * Settings.screenResolutionsByTier(0)._2)

  override protected def bufferX: Int = (8 + (maxBufferWidth - bufferRenderWidth) / 2).toInt

  override protected def bufferY: Int = (8 + (maxBufferHeight - bufferRenderHeight) / 2).toInt

  private val inventoryX = 169
  private val inventoryY = 155 - deltaY

  private val scrollX = inventoryX + slotSize * 4 + 2
  private val scrollY = inventoryY
  private val scrollWidth = 8
  private val scrollHeight = 94

  private val power = addCustomWidget(new ProgressBar(26, 156 - deltaY))

  private val selectionSize = 20
  private val selectionsStates = 17
  private val selectionStepV = 1 / selectionsStates.toFloat

  override def render(stack: MatrixStack, mouseX: Int, mouseY: Int, dt: Float) {
    powerButton.toggled = robot.isRunning
    scrollButton.active = canScroll
    scrollButton.hoverOverride = isDragging
    if (robot.inventorySize < 16 + inventoryOffset * 4) {
      scrollTo(0)
    }
    super.render(stack, mouseX, mouseY, dt)
  }

  override protected def init() {
    super.init()
    powerButton = new ImageButton(leftPos + 5, topPos + 153 - deltaY, 18, 18, new Button.IPressable {
      override def onPress(b: Button) = ClientPacketSender.sendComputerPower(robot, !robot.isRunning)
    }, Textures.GUI.ButtonPower, canToggle = true)
    scrollButton = new ImageButton(leftPos + scrollX + 1, topPos + scrollY + 1, 6, 13, new Button.IPressable {
      override def onPress(b: Button) = Unit
    }, Textures.GUI.ButtonScroll)
    addButton(powerButton)
    addButton(scrollButton)
  }

  override def drawBuffer(stack: MatrixStack) {
    if (buffer != null) {
      stack.translate(bufferX, bufferY, 0)
      RenderState.disableEntityLighting()
      stack.pushPose()
      stack.translate(-3, -3, 0)
      RenderSystem.color4f(1, 1, 1, 1)
      BufferRenderer.drawBackground()
      stack.popPose()
      RenderState.makeItBlend()
      val scaleX = bufferRenderWidth / buffer.renderWidth
      val scaleY = bufferRenderHeight / buffer.renderHeight
      val scale = math.min(scaleX, scaleY).toFloat
      if (scaleX > scale) {
        stack.translate(buffer.renderWidth * (scaleX - scale) / 2, 0, 0)
      }
      else if (scaleY > scale) {
        stack.translate(0, buffer.renderHeight * (scaleY - scale) / 2, 0)
      }
      stack.scale(scale, scale, scale)
      stack.scale(this.scale.toFloat, this.scale.toFloat, 1)
      BufferRenderer.drawText(stack, buffer)
    }
  }

  override protected def drawSecondaryForegroundLayer(stack: MatrixStack, mouseX: Int, mouseY: Int) {
    drawBufferLayer(stack)
    RenderState.pushAttrib()
    if (isPointInRegion(power.x, power.y, power.width, power.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      val format = Localization.Computer.Power + ": %d%% (%d/%d)"
      tooltip.add(format.format(
        ((robot.globalBuffer / robot.globalBufferSize) * 100).toInt,
        robot.globalBuffer.toInt,
        robot.globalBufferSize.toInt))
      copiedDrawHoveringText(stack, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }
    if (powerButton.isMouseOver(mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll(asJavaCollection(if (robot.isRunning) Localization.Computer.TurnOff.lines.toIterable else Localization.Computer.TurnOn.lines.toIterable))
      copiedDrawHoveringText(stack, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }
    RenderState.popAttrib()
  }

  override protected def renderBg(stack: MatrixStack, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.color3f(1, 1, 1)
    if (buffer != null) Textures.bind(Textures.GUI.Robot)
    else Textures.bind(Textures.GUI.RobotNoScreen)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    power.level = robot.globalBuffer / robot.globalBufferSize
    drawWidgets(stack)
    if (robot.inventorySize > 0) {
      drawSelection(stack)
    }

    drawInventorySlots(stack)
  }

  // No custom slots, we just extend DynamicGuiContainer for the highlighting.
  override protected def drawSlotBackground(stack: MatrixStack, x: Int, y: Int) {}

  override def mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    val mx = mouseX.asInstanceOf[Int]
    val my = mouseY.asInstanceOf[Int]
    if (canScroll && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isCoordinateOverScrollBar(mx - leftPos, my - topPos)) {
      setDragging(true)
      scrollMouse(mouseY)
      true
    }
    else super.mouseClicked(mouseX, mouseY, button)
  }

  override def mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = {
    if (isDragging) {
      scrollMouse(mouseY)
      true
    }
    else super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
  }

  private def scrollMouse(mouseY: Double) {
    scrollTo(math.round((mouseY - topPos - scrollY + 1 - 6.5) * maxOffset / (scrollHeight - 13.0)).toInt)
  }

  override def mouseScrolled(mouseX: Double, mouseY: Double, scroll: Double): Boolean = {
    val mx = mouseX.asInstanceOf[Int]
    val my = mouseY.asInstanceOf[Int]
    if (isCoordinateOverInventory(mx, my) || isCoordinateOverScrollBar(mx, my)) {
      if (scroll < 0) scrollDown()
      else scrollUp()
      true
    }
    else super.mouseScrolled(mouseX, mouseY, scroll)
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
    menu.generateSlotsFor(inventoryOffset)
  }

  override protected def changeSize(w: Double, h: Double, recompile: Boolean): Double = {
    val bw = w * TextBufferRenderCache.renderer.charRenderWidth
    val bh = h * TextBufferRenderCache.renderer.charRenderHeight
    val scaleX = math.min(bufferRenderWidth / bw, 1)
    val scaleY = math.min(bufferRenderHeight / bh, 1)
    if (recompile) {
      BufferRenderer.compileBackground(bufferRenderWidth.toInt, bufferRenderHeight.toInt, forRobot = true)
    }
    math.min(scaleX, scaleY)
  }

  private def drawSelection(stack: MatrixStack) {
    val slot = robot.selectedSlot - inventoryOffset * 4
    if (slot >= 0 && slot < 16) {
      RenderState.makeItBlend()
      Textures.bind(Textures.GUI.RobotSelection)
      val now = System.currentTimeMillis() / 1000.0f
      val offsetV = ((now - now.toInt) * selectionsStates).toInt * selectionStepV
      val x = leftPos + inventoryX - 1 + (slot % 4) * (selectionSize - 2)
      val y = topPos + inventoryY - 1 + (slot / 4) * (selectionSize - 2)

      val t = Tessellator.getInstance
      val r = t.getBuilder
      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
      r.vertex(stack.last.pose, x, y, getBlitOffset).uv(0, offsetV).endVertex()
      r.vertex(stack.last.pose, x, y + selectionSize, getBlitOffset).uv(0, offsetV + selectionStepV).endVertex()
      r.vertex(stack.last.pose, x + selectionSize, y + selectionSize, getBlitOffset).uv(1, offsetV + selectionStepV).endVertex()
      r.vertex(stack.last.pose, x + selectionSize, y, getBlitOffset).uv(1, offsetV).endVertex()
      t.end()
    }
  }
}
