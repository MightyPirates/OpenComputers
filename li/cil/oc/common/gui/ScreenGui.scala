package li.cil.oc.common.gui

import net.minecraft.client.gui.GuiScreen
import li.cil.oc.common.tileentity.TileEntityScreen

class ScreenGui(val tileEntity: TileEntityScreen) extends GuiScreen {
  tileEntity.gui_=(this)
  var textField: GuiMultilineTextField = null
  
  override def initGui() = {
    super.initGui()
    textField = new GuiMultilineTextField(this.fontRenderer, 20, 20, 200, 100)
    textField.setText(tileEntity.text)
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) = {
    super.drawScreen(mouseX, mouseY, dt);

    textField.drawTextBox()
  }
}