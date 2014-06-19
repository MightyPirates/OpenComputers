package li.cil.oc.client.gui

import java.util

import li.cil.oc.{Settings, api}
import li.cil.oc.api.driver.{Inventory, Memory, Processor, Slot}
import li.cil.oc.client.{Textures, PacketSender => ClientPacketSender}
import li.cil.oc.common.{container, tileentity}
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

class RobotAssembler(playerInventory: InventoryPlayer, val assembler: tileentity.RobotAssembler) extends DynamicGuiContainer(new container.RobotAssembler(playerInventory, assembler)) {
  xSize = 176
  ySize = 192

  private def assemblerContainer = inventorySlots.asInstanceOf[container.RobotAssembler]

  protected var runButton: ImageButton = _

  private val progressX = 28
  private val progressY = 92

  private val progressWidth = 140
  private val progressHeight = 12

  val suggestedComponents = Array(
    "Screen" -> (() => hasComponent("screen1")),
    "Keyboard" -> (() => hasComponent("keyboard")),
    "GraphicsCard" -> (() => Array("graphicsCard1", "graphicsCard2", "graphicsCard3").exists(hasComponent)),
    "Inventory" -> (() => hasInventory),
    "OS" -> (() => hasFileSystem))

  def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  private def hasCase = assembler.isItemValidForSlot(0, assembler.getStackInSlot(0))

  private def hasCPU = assembler.items.exists {
    case Some(stack) => api.Driver.driverFor(stack) match {
      case _: Processor => true
      case _ => false
    }
    case _ => false
  }

  private def hasRAM = assembler.items.exists {
    case Some(stack) => api.Driver.driverFor(stack) match {
      case _: Memory => true
      case _ => false
    }
    case _ => false
  }

  private def hasComponent(name: String) = assembler.items.exists {
    case Some(stack) => Option(api.Items.get(stack)) match {
      case Some(descriptor) => descriptor.name == name
      case _ => false
    }
    case _ => false
  }

  private def hasInventory = assembler.items.exists {
    case Some(stack) => api.Driver.driverFor(stack) match {
      case _: Inventory => true
      case _ => false
    }
    case _ => false
  }

  private def hasFileSystem = assembler.items.exists {
    case Some(stack) => Option(api.Driver.driverFor(stack)) match {
      case Some(driver) => driver.slot(stack) == Slot.Disk || driver.slot(stack) == Slot.HardDiskDrive
      case _ => false
    }
    case _ => false
  }

  private def isCapacityValid = assembler.complexity <= assembler.maxComplexity

  private def canBuild = !assemblerContainer.isAssembling && hasCase && hasCPU && hasRAM && isCapacityValid

  protected override def actionPerformed(button: GuiButton) {
    if (button.id == 0 && canBuild) {
      ClientPacketSender.sendRobotAssemblerStart(assembler)
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    runButton.enabled = canBuild
    runButton.toggled = !runButton.enabled
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    runButton = new ImageButton(0, guiLeft + 7, guiTop + 89, 18, 18, Textures.guiButtonRun, canToggle = true)
    add(buttonList, runButton)
  }

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS) // Me lazy... prevents NEI render glitch.
    if (!assemblerContainer.isAssembling) {
      def drawMessage(message: String) {
        fontRenderer.drawString(
          StatCollector.translateToLocal(Settings.namespace + message),
          30, 94, 0x404040)
      }
      if (!inventorySlots.getSlot(0).getHasStack) {
        drawMessage("gui.RobotAssembler.InsertCase")
      }
      else if (api.Items.get(inventorySlots.getSlot(0).getStack) == api.Items.get("robot")) {
        drawMessage("gui.RobotAssembler.CollectRobot")
      }
      else if (!hasCPU) {
        drawMessage("gui.RobotAssembler.InsertCPU")
      }
      else if (!hasRAM) {
        drawMessage("gui.RobotAssembler.InsertRAM")
      }
      else {
        fontRenderer.drawString(
          StatCollector.translateToLocalFormatted(Settings.namespace + "gui.RobotAssembler.Complexity", Int.box(assembler.complexity), Int.box(assembler.maxComplexity)),
          30, 94, if (isCapacityValid) 0x404040 else 0x804040)
      }
      if (runButton.func_82252_a) {
        val tooltip = new java.util.ArrayList[String]
        tooltip.add(StatCollector.translateToLocal(Settings.namespace + "gui.RobotAssembler.Run"))
        if (canBuild) {
          var warnings = mutable.ArrayBuffer.empty[String]
          for ((name, check) <- suggestedComponents) {
            if (!check()) {
              warnings += "§7- " + StatCollector.translateToLocal(Settings.namespace + "gui.RobotAssembler.Warning." + name)
            }
          }
          if (warnings.length > 0) {
            tooltip.add(StatCollector.translateToLocalFormatted(Settings.namespace + "gui.RobotAssembler.Warnings"))
            tooltip.addAll(warnings)
          }
        }
        drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
      }
    }
    else if (isPointInRegion(progressX, progressY, progressWidth, progressHeight, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      val timeRemaining = formatTime(assemblerContainer.assemblyRemainingTime)
      tooltip.add(StatCollector.translateToLocalFormatted(Settings.namespace + "gui.RobotAssembler.Progress", assemblerContainer.assemblyProgress.toInt.toString, timeRemaining))
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }
    GL11.glPopAttrib()
  }

  private def formatTime(seconds: Int) = {
    // Assembly times should not / rarely exceed one hour, so this is good enough.
    if (seconds < 60) "0:%02d".format(seconds)
    else "%d:%02d".format(seconds / 60, seconds % 60)
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    super.drawGuiContainerBackgroundLayer(dt, mouseX, mouseY)
    mc.renderEngine.bindTexture(Textures.guiRobotAssembler)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    if (assemblerContainer.isAssembling) {
      drawProgress()
    }
  }

  override def doesGuiPauseGame = false

  private def drawProgress() {
    val level = assemblerContainer.assemblyProgress / 100.0

    val u0 = 0
    val u1 = progressWidth / 256.0 * level
    val v0 = 1 - progressHeight / 256.0
    val v1 = 1
    val x = guiLeft + progressX
    val y = guiTop + progressY
    val w = progressWidth * level

    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y, zLevel, u0, v0)
    t.addVertexWithUV(x, y + progressHeight, zLevel, u0, v1)
    t.addVertexWithUV(x + w, y + progressHeight, zLevel, u1, v1)
    t.addVertexWithUV(x + w, y, zLevel, u1, v0)
    t.draw()
  }
}