package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.template.AssemblerTemplates
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.util.text.ITextComponent

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

class Assembler(playerInventory: InventoryPlayer, val assembler: tileentity.Assembler) extends DynamicGuiContainer(new container.Assembler(playerInventory, assembler)) {
  xSize = 176
  ySize = 192

  for (slot <- inventorySlots.inventorySlots) slot match {
    case component: ComponentSlot => component.changeListener = Option(onSlotChanged)
    case _ =>
  }

  private def onSlotChanged(slot: Slot) {
    runButton.enabled = canBuild
    runButton.toggled = !runButton.enabled
    info = validate
  }

  var info: Option[(Boolean, ITextComponent, Array[ITextComponent])] = None

  protected var runButton: ImageButton = _

  private val progress = addWidget(new ProgressBar(28, 92))

  private def validate = AssemblerTemplates.select(inventoryContainer.getSlot(0).getStack).map(_.validate(inventoryContainer.otherInventory))

  private def canBuild = !inventoryContainer.isAssembling && validate.exists(_._1)

  protected override def actionPerformed(button: GuiButton) {
    if (button.id == 0 && canBuild) {
      ClientPacketSender.sendRobotAssemblerStart(assembler)
    }
  }

  override def initGui() {
    super.initGui()
    runButton = new ImageButton(0, guiLeft + 7, guiTop + 89, 18, 18, Textures.GUI.ButtonRun, canToggle = true)
    add(buttonList, runButton)
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    RenderState.pushAttrib()
    if (!inventoryContainer.isAssembling) {
      val message =
        if (!inventoryContainer.getSlot(0).getHasStack) {
          Localization.Assembler.InsertTemplate
        }
        else info match {
          case Some((_, value, _)) if value != null => value.getUnformattedText
          case _ if inventoryContainer.getSlot(0).getHasStack => Localization.Assembler.CollectResult
          case _ => ""
        }
      fontRendererObj.drawString(message, 30, 94, 0x404040)
      if (runButton.isMouseOver) {
        val tooltip = new java.util.ArrayList[String]
        tooltip.add(Localization.Assembler.Run)
        info.foreach {
          case (valid, _, warnings) => if (valid && warnings.length > 0) {
            tooltip.addAll(warnings.map(_.getUnformattedText).toList)
          }
        }
        copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
      }
    }
    else if (isPointInRegion(progress.x, progress.y, progress.width, progress.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      val timeRemaining = formatTime(inventoryContainer.assemblyRemainingTime)
      tooltip.add(Localization.Assembler.Progress(inventoryContainer.assemblyProgress, timeRemaining))
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }
    RenderState.popAttrib()
  }

  private def formatTime(seconds: Int) = {
    // Assembly times should not / rarely exceed one hour, so this is good enough.
    if (seconds < 60) f"0:$seconds%02d"
    else f"${seconds / 60}:${seconds % 60}%02d"
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GlStateManager.color(1, 1, 1) // Required under Linux.
    Textures.bind(Textures.GUI.RobotAssembler)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    if (inventoryContainer.isAssembling) progress.level = inventoryContainer.assemblyProgress / 100.0
    else progress.level = 0
    drawWidgets()
    drawInventorySlots()
  }

  override protected def drawDisabledSlot(slot: ComponentSlot) {}
}