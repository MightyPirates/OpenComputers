package li.cil.oc.common.gui

import li.cil.oc.common.tileentity.TileEntityScreen

class GuiScreen(val tileEntity: TileEntityScreen) extends net.minecraft.client.gui.GuiScreen {
  tileEntity.gui = Some(this)
  var textField: GuiMultilineTextField = null

  override def initGui() = {
    super.initGui()
    val (w, h) = tileEntity.component.resolution
    val (pixelWidth, pixelHeight) = (w * 5 + 4, h * fontRenderer.FONT_HEIGHT + 4)
    val x = (width - pixelWidth) / 2
    val y = (height - pixelHeight) / 2
    textField = new GuiMultilineTextField(
      fontRenderer, x, y, pixelWidth, pixelHeight)
    textField.setText(tileEntity.component.toString)
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) = {
    super.drawScreen(mouseX, mouseY, dt);
    textField.drawTextBox()
  }

  override def doesGuiPauseGame = false
}