package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.common.container
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer
import org.lwjgl.opengl.GL11

class Printer(playerInventory: InventoryPlayer, val printer: tileentity.Printer) extends DynamicGuiContainer(new container.Printer(playerInventory, printer)) {
  xSize = 176
  ySize = 166

  private val materialBar = addWidget(new ProgressBar(40, 21) {
    override def width = 62

    override def height = 12

    override def barTexture = Textures.GUI.PrinterMaterial
  })
  private val inkBar = addWidget(new ProgressBar(40, 53) {
    override def width = 62

    override def height = 12

    override def barTexture = Textures.GUI.PrinterInk
  })
  private val progressBar = addWidget(new ProgressBar(105, 20) {
    override def width = 46

    override def height = 46

    override def barTexture = Textures.GUI.PrinterProgress
  })

  private def printerContainer = inventorySlots.asInstanceOf[container.Printer]

  override def initGui() {
    super.initGui()
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(printer.getName),
      8, 6, 0x404040)
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS) // Me lazy... prevents NEI render glitch.
    if (isPointInRegion(materialBar.x, materialBar.y, materialBar.width, materialBar.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(printerContainer.amountMaterial + "/" + printer.maxAmountMaterial)
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }
    if (isPointInRegion(inkBar.x, inkBar.y, inkBar.width, inkBar.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(printerContainer.amountInk + "/" + printer.maxAmountInk)
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }
    GL11.glPopAttrib()
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    Textures.bind(Textures.GUI.Printer)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    materialBar.level = printerContainer.amountMaterial / printer.maxAmountMaterial.toDouble
    inkBar.level = printerContainer.amountInk / printer.maxAmountInk.toDouble
    progressBar.level = printerContainer.progress
    drawWidgets()
    drawInventorySlots()
  }

  override protected def drawDisabledSlot(slot: ComponentSlot) {}

  override def doesGuiPauseGame = false
}