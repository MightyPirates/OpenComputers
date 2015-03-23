package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

class Printer(playerInventory: InventoryPlayer, val printer: tileentity.Printer) extends DynamicGuiContainer(new container.Printer(playerInventory, printer)) {
  xSize = 176
  ySize = 166

  private val progress = addWidget(new ProgressBar(104, 21) {
    override def width = 46

    override def height = 46

    override def barTexture = Textures.guiPrinterProgress
  })

  private def assemblerContainer = inventorySlots.asInstanceOf[container.Printer]

  override def initGui() {
    super.initGui()
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(printer.getInventoryName),
      8, 6, 0x404040)
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS) // Me lazy... prevents NEI render glitch.
    if (assemblerContainer.isAssembling && func_146978_c(progress.x, progress.y, progress.width, progress.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      val timeRemaining = formatTime(assemblerContainer.assemblyRemainingTime)
      tooltip.add(Localization.Assembler.Progress(assemblerContainer.assemblyProgress, timeRemaining))
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }
    GL11.glPopAttrib()
  }

  private def formatTime(seconds: Int) = {
    // Assembly times should not / rarely exceed one hour, so this is good enough.
    if (seconds < 60) f"0:$seconds%02d"
    else f"${seconds / 60}:${seconds % 60}%02d"
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    mc.renderEngine.bindTexture(Textures.guiPrinter)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    if (assemblerContainer.isAssembling) progress.level = assemblerContainer.assemblyProgress / 100.0
    else progress.level = 0
    drawWidgets()
    drawInventorySlots()
  }

  override protected def drawDisabledSlot(slot: ComponentSlot) {}

  override def doesGuiPauseGame = false
}