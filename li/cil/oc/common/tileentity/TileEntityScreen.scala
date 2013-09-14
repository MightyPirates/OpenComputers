package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher._
import li.cil.oc.client.components.{ Screen => ClientScreen }
import li.cil.oc.common.gui.GuiScreen
import li.cil.oc.server.components.{ Screen => ServerScreen }
import net.minecraft.tileentity.TileEntity

class TileEntityScreen(isClient: Boolean) extends TileEntity {
  def this() = this(false)

  val component =
    if (isClient) new ClientScreen(this)
    else new ServerScreen(this)

  var gui: Option[GuiScreen] = None

  @SideOnly(Side.CLIENT)
  def updateGui(value: () => String): Unit =
    gui.foreach(_.textField.setText(value()))
}