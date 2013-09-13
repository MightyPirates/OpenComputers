package li.cil.oc.common.gui

import net.minecraft.client.gui.GuiScreen
import li.cil.oc.common.tileentity.TileEntityScreen

class ScreenGui(val tileEntity: TileEntityScreen) extends GuiScreen {
  tileEntity.gui_=(this)
  var textField: GuiMultilineTextField = null
  
  override def initGui() = {
    super.initGui()
    var(w,h) = tileEntity.component.resolution
    println(" widht: "+w)
    println("heigth:" +h)
    w *=2
    h *=2
    var x =  (width - w)/2
    var y = (height -h)/2
      
    textField = new GuiMultilineTextField(this.fontRenderer, x, y, w, h)
    textField.setText(tileEntity.text)
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) = {
    super.drawScreen(mouseX, mouseY, dt);

    textField.drawTextBox()
  }
}