package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.common.container
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.entity.player.PlayerInventory

class Printer(id: Int, playerInventory: PlayerInventory, val printer: tileentity.Printer)
  extends DynamicGuiContainer(new container.Printer(id, playerInventory, printer),
    playerInventory, printer.getName) {

  imageWidth = 176
  imageHeight = 166

  private val materialBar = addCustomWidget(new ProgressBar(40, 21) {
    override def width = 62

    override def height = 12

    override def barTexture = Textures.GUI.PrinterMaterial
  })
  private val inkBar = addCustomWidget(new ProgressBar(40, 53) {
    override def width = 62

    override def height = 12

    override def barTexture = Textures.GUI.PrinterInk
  })
  private val progressBar = addCustomWidget(new ProgressBar(105, 20) {
    override def width = 46

    override def height = 46

    override def barTexture = Textures.GUI.PrinterProgress
  })

  override def drawSecondaryForegroundLayer(stack: MatrixStack, mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(stack, mouseX, mouseY)
    RenderState.pushAttrib()
    if (isPointInRegion(materialBar.x, materialBar.y, materialBar.width, materialBar.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(inventoryContainer.amountMaterial + "/" + printer.maxAmountMaterial)
      copiedDrawHoveringText(stack, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }
    if (isPointInRegion(inkBar.x, inkBar.y, inkBar.width, inkBar.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(inventoryContainer.amountInk + "/" + printer.maxAmountInk)
      copiedDrawHoveringText(stack, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }
    RenderState.popAttrib()
  }

  override def renderBg(stack: MatrixStack, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.color3f(1, 1, 1)
    Textures.bind(Textures.GUI.Printer)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    materialBar.level = inventoryContainer.amountMaterial / printer.maxAmountMaterial.toDouble
    inkBar.level = inventoryContainer.amountInk / printer.maxAmountInk.toDouble
    progressBar.level = inventoryContainer.progress
    drawWidgets(stack)
    drawInventorySlots(stack)
  }

  override protected def drawDisabledSlot(stack: MatrixStack, slot: ComponentSlot) {}
}
