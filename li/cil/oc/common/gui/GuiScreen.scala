package li.cil.oc.common.gui

import li.cil.oc.common.tileentity.TileEntityScreen

class GuiScreen(val tileEntity: TileEntityScreen) extends net.minecraft.client.gui.GuiScreen {
  tileEntity.gui = Some(this)

  private var textField: GuiMultilineTextField = null

  def setSize(w: Int, h: Int) = {
    val (pixelWidth, pixelHeight) = (w * 5 + 4, h * fontRenderer.FONT_HEIGHT + 4)
    val x = (width - pixelWidth) / 2
    val y = (height - pixelHeight) / 2
    textField.setBounds(x, y, pixelWidth, pixelHeight)
  }

  def setText(s: String) = textField.setText(s)

  override def initGui() = {
    super.initGui()
    textField = new GuiMultilineTextField(fontRenderer)

    val (w, h) = tileEntity.component.resolution
    setSize(w, h)
    textField.setText(tileEntity.component.text)
  }

  override def onGuiClosed = {
    super.onGuiClosed()
    tileEntity.gui = None
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) = {
    super.drawScreen(mouseX, mouseY, dt);
    textField.drawTextBox()
  }

  override def doesGuiPauseGame = false
}