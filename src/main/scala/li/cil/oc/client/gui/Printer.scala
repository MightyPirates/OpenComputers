package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.common.container
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.tileentity
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.InventoryPlayer

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

  override def initGui() {
    super.initGui()
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(printer.getName),
      8, 6, 0x404040)
    GlStateManager.pushAttrib()
    if (isPointInRegion(materialBar.x, materialBar.y, materialBar.width, materialBar.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(inventoryContainer.amountMaterial + "/" + printer.maxAmountMaterial)
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }
    if (isPointInRegion(inkBar.x, inkBar.y, inkBar.width, inkBar.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(inventoryContainer.amountInk + "/" + printer.maxAmountInk)
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }
    GlStateManager.popAttrib()
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GlStateManager.color(1, 1, 1)
    Textures.bind(Textures.GUI.Printer)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    materialBar.level = inventoryContainer.amountMaterial / printer.maxAmountMaterial.toDouble
    inkBar.level = inventoryContainer.amountInk / printer.maxAmountInk.toDouble
    progressBar.level = inventoryContainer.progress
    drawWidgets()
    drawInventorySlots()
  }

  override protected def drawDisabledSlot(slot: ComponentSlot) {}
}