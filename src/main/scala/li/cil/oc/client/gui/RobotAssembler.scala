package li.cil.oc.client.gui

import java.util

import li.cil.oc.api.driver.{Inventory, Memory, Processor}
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.client.{Textures, PacketSender => ClientPacketSender}
import li.cil.oc.common.{Slot, container, tileentity}
import li.cil.oc.{Localization, api}
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.InventoryPlayer
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

class RobotAssembler(playerInventory: InventoryPlayer, val assembler: tileentity.RobotAssembler) extends DynamicGuiContainer(new container.RobotAssembler(playerInventory, assembler)) {
  xSize = 176
  ySize = 192

  private def assemblerContainer = inventorySlots.asInstanceOf[container.RobotAssembler]

  protected var runButton: ImageButton = _

  private val progress = addWidget(new ProgressBar(28, 92))

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
      case Some(driver) => Slot.fromApi(driver.slot(stack)) == Slot.Floppy || Slot.fromApi(driver.slot(stack)) == Slot.HDD
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
        fontRenderer.drawString(message, 30, 94, 0x404040)
      }
      if (!inventorySlots.getSlot(0).getHasStack) {
        drawMessage(Localization.RobotAssembler.InsertCase)
      }
      else if (api.Items.get(inventorySlots.getSlot(0).getStack) == api.Items.get("robot")) {
        drawMessage(Localization.RobotAssembler.CollectRobot)
      }
      else if (!hasCPU) {
        drawMessage(Localization.RobotAssembler.InsertCPU)
      }
      else if (!hasRAM) {
        drawMessage(Localization.RobotAssembler.InsertRAM)
      }
      else {
        fontRenderer.drawString(Localization.RobotAssembler.Complexity(assembler.complexity, assembler.maxComplexity), 30, 94, if (isCapacityValid) 0x404040 else 0x804040)
      }
      if (runButton.func_82252_a) {
        val tooltip = new java.util.ArrayList[String]
        tooltip.add(Localization.RobotAssembler.Run)
        if (canBuild) {
          var warnings = mutable.ArrayBuffer.empty[String]
          for ((name, check) <- suggestedComponents) {
            if (!check()) {
              warnings += Localization.RobotAssembler.Warning(name)
            }
          }
          if (warnings.length > 0) {
            tooltip.add(Localization.RobotAssembler.Warnings)
            tooltip.addAll(warnings)
          }
        }
        drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
      }
    }
    else if (isPointInRegion(progress.x, progress.y, progress.width, progress.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      val timeRemaining = formatTime(assemblerContainer.assemblyRemainingTime)
      tooltip.add(Localization.RobotAssembler.Progress(assemblerContainer.assemblyProgress, timeRemaining))
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
    if (assemblerContainer.isAssembling) progress.level = assemblerContainer.assemblyProgress / 100.0
    else progress.level = 0
    drawWidgets()
  }

  override def doesGuiPauseGame = false
}