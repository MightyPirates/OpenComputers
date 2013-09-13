package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher._
import li.cil.oc.client.components.{ Screen => ClientScreen }
import li.cil.oc.server.components.{ Screen => ServerScreen }
import net.minecraft.tileentity.TileEntity
import li.cil.oc.common.gui.ScreenGui
import li.cil.oc.common.gui.ScreenGui

class TileEntityScreen(isClient: Boolean) extends TileEntity {
  def this() = this(false)

  val component =
    if (isClient) new ClientScreen(this)
    else new ServerScreen(this)

  @SideOnly(Side.CLIENT)
  def updateGui(value: () => String): Unit = {
    // TODO if GUI is open, call value() to get the new display string and show it
      println("CLIENT SCREEN: " + value())
      if(_gui != null){
        _gui.textField.setText(value())
      }
  }

  private var _gui:ScreenGui = null
  def gui = _gui
  def gui_=(value:ScreenGui):Unit = _gui = value
  
  def text = component.toString()
}